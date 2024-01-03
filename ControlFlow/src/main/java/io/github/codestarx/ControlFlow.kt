package io.github.codestarx

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.codestarx.interfaces.WorkFlowTracker
import io.github.codestarx.interfaces.TaskProcessor
import io.github.codestarx.interfaces.RollbackTaskProcessor
import io.github.codestarx.interfaces.RollbackStatusTracker
import io.github.codestarx.interfaces.TaskStatusTracker
import io.github.codestarx.models.TaskFlow
import io.github.codestarx.status.State
import io.github.codestarx.status.TaskStatus
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

/**
 * ControlFlow manages the execution sequence of tasks and potential rollback actions.
 * It orchestrates the execution, rollback, completion, and error handling of tasks and their rollbacks.
 * This class offers a structured way to manage a series of tasks and handles their execution flow and potential rollbacks.
 * Example usage:
 *
 * // Create a ControlFlow instance
 * val controlFlow = ControlFlow(object : WorkFlowTracker {
 *      // Implement work Flow callback methods
 *  })
 *
 * // Define task sequence
 * controlFlow.startWith(Task1())
 * controlFlow.then(Task2())
 *
 * // Set up callbacks for task and rollback task execution progress
 * controlFlow.useTaskStatusTracker(object : TaskStatusTracker {
 *     // Implement callback methods
 * })
 * controlFlow.useRollbackStatusTracker(object : RollbackStatusTracker {
 *     // Implement rollback callback methods
 * })
 *
 * // Start executing tasks
 * controlFlow.start()
 *
 * // If an error occurs during execution, initiate rollback manually
 * controlFlow.startRollback()
 *
 * // Restart execution from a specific task or task index
 * controlFlow.startFrom(taskName = "Task2")
 * controlFlow.startFrom(taskIndex = 1)
 *
 * // Restart the entire sequence
 * controlFlow.restart()
 */
@Keep
class ControlFlow(
    private val workFlowTracker: WorkFlowTracker
): ViewModel(), WorkFlowTracker by workFlowTracker {

    private val handler = CoroutineExceptionHandler { _, exception ->
        // Handles exceptions that occur within coroutines
        exception.message?.let { Log.e("ControlFlow", it) }
    }

    // Manages the execution of tasks within a coroutine scope
    private fun safeLauncher(block: suspend CoroutineScope.() -> Unit): Job {
       return viewModelScope.launch(handler, block = block)
    }

    // Stores the original sequence of tasks
    private var originalTasks: MutableList<TaskProcessor> = mutableListOf()


    // Stores the original sequence of rollback tasks
    private var originalRollbackTasks: MutableList<RollbackTaskProcessor> = mutableListOf()

    // Stores the tasks to be executed
    private val tasks: MutableList<TaskProcessor> = mutableListOf()

    // Stores the rollback tasks to be executed
    private val rollbackTasks: MutableList<RollbackTaskProcessor> = mutableListOf()

    // Stores the completed tasks during execution
    private val _completedTasks: MutableList<TaskProcessor>  = mutableListOf()

    // Provides access to the list of completed tasks
    val completedTasks: List<TaskProcessor>
        get() { return _completedTasks.toList() }

    // Stores the completed rollback tasks during execution
    private val _completedRollbackTasks: MutableList<RollbackTaskProcessor>  = mutableListOf()

    // Provides access to the list of completed rollback tasks
    val completedRollbackTasks: List<RollbackTaskProcessor>
        get() { return _completedRollbackTasks.toList() }

    // Tracks if the rollback should be automatically initiated
    private var runAutomaticallyRollback: AtomicReference<Boolean> = AtomicReference(false)

    // Callback for the main task execution progress
    private var taskStatusTracker: TaskStatusTracker? = null

    // Callback for the rollback task execution progress
    private var rollbackStatusTracker: RollbackStatusTracker? = null

    // Coroutine job for executing tasks
    private lateinit var taskJob: Job

    // Coroutine job for executing rollback tasks
    private lateinit var rollbackTaskJob: Job

    private var taskResult: Any? = null

    private var taskIsCurrentlyInProgress: Boolean? = null

    /**
     * Add the first task to the control flow sequence.
     * @param first task to be added to the control flow sequence.
     */
    fun startWith(first: TaskProcessor) {
        originalTasks.clear()
        originalTasks.add(first)
        tasks.clear()
        tasks.add(first)
    }

    /**
     * Adds the next task to the control flow sequence.
     * @param next The subsequent task to be added to the control flow sequence.
     */
    fun then(next: TaskProcessor) {
        originalTasks.add(next)
        tasks.add(next)
    }

    /**
     * Starts executing the tasks in the control flow sequence.
     * @param runAutomaticallyRollback Set to true if you want tasks to automatically rollback on failure.
     */
    fun start(runAutomaticallyRollback: Boolean = false) {
        started(this@ControlFlow)
        this.runAutomaticallyRollback.set(runAutomaticallyRollback)
        runTasks()
    }

    private fun runTasks() {
        taskJob = safeLauncher {
            while (isActive) {
                when(tasks.size > 0) {
                    true -> {
                        executeTask(task = tasks.first())
                    }
                    else -> {
                        taskJob.cancel()
                    }
                }
            }
        }
    }

    /**
     * Starts executing the rollback tasks in the control flow sequence.
     */

    fun startRollback() {
        rollbackTaskJob = safeLauncher {
            while (isActive) {
                if(rollbackTasks.size > 0){
                    executeRollback(task = rollbackTasks.last())
                }else {
                    rollbackTaskJob.cancel()
                }
            }
        }
    }

    /**
     * Restarts the control flow from the beginning.
     */
    fun restart() {
        reset()
        start(runAutomaticallyRollback.get())
    }

    /**
     * Restarts the rollback process.
     */
    fun restartRollback() {
        resetRollback()
        startRollback()
    }

    /**
     * Starts executing tasks from a specific task name in the sequence.
     * @param taskName The name of the task from which to start the execution.
     */
    fun startFrom(taskName: String) {
        val index = originalTasks.indexOfFirst { it.info.name == taskName }
        if (index == -1) {
            // Task not found, throw an exception or handle it accordingly
            return
        }
        setNewTask(index= index)
        start(runAutomaticallyRollback.get())
    }

    /**
     * Starts executing tasks from a specific task index in the sequence.
     * @param taskIndex The index of the task from which to start the execution.
     */
    fun startFrom(taskIndex: Int) {
        val index = originalTasks.indexOfFirst { it.info.index == taskIndex }
        if (index == -1) {
            // Task not found, throw an exception or handle it accordingly
            return
        }
        setNewTask(index= index)
        start(runAutomaticallyRollback.get())
    }

    private fun setNewTask(index: Int) {
        val tasksToExecute = originalTasks.subList(index, originalTasks.size)
        tasks.clear() // Clear existing tasks
        tasks.addAll(tasksToExecute) // Set tasks to execute from the specified task onwards
    }

    /**
     * Initiates the rollback process from a specific task name in the rollback sequence.
     * @param taskName The name of the task from which to start the rollback process.
     */
    fun startRollbackFrom(taskName: String) {
        val index = originalRollbackTasks.indexOfFirst { it.info.name == taskName }
        if (index == -1) {
            // Task not found, throw an exception or handle it accordingly
            return
        }
        setNewRollbackTask(index= index)
        startRollback()
    }

    /**
     * Initiates the rollback process from a specific task index in the rollback sequence.
     * @param taskIndex The index of the task from which to start the rollback process.
     */
    fun startRollbackFrom(taskIndex: Int) {
        val index = originalRollbackTasks.indexOfFirst { it.info.index == taskIndex }
        if (index == -1) {
            // Task not found, throw an exception or handle it accordingly
            return
        }
        setNewRollbackTask(index= index)
        startRollback()
    }

    private fun setNewRollbackTask(index: Int){
        val tasksToExecute = originalRollbackTasks.subList(index, originalRollbackTasks.size)
        rollbackTasks.clear() // Clear existing tasks
        rollbackTasks.addAll(tasksToExecute) // Set tasks to execute from the specified task onwards

    }

    /**
     * Associates a callback for the main task execution.
     * @param callBack The callback to be associated with the main task execution.
     */
    fun useTaskStatusTracker(callBack: TaskStatusTracker){
        this.taskStatusTracker = callBack
    }

    /**
     * Associates a callback for the rollback task execution.
     * @param callBack The callback to be associated with the rollback task execution.
     */
    fun useRollbackStatusTracker( callBack: RollbackStatusTracker){
        this.rollbackStatusTracker = callBack
    }

    private suspend fun executeTask(task: TaskProcessor) {
        val taskFlow = withContext(task.info.runIn) { task.doProcess(param = taskResult) }
        taskFlow
            .onStart {
                if(taskIsCurrentlyInProgress == null){
                    taskStatus(controlFlow = this@ControlFlow, taskFlow = TaskFlow().apply {
                        taskIndex = task.info.index
                        taskName = task.info.name },state= State.Started)
                    delay(10L)
                    taskStatus(controlFlow = this@ControlFlow,taskFlow = TaskFlow().apply {
                        taskIndex = task.info.index
                        taskName = task.info.name },state= State.InProgress)
                }
            }
            .retryWhen { cause, attempt ->
                val isUseRetryStrategy = task.info.retry?.count != null &&
                     task.info.retry?.count!! > 0 &&
                    !task.info.retry?.causes.isNullOrEmpty() &&
                     task.info.retry?.delay != null && task.info.retry?.delay!! > 0L

                when(isUseRetryStrategy) {
                    true -> {
                        if(task.info.retry?.count!! >= attempt+1 && task.info.retry?.causes?.contains(cause::class) == true){
                            taskIsCurrentlyInProgress = true
                            delay(task.info.retry?.delay!!)
                            return@retryWhen true
                        }else {
                            return@retryWhen false
                        }
                    }
                    false -> {
                        return@retryWhen false
                    }
                }

            }
            .catch {
                tasks.remove(element = task)
                taskStatusTracker?.failure(controlFlow = this@ControlFlow, info = task.info, errorCause = it)
                if(runAutomaticallyRollback.get() == true) {
                    if (rollbackTasks.size > 0) {
                        startRollback()
                    }else{
                        completed(controlFlow = this@ControlFlow)
                    }
                } else{
                    completed(controlFlow = this@ControlFlow)
                }
                taskIsCurrentlyInProgress = null
                taskJob.cancel()
            }
            .collect { taskStatus ->
                handleTaskStatus(task, taskStatus)
                delay(10L)
            }
    }

    private suspend fun executeRollback(task: RollbackTaskProcessor) {
        val rollbackFlow = withContext(task.rollbackInfo.runIn) { task.doRollbackProcess() }
        rollbackFlow
            .onStart {
                if(taskIsCurrentlyInProgress == null){
                    taskStatus(controlFlow = this@ControlFlow,taskFlow = TaskFlow().apply {
                        taskIndex = task.rollbackInfo.index
                        taskName = task.rollbackInfo.name
                        isRollback = true
                    }, state = State.Started)
                    delay(10L)
                    taskStatus(controlFlow = this@ControlFlow,taskFlow = TaskFlow().apply {
                        taskIndex = task.rollbackInfo.index
                        taskName = task.rollbackInfo.name
                        isRollback = true
                    }, state = State.InProgress)
                }
            }
            .retryWhen { cause, attempt ->
                val isUseRetryStrategy = task.rollbackInfo.retry?.count != null &&
                        task.rollbackInfo.retry?.count!! > 0 &&
                        !task.rollbackInfo.retry?.causes.isNullOrEmpty() &&
                        task.rollbackInfo.retry?.delay != null && task.rollbackInfo.retry?.delay!! > 0L

                when(isUseRetryStrategy) {
                    true -> {
                        if(task.rollbackInfo.retry?.count!! >= attempt+1 && task.rollbackInfo.retry?.causes?.contains(cause::class) == true){
                            taskIsCurrentlyInProgress = true
                            delay(task.rollbackInfo.retry?.delay!!)
                            return@retryWhen true
                        }else {
                            return@retryWhen false
                        }
                    }
                    false -> {
                        return@retryWhen false
                    }
                }

            }
            .catch {
                rollbackTasks.remove(element = task)
                rollbackStatusTracker?.failure(controlFlow = this@ControlFlow, info = task.rollbackInfo,errorCause= it)
                completed(controlFlow = this@ControlFlow)
                rollbackTaskJob.cancel()
                taskIsCurrentlyInProgress = null
            }
            .collect { rollbackStatus ->
                handleRollbackStatus(task, rollbackStatus)
                delay(10L)
            }
    }

    private fun handleTaskStatus(task: TaskProcessor, taskStatus: TaskStatus) {
        when (taskStatus) {
            is TaskStatus.DoneSuccessfully<*> -> {
                tasks.remove(element = task)
                _completedTasks.add(task)
                taskResult = taskStatus.result
                taskStatusTracker?.successful(controlFlow = this@ControlFlow, info = task.info, result = taskStatus.result)
                if(task is RollbackTaskProcessor) {
                    originalRollbackTasks.add(task)
                    rollbackTasks.add(task)
                }
                if(tasks.isEmpty()) {
                    completed(controlFlow = this@ControlFlow)
                    taskJob.cancel()}
            }
        }
    }

    private fun handleRollbackStatus(task: RollbackTaskProcessor, rollbackStatus: TaskStatus) {
        when (rollbackStatus) {
            is TaskStatus.DoneSuccessfully<*> -> {
                rollbackTasks.remove(element = task)
                _completedRollbackTasks.add(task)
                rollbackStatusTracker?.successful(controlFlow= this@ControlFlow, info = task.rollbackInfo, result = rollbackStatus.result)
                if(rollbackTasks.isEmpty()) {
                    completed(controlFlow = this@ControlFlow)
                    rollbackTaskJob.cancel()
                }
            }
        }

    }

    private fun reset(){
        originalRollbackTasks = mutableListOf()
        tasks.clear()
        _completedTasks.clear()
        tasks.addAll(originalTasks)
        taskIsCurrentlyInProgress = null

    }

    private fun resetRollback() {
        rollbackTasks.clear()
        rollbackTasks.addAll(originalRollbackTasks)
        _completedRollbackTasks.clear()
        taskIsCurrentlyInProgress = null
    }
}