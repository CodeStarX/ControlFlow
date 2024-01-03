package io.github.codestarx.helper

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.codestarx.models.ConditionData
import io.github.codestarx.models.TransformData
import io.github.codestarx.status.TaskStatus
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

typealias Action = (onContinuation: (Any) -> Unit, onFailure: (Throwable) -> Unit) -> Unit
typealias ActionUnit = (onContinuation: () -> Unit,onFailure: (Throwable) -> Unit) -> Unit
const val THROWABLE_DEFAULT_MESSAGE = "You haven't utilized Throwable for This Task. By default, this message is set, but for optimal performance, it's advisable to specifically define the throwable value in relation to the Task."
@Keep
abstract class Dispatcher: ViewModel() {

    private val handler = CoroutineExceptionHandler { _, exception ->
        exception.message?.let { Log.e("Dispatcher", it) }
    }

    /**
     * Safely launches a coroutine with error handling using a provided block.
     * Usage:
     * Call this method with a suspend function to execute as a coroutine.
     * Example:
     * ```
     * safeLauncher { mySuspendFunction() }
     * ```
     */
     fun safeLauncher(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(handler, block = block)
    }


    /**
     * Executes an action and emits TaskStatus based on the result or error.
     * No transformation applied to the result.
     * Usage:
     * Call this method with an action and handle its continuation and failure scenarios.
     * Example:
     * ```
     * launchAwait(action = { onContinue, onFailure -> myAction(onContinue, onFailure) })
     * ```
     */
    fun launchAwait(
        action: Action
    ): Flow<TaskStatus> = flow {
        try {
            val result = suspendCoroutine<TaskStatus> { continuation ->
                action.invoke({
                    continuation.resume(value = TaskStatus.DoneSuccessfully(result = it))
                },{
                    continuation.resumeWithException(it)
                })
            }
            emit(value = result)
        }catch (throwable: Throwable){
            throw throwable
        }
    }


    /**
     * Executes an action and emits TaskStatus based on conditions applied to the result.
     * No transformation applied to the result.
     * Usage:
     * Call this method with an action and conditions to handle different results.
     * Example:
     * ```
     * launchAwait(
     *     action = { onContinue, onFailure -> myAction(onContinue, onFailure) },
     *     actionCondition = { result -> checkConditions(result) }
     * )
     * ```
     */
    @JvmName("launchAwaitCondition")
    fun launchAwait(
        action: Action,
        actionCondition: (Any) -> ConditionData
    ): Flow<TaskStatus> = flow {
        try {
            val result = suspendCoroutine<TaskStatus> { continuation ->
                action.invoke({
                    val result = actionCondition.invoke(it)
                    when(result.status) {
                        true -> {
                            continuation.resume(value = TaskStatus.DoneSuccessfully(result = it))
                        }
                        else -> {
                            continuation.resumeWithException(result.throwable ?: Throwable(THROWABLE_DEFAULT_MESSAGE)
                            )
                        }
                    }
                },{
                    continuation.resumeWithException(it)
                })
            }
            emit(value = result)
        }catch (throwable: Throwable){
            throw throwable
        }
    }

    /**
     * Executes an action and emits TaskStatus based on conditions applied to the result.
     * Applies transformation to the result before emitting the status.
     * Usage:
     * Call this method with an action, a transformation, and conditions to handle different results.
     * Example:
     * ```
     * launchAwait(
     *     action = { onContinue, onFailure -> myAction(onContinue, onFailure) },
     *     transformer = { result -> transformResult(result) },
     *     actionCondition = { result -> checkConditions(result) }
     * )
     * ```
     */
    fun launchAwait(
        action: Action,
        actionCondition: (Any) -> ConditionData,
        transformer: (Any) -> TransformData<Any>
    ): Flow<TaskStatus> = flow {
        try {
            val result = suspendCoroutine<TaskStatus> { continuation ->
                action.invoke({
                    val result = actionCondition.invoke(it)
                    when(result.status) {
                        true -> {
                            continuation.resume(value = TaskStatus.DoneSuccessfully(result = transformer.invoke(it).data))
                        }
                        else -> {
                            continuation.resumeWithException(result.throwable ?: Throwable(THROWABLE_DEFAULT_MESSAGE)
                            )
                        }
                    }
                },{
                    continuation.resumeWithException(it)
                })
            }
            emit(value = result)
        }catch (throwable: Throwable){
            throw throwable
        }
    }

    /**
     * Executes an action and emits TaskStatus based on the transformed result.
     * Applies transformation to the result before emitting the status.
     * Usage:
     * Call this method with an action and apply a transformation to emit the status.
     * Example:
     * ```
     * launchAwait(
     *     action = { onContinue, onFailure -> myAction(onContinue, onFailure) },
     *     transformer = { result -> transformResult(result) }
     * )
     * ```
     */
    @JvmName("launchAwaitTransformer")
    fun launchAwait(
        action: Action,
        transformer: (Any) -> TransformData<Any>
    ): Flow<TaskStatus> = flow {
        try {
            val result = suspendCoroutine<TaskStatus> { continuation ->
                action.invoke({
                    continuation.resume(value = TaskStatus.DoneSuccessfully(result = transformer.invoke(it).data))
                },{
                    continuation.resumeWithException(it)
                })
            }
            emit(value = result)
        }catch (throwable: Throwable){
           throw throwable
        }
    }

    /**
     * Executes an action without a result and emits TaskStatus.
     * Usage:
     * Call this method with an action that doesn't return a result and handle failure scenarios.
     * Example:
     * ```
     * launchUnitAwait(
     *     action = { onContinue, onFailure -> myAction(onContinue, onFailure) }
     * )
     * ```
     */
    fun launchUnitAwait(
        action: ActionUnit
    ): Flow<TaskStatus> = flow {
        try {
            val result = suspendCoroutine<TaskStatus> { continuation ->
                action.invoke({
                    continuation.resume(value = TaskStatus.DoneSuccessfully(result = Unit))
                },{
                    continuation.resumeWithException(it)
                })
            }
            emit(value = result)
        }catch (throwable: Throwable){
           throw throwable
        }
    }

    /**
     * Executes a Flow and emits TaskStatus based on conditions applied to the collected result.
     * No transformation applied to the result.
     * Usage:
     * Call this method with a suspend function returning a Flow and handle result conditions.
     * Example:
     * ```
     * launchFlow(
     *     action = { myFlowFunction() },
     *     actionCondition = { result -> checkConditions(result) }
     * )
     * ```
     */
    @JvmName("launchFlowCondition")
    fun<T> launchFlow(
        action: suspend () -> Flow<T>,
        actionCondition: (T) -> ConditionData
    ): Flow<TaskStatus> = flow {
        var result: T? = null
        try {
            action.invoke().collect { value ->
                result = value
            }
        }catch (throwable: Throwable) {
             throw throwable
        }
        finally {
            if(result != null){
                val conResult = actionCondition.invoke(result!!)
                when(conResult.status){
                    true -> {
                        emit(value = TaskStatus.DoneSuccessfully(result = result))
                    }
                    else -> {
                        throw conResult.throwable ?: Throwable(THROWABLE_DEFAULT_MESSAGE)
                    }
                }
            }

        }
    }



    /**
     * Executes a Flow and emits TaskStatus based on conditions applied to the transformed result.
     * Applies transformation to the collected result before emitting the status.
     * Usage:
     * Call this method with a suspend function returning a Flow, a transformation, and conditions.
     * Example:
     * ```
     * launchFlow(
     *     action = { myFlowFunction() },
     *     transformer = { result -> transformResult(result) },
     *     actionCondition = { result -> checkConditions(result) }
     * )
     * ```
     */
    fun<T> launchFlow(
        action: suspend () -> Flow<T>,
        actionCondition: (T) -> ConditionData,
        transformer: (T) -> TransformData<Any>,
    ): Flow<TaskStatus> = flow {
        var result: T? = null
        try {
            action.invoke().collect { value ->
                result = value
            }
        }catch (throwable: Throwable) {
            throw throwable
        }
        finally {
            if(result != null){
                val conResult = actionCondition.invoke(result!!)
                when(conResult.status){
                    true -> {
                        emit(value = TaskStatus.DoneSuccessfully(result = transformer.invoke(result!!).data))
                    }
                    else -> {
                        throw conResult.throwable ?:
                               Throwable(THROWABLE_DEFAULT_MESSAGE)
                    }
                }
            }

        }
    }

    /**
     * Executes a Flow and emits TaskStatus based on the transformed result.
     * Applies transformation to the collected result before emitting the status.
     * Usage:
     * Call this method with a suspend function returning a Flow and apply a transformation.
     * Example:
     * ```
     * launchFlow(
     *     action = { myFlowFunction() },
     *     transformer = { result -> transformResult(result) }
     * )
     * ```
     */
    @JvmName("launchFlowTransformer")
    fun<T> launchFlow(
        action: suspend () -> Flow<T>,
        transformer: (T) -> TransformData<Any>
    ): Flow<TaskStatus> = flow {
        var result: T? = null
        try {
            action.invoke().collect { value ->
                result = value
            }
        }catch (throwable: Throwable) {
          throw throwable
        }
        finally {
            if(result != null){
                emit(value = TaskStatus.DoneSuccessfully(result = transformer.invoke(result!!).data))
            }

        }
    }

    /**
     * Executes a Flow and emits TaskStatus based on the collected result.
     * No conditions or transformations applied to the result.
     * Usage:
     * Call this method with a suspend function returning a Flow and emit the status based on the result.
     * Example:
     * ```
     * launchFlow(
     *     action = { myFlowFunction() }
     * )
     * ```
     */
    fun<T> launchFlow(
        action: suspend () -> Flow<T>
    ): Flow<TaskStatus> = flow {
        var result: T? = null
        try {
            action.invoke().collect { value ->
                result = value
            }
        }catch (throwable: Throwable) {
            throw throwable
        }
        finally {
            if(result != null){
                emit(value = TaskStatus.DoneSuccessfully(result = result))
            }
        }
    }


    /**
     * Executes an action and emits TaskStatus based on conditions applied to the result.
     * Usage:
     * Call this method with an action and conditions to handle different results.
     * Example:
     * ```
     * launch(
     *     action = { myAction() },
     *     actionCondition = { result -> checkConditions(result) }
     * )
     * ```
     */
    @JvmName("launchCondition")
    fun <T> launch(
        action: () -> T,
        actionCondition: (T) -> ConditionData
    ): Flow<TaskStatus> = flow {
        var result: T? = null
        try {
            result = action.invoke()

        }catch (throwable: Throwable) {
           throw throwable
        }
        finally {
            if(result != null){
                val conResult = actionCondition.invoke(result)
                when(conResult.status){
                    true -> {
                        emit(value = TaskStatus.DoneSuccessfully(result = result))
                    }
                    else -> {
                        throw conResult.throwable ?: Throwable(THROWABLE_DEFAULT_MESSAGE)
                    }
                }
            }
        }
    }

    /**
     * Executes an action and emits TaskStatus.
     * Usage:
     * Call this method with an action and emit the status based on the result or error.
     * Example:
     * ```
     * launch(
     *     action = { myAction() }
     * )
     * ```
     */
    fun <T>launch(
        action: () -> T
    ): Flow<TaskStatus> = flow {
        try {
            emit(value = TaskStatus.DoneSuccessfully(result = action.invoke()))
        }catch (throwable: Throwable) {
           throw throwable
        }
    }

    /**
     * Executes an action and emits TaskStatus based on the transformed result.
     * Applies transformation to the result before emitting the status.
     * Usage:
     * Call this method with an action and apply a transformation to emit the status.
     * Example:
     * ```
     * launch(
     *     action = { myAction() },
     *     transformer = { result -> transformResult(result) }
     * )
     * ```
     */
    @JvmName("launchTransformer")
    fun <T>launch(
        action: () -> T,
        transformer: (T) -> TransformData<Any>
    ): Flow<TaskStatus> = flow {
        try {
            emit(value = TaskStatus.DoneSuccessfully(result = transformer.invoke(action.invoke()).data))
        }catch (throwable: Throwable) {
           throw throwable
        }
    }

}