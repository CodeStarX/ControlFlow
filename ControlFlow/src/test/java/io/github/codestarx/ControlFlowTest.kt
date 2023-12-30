package io.github.codestarx


import io.github.codestarx.core.BaseUnitTest
import io.github.codestarx.interfaces.RollbackTaskProcessor
import io.github.codestarx.interfaces.TaskProcessor
import io.github.codestarx.interfaces.WorkFlowTracker
import io.github.codestarx.models.TaskInfo
import io.github.codestarx.status.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

class ControlFlowTest: BaseUnitTest() {

    private lateinit var controlFlow: ControlFlow

    @MockK
    private lateinit var workFlowTracker: WorkFlowTracker
    @MockK
    private lateinit var taskProcessor: TaskProcessor
    @MockK
    private lateinit var rollbackTaskProcessor: RollbackTaskProcessor


    private val testDispatcher = TestCoroutineDispatcher()

    override fun onSetUpTest() {
        super.onSetUpTest()
        // Set the main dispatcher for testing
        Dispatchers.setMain(testDispatcher)
        controlFlow = ControlFlow(workFlowTracker)
    }

    @Test
    fun `test adding a task with startWith`() {
        controlFlow.startWith(taskProcessor)

        assertEquals(1, (getVariableValue(obj = controlFlow, fieldName = "originalTasks") as MutableList<*>).size)
        assertEquals(1, (getVariableValue(obj = controlFlow, fieldName = "tasks") as MutableList<*>).size)

    }

    @Test
    fun `test adding a task with then`(){
        controlFlow.startWith(taskProcessor)
        controlFlow.then(rollbackTaskProcessor)

        assertEquals(2, (getVariableValue(obj = controlFlow, fieldName = "originalTasks") as MutableList<*>).size)
        assertEquals(2, (getVariableValue(obj = controlFlow, fieldName = "tasks") as MutableList<*>).size)

    }

    @Test
    fun `test start() method`() = runTest {
        val task1 = mockk<TaskProcessor>()
        val param = "test_param"
        val expectedResult = TaskStatus.DoneSuccessfully("Task1 Done")

        every { task1.info } returns TaskInfo().apply {
            index = 0
            name = "Task1"
            runIn = Dispatchers.IO
        }

        coEvery { task1.doProcess(any()) } returns flowOf(expectedResult)
        every { workFlowTracker.started(any()) } just runs
        every { workFlowTracker.taskStatus(any(),any(),any()) } just runs
        every { workFlowTracker.completed(any()) } just runs

        controlFlow.startWith(task1)
        controlFlow.start()

        coVerifyOrder{
            workFlowTracker.started(any())
            task1.info
            task1.doProcess(any())
            workFlowTracker.taskStatus(any(),any(),any())
        }

        val flow = task1.doProcess(param = param)

        val result = mutableListOf<TaskStatus>()
        flow.collect {
            result.add(it)
        }

        assert(result.size == 1)
        assert(result.first() is TaskStatus.DoneSuccessfully<*>)
        assert((result.first() as TaskStatus.DoneSuccessfully<*>).result == expectedResult.result)

    }


    override fun onStopTest() {
        // Reset the main dispatcher after the test completes
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

}
