package io.github.codestarx

import io.github.codestarx.core.BaseUnitTest
import io.github.codestarx.helper.Dispatcher
import io.github.codestarx.models.ConditionData
import io.github.codestarx.models.TransformData
import io.github.codestarx.status.TaskStatus
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DispatcherTest: BaseUnitTest() {

    private val dispatcher = object : Dispatcher() {}

    @Test
    fun `test launchAwait with action`() = runTest {
        val expectedResult = "Success"
        val actionMock = mockk< (onContinuation: (Any) -> Unit, onFailure: (Throwable) -> Unit) -> Unit>()

        coEvery { actionMock.invoke(any(),any()) } answers {
            val onContinuation = firstArg<(Any) -> Unit>()
            onContinuation.invoke(expectedResult)
        }

        val result = mutableListOf<TaskStatus>()
        val flow = dispatcher.launchAwait(actionMock)
        flow.collect {
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*>)
        assert((result.first() as TaskStatus.DoneSuccessfully<*>).result == expectedResult)
    }

    @Test
    fun `test launchAwait with action and condition`() = runTest {
        val expectedResult = "action result"
        val actionMock = mockk< (onContinuation: (Any) -> Unit, onFailure: (Throwable) -> Unit) -> Unit>()
        val conditionMock = mockk<(Any) -> ConditionData>()

        coEvery { actionMock.invoke(any(),any()) } answers {
            val onContinuation = firstArg<(Any) -> Unit>()
            onContinuation.invoke(expectedResult)
        }
        coEvery { conditionMock.invoke(any()) } returns ConditionData(status = false, throwable = Throwable("an error occurs during execution"))

        val result = mutableListOf<Throwable>()
       val flow = dispatcher.launchAwait(actionMock, conditionMock)
        flow
            .catch {
                result.add(it)
            }
            .collect {}

        assert(result.size == 1)
        assert(result.first().message == "an error occurs during execution")
    }


    @Test
    fun `test launchAwait with action,condition,transformer`() = runTest {
        val expectedResult = "action result"
        val actionMock = mockk< (onContinuation: (Any) -> Unit, onFailure: (Throwable) -> Unit) -> Unit>()
        val conditionMock = mockk<(Any) -> ConditionData>()
        val transformerMock = mockk<(Any) -> TransformData<Any>>()

        coEvery { actionMock.invoke(any(),any()) } answers {
            val onContinuation = firstArg<(Any) -> Unit>()
            onContinuation.invoke(expectedResult)
        }
        coEvery { conditionMock.invoke(any()) } returns ConditionData(status = true)
        coEvery { transformerMock.invoke(any()) } returns TransformData(data = 14000704)

        val result = mutableListOf<TaskStatus>()
        dispatcher.launchAwait(actionMock,conditionMock,transformerMock).collect{
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*>)
        assert((result.first() as TaskStatus.DoneSuccessfully<*>).result == 14000704 )

    }

    @Test
    fun `test launchAwait with action and transformer`() = runTest {
        val expectedResult = "action result"
        val actionMock = mockk< (onContinuation: (Any) -> Unit, onFailure: (Throwable) -> Unit) -> Unit>()
        val transformerMock = mockk<(Any) -> TransformData<Any>>()

        coEvery { actionMock.invoke(any(),any()) } answers {
            val onContinuation = firstArg<(Any) -> Unit>()
            onContinuation.invoke(expectedResult)
        }
        coEvery { transformerMock.invoke(any()) } returns TransformData(data = 14000704)

        val result = mutableListOf<TaskStatus>()
        dispatcher.launchAwait(actionMock,transformerMock).collect{
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*>)
        assert((result.first() as TaskStatus.DoneSuccessfully<*>).result == 14000704 )

    }
    @Test
    fun `test launchUnitAwait`() = runTest {
        val actionMock = mockk< (onContinuation: () -> Unit, onFailure: (Throwable) -> Unit) -> Unit>()

        coEvery { actionMock.invoke(any(),any()) } answers {
            val onContinuation = firstArg<() -> Unit>()
            onContinuation.invoke()
        }

        val result = mutableListOf<TaskStatus>()
        dispatcher.launchUnitAwait(actionMock).collect{
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*>)
        assert((result.first() as TaskStatus.DoneSuccessfully<*>).result == Unit )

    }

    @Test
    fun `test launchFlow with action and condition`() = runTest {
        val expectedResult = "action result"
        val actionMock = mockk<(() -> Flow<Any>)>()
        val conditionMock = mockk<(Any) -> ConditionData>()

        coEvery { actionMock.invoke() } returns flowOf(expectedResult)
        coEvery { conditionMock.invoke(any()) } returns ConditionData(status = true)

        val result = mutableListOf<TaskStatus>()
        val flow = dispatcher.launchFlow(actionMock, conditionMock)
        flow.collect {
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*> )
        assert((result.first() as TaskStatus.DoneSuccessfully<*> ).result == "action result")
    }

    @Test
    fun `test launchFlow with action,condition and transformer`() = runTest {
        val expectedResult = "action result"
        val actionMock = mockk<(() -> Flow<Any>)>()
        val conditionMock = mockk<(Any) -> ConditionData>()
        val transformerMock = mockk<(Any) -> TransformData<Any>>()

        coEvery { actionMock.invoke() } returns flowOf(expectedResult)
        coEvery { conditionMock.invoke(any()) } returns ConditionData(status = true)
        coEvery { transformerMock.invoke(any()) } returns TransformData(data = "another data")

        val result = mutableListOf<TaskStatus>()
        val flow = dispatcher.launchFlow(actionMock, conditionMock, transformerMock)
        flow.collect {
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*> )
        assert((result.first() as TaskStatus.DoneSuccessfully<*> ).result == "another data")
    }

    @Test
    fun `test launchFlow with action and transformer`() = runTest {
        val expectedResult = "action result"
        val actionMock = mockk<(() -> Flow<Any>)>()
        val transformerMock = mockk<(Any) -> TransformData<Any>>()

        coEvery { actionMock.invoke() } returns flowOf(expectedResult)
        coEvery { transformerMock.invoke(any()) } returns TransformData(data = "another data")

        val result = mutableListOf<TaskStatus>()
        val flow = dispatcher.launchFlow(actionMock, transformerMock)
        flow.collect {
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*> )
        assert((result.first() as TaskStatus.DoneSuccessfully<*> ).result == "another data")
    }

    @Test
    fun `test launchFlow with action`() = runTest {
        val expectedResult = "action result"
        val actionMock = mockk<(() -> Flow<Any>)>()

        coEvery { actionMock.invoke() } returns flowOf(expectedResult)

        val result = mutableListOf<TaskStatus>()
        val flow = dispatcher.launchFlow(actionMock)
        flow.collect {
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*> )
        assert((result.first() as TaskStatus.DoneSuccessfully<*> ).result == "action result")
    }

    @Test
    fun `test launch with action and condition`() = runTest {
        val expectedResult = "action result"
        val actionMock = mockk<(() -> Any)>()
        val conditionMock = mockk<(Any) -> ConditionData>()

        coEvery { actionMock.invoke() } returns expectedResult
        coEvery { conditionMock.invoke(any()) } returns ConditionData(status = false,throwable = Throwable("an error occurs during execution"))

        val result = mutableListOf<Throwable>()
        val flow = dispatcher.launch(actionMock, conditionMock)
        flow
            .catch {
                result.add(it)
            }
            .collect()

        assert(result.size == 1)
        assert(result.first().message == "an error occurs during execution")

    }

    @Test
    fun `test launch with action`() = runTest {
        val expectedResult = Throwable("an error occurs during execution")
        val actionMock = mockk<(() -> Any)>()

        coEvery { actionMock.invoke() } answers {
            throw expectedResult
        }

        val result = mutableListOf<Throwable>()
        val flow = dispatcher.launch(actionMock)
        flow.catch {
            result.add(it)
        }.collect()

        assert(result.size == 1)
        assert(result.first().message == "an error occurs during execution")

    }

    @Test
    fun `test launch with action and transformer`() = runTest {
        val expectedResult = "action result"
        val actionMock = mockk<(() -> Any)>()
        val transformerMock = mockk<(Any) -> TransformData<Any>>()

        coEvery { actionMock.invoke() } returns expectedResult
        coEvery { transformerMock.invoke(any()) } returns TransformData(data = "another data")

        val result = mutableListOf<TaskStatus>()
        val flow = dispatcher.launch(actionMock, transformerMock)
        flow.collect {
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*> )
        assert((result.first() as TaskStatus.DoneSuccessfully<*> ).result == "another data")
    }
}
