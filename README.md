# ControlFlow

ControlFlow is an Android library that facilitates task sequencing, rollback actions, and error handling. It systematically oversees task execution, offering structured error handling and facilitating rollback processes for efficient management.

Explore the implementation of the controlflow library through the code samples available in the [ControlFlowDemo](https://github.com/CodeStarX/ControlFlowDemo). repository.

## Features

- **Task Execution Sequencing:** Define and manage a sequence of tasks.
- **Subtasks Execution Sequencing:** Define and manage a sequence of subtasks for each primary task.
- **Rollback Mechanism:** Implement rollback functionalities for tasks that need to revert changes made during their execution.
- **Error Handling:** Handle errors occurring during task execution and initiate rollback processes if required.
- **Automated Data Forwarding:** Each task's output data type is automatically forwarded as the input for the subsequent task by default.


## Installation

To include ControlFlow in your Android project, add the following dependency to your app's `build.gradle` file:

```kotlin
implementation("io.github.codestarx:control-flow:1.0.0-alpha10")

repositories {
  //..
  //..
  mavenCentral()
}
```


### Task Execution with ControlFlow

1. **Task Management:** Create instances of tasks inheriting from `Dispatcher` and implementing `TaskProcessor`.
2. **ControlFlow Class:** Use the `ControlFlow` class to manage task sequences and their execution flow.
3. **Start Execution:** Begin executing tasks using `start()` method.


## Usage

### Task Implementation

Inherit from Dispatcher: Create tasks by inheriting from the `Dispatcher` class and implementing the `TaskProcessor` properties. For example:

```kotlin
class MyTask : Dispatcher(), TaskProcessor {
          override val info: TaskInfo
            get() = TaskInfo().apply {
              index = 0
              name = MyTask::class.java.name
              runIn = Dispatchers.IO
            }
        
          override suspend fun doProcess(param: Any?): Flow<TaskStatus> {
            // Define the action here
            return launchAwait(
              action = {
                // Perform the action
                // ...
              },
              transformer = {
                // The output generated by this function will serve as the input for the subsequent task
                // Return TransformData(data= ...)  
              },
              actionCondition = {
                // Define conditions for continuation or breaking
                // Return ConditionData(status = ... , throwable = ... )
              }
            )
          }
}
 ```


### Handling Rollback

Tasks implementing rollback functionality should also override methods from the `RollbackTaskProcessor` interface, specifying rollback actions.

Example of a task with rollback:
```kotlin
class MyRollbackTask : Dispatcher(), RollbackTaskProcessor {
          override val info: TaskInfo
            get() = TaskInfo().apply {
              index = 0
              name = MyRollbackTask::class.java.name
              runIn = Dispatchers.IO
            }
        
          override val rollbackInfo: RollbackInfo
            get() = RollbackInfo().apply {
              index = 0
              name = MyRollbackTask::class.java.name
              runIn = Dispatchers.IO
            }
        
          override suspend fun doProcess(param: Any?): Flow<TaskStatus> {
            // Define the action for the task
            return launchAwait(
              action = {
                // Perform the action
                // ...
              },
              actionCondition = {
                // Define conditions for continuation or breaking
                // Return ConditionData(status = ... , throwable = ... )
              }
            )
          }
        
          override suspend fun doRollbackProcess(): Flow<TaskStatus> {
            // Define the rollback action here
            return launchAwait(
              action = {
                // Perform the rollback action
                // ...
              },
              actionCondition = {
                // Define conditions for rollback continuation or breaking
                // Return ConditionData(status = ... , throwable = ... )
              }
            )
          }
} 
```

### Automated Data Forwarding

Each task's output data type is automatically forwarded as the input for the subsequent task by default.
If you require altering the output data type passed to the next task, utilize the `transformer` method for this purpose.

Example:

```kotlin
class Task : Dispatcher(), TaskProcessor {
  override val info: TaskInfo
    get() = TaskInfo().apply {
      index = 0
      name = Task::class.java.name
      runIn = Dispatchers.IO
    }

  override suspend fun doProcess(param: Any?): Flow<TaskStatus> {
    // Define the action here
    return launchAwait(
      action = {
        // Perform the action
        // ...
      },
      transformer = {
        // The output generated by this function will serve as the input for the subsequent task
        // Return TransformData(data= ...)  
      },
      actionCondition = {
        // Define conditions for continuation or breaking
        // Return ConditionData(status = ... , throwable = ... )
      }
    )
  }
}

```

### Attributes Of Each Task

The attributes of each task are outlined using the `TaskInfo` class.
The ‍‍‍‍`index`, `name` and `runIn` parameters define the task's specifications and execution thread. By utilizing `index` or `name` and the `startFrom` method within the `ControlFlow`, tasks can be rerun as needed.

Example:

```kotlin
class Task : Dispatcher(), TaskProcessor {
  get() = TaskInfo().apply {
    index = 0
    name = Task::class.java.name
    runIn = Dispatchers.IO
  }

  override suspend fun doProcess(param: Any?): Flow<TaskStatus> {
    ...
  }

```
### Activate The Retry Mechanism For Each Task

To activate the Retry mechanism for each task, set the `count` to define the number of retries in case of failure. 
Additionally, assign specific `causes`, a list of errors, to trigger retries upon encountering these errors. 
Adjust the `delay` value to determine the interval between each retry attempt.

Example:

```kotlin
class Task : Dispatcher(), TaskProcessor {
  get() = TaskInfo().apply {
    index = 0
    name = Task::class.java.name
    retry = RetryStrategy().apply {
      count = 2
      causes = setOf(TimeoutException::class,AnotherException::class,...)
      delay = 1000L
    }
    runIn = Dispatchers.IO
  }

  override suspend fun doProcess(param: Any?): Flow<TaskStatus> {
    ...
  }

```

### Attributes Of Each Rollback Task

The attributes of each task are outlined using the `RollbackInfo` class.
The ‍‍‍‍`index`, `name` and `runIn` parameters define the task's specifications and execution thread. By utilizing `index` or `name` and the `startRollbackFrom` method within the `ControlFlow`, tasks can be rerun as needed.

Example:

```kotlin
class Task : Dispatcher(), RollbackTaskProcessor {
  override val info: TaskInfo
    get() = TaskInfo().apply {
      index = 0
      name = Task::class.java.name
      runIn = Dispatchers.IO
    }

  override val rollbackInfo: RollbackInfo
    get() = RollbackInfo().apply {
      index = 0
      name = Task::class.java.name
      runIn = Dispatchers.IO
    }

  override suspend fun doProcess(param: Any?): Flow<TaskStatus> {
    ...
  }

  override suspend fun doRollbackProcess(): Flow<TaskStatus> {
    ...
  }

```

### Activate The Retry Mechanism For Each Rollback Task

To activate the Retry mechanism for each rollback task, set the `count` to define the number of retries in case of failure.
Additionally, assign specific `causes`, a list of errors, to trigger retries upon encountering these errors.
Adjust the `delay` value to determine the interval between each retry attempt.

Example:

```kotlin
class Task : Dispatcher(), RollbackTaskProcessor {
  override val info: TaskInfo
    get() = ...

  override val rollbackInfo: RollbackInfo
    get() = RollbackInfo().apply {
      index = 0
      name = Task::class.java.name
      retry = RetryStrategy().apply {
        count = 2
        causes = setOf(TimeoutException::class,AnotherException::class,...)
        delay = 1000L
      }
      runIn = Dispatchers.IO
    }

  override suspend fun doProcess(param: Any?): Flow<TaskStatus> {
    ...
  }

  override suspend fun doRollbackProcess(): Flow<TaskStatus> {
    ...
  }

```

## ControlFlow Class
`ControlFlow` manages the execution sequence of tasks and potential rollback actions.
It orchestrates the execution, rollback, completion, and error handling of tasks and their rollbacks.
This class offers a structured way to manage a series of tasks and handles their execution flow and potential rollbacks.

### Running Control Flow

Example usage:

```kotlin
    // Create a ControlFlow instance
val controlFlow = ControlFlow(object : WorkFlowTracker {
  // Implement work Flow callback methods
})

// Define your tasks
controlFlow.startWith(MyTask())
controlFlow.then(AnotherTask())
controlFlow.then(AnotherTask())

// Set up TaskStatusTracker if needed
controlFlow.useTaskStatusTracker(object : TaskStatusTracker {
  // Implement callback methods
})

// Set up RollbackStatusTracker if needed
controlFlow.useRollbackStatusTracker(object : RollbackStatusTracker {
  // Implement callback methods
})

// Start executing tasks
controlFlow.start()
```

### Subtasks Execution
To incorporate subtasks for each task, you can define their implementation as outlined below:

Example usage:

```kotlin
// Create a ControlFlow instance
val controlFlow = ControlFlow(object : WorkFlowTracker {
  // Implement work Flow callback methods
})

// Define your tasks
controlFlow.startWith(MyTask().apply{
  // Define your subtasks
  then(subtask= MySubtask())
  then(subtask= AnotherSubtask())
})
controlFlow.then(AnotherTask().apply{
  // Define your subtasks
  then(subtask= MySubtask())
  then(subtask= AnotherSubtask())
})

// Set up TaskStatusTracker if needed
controlFlow.useTaskStatusTracker(object : TaskStatusTracker {
  // Implement callback methods
})

// Set up RollbackStatusTracker if needed
controlFlow.useRollbackStatusTracker(object : RollbackStatusTracker {
  // Implement callback methods
})

// Start executing tasks
controlFlow.start()
```

### Control-Flow Method Details
```kotlin
/**
 * Add the first task to the control flow sequence.
 * @param first task to be added to the control flow sequence.
 */
startWith(first: TaskProcessor) 
```
```kotlin
/**
 * Adds the next task to the control flow sequence.
 * @param next The subsequent task to be added to the control flow sequence.
 */
then(next: TaskProcessor)
```
```kotlin
/**
 * Starts executing the tasks in the control flow sequence.
 * @param runAutomaticallyRollback Set to true if you want tasks to automatically rollback on failure.
 */
start(runAutomaticallyRollback: Boolean = false)
```
```kotlin
/**
 * Starts executing tasks from a specific task name in the sequence.
 * @param taskName The name of the task from which to start the execution.
 */
startFrom(taskName: String)
```
```kotlin
/**
 * Starts executing tasks from a specific task index in the sequence.
 * @param taskIndex The index of the task from which to start the execution.
 */
startFrom(taskIndex: Int)
```
```kotlin
/**
 * Restarts the control flow from the beginning.
 */
restart() 
```
```kotlin
/**
 * Starts executing the rollback tasks in the control flow sequence.
 */
startRollback()
```
```kotlin
/**
 * Initiates the rollback process from a specific task name in the rollback sequence.
 * @param taskName The name of the task from which to start the rollback process.
 */
startRollbackFrom(taskName: String)
```
```kotlin
/**
 * Initiates the rollback process from a specific task index in the rollback sequence.
 * @param taskIndex The index of the task from which to start the rollback process.
 */
startRollbackFrom(taskIndex: Int) 
```
```kotlin
/**
 * Restarts the rollback process.
 */
restartRollback() 
```
```kotlin
/**
 * Associates a callback for the main task execution.
 * @param callBack The callback to be associated with the main task execution.
 */
useTaskStatusTracker(callBack: TaskStatusTracker)
```
```kotlin
/**
 * Associates a callback for the rollback task execution.
 * @param callBack The callback to be associated with the rollback task execution.
 */
useRollbackStatusTracker( callBack: RollbackStatusTracker)
```

## Dispatcher Class

The `Dispatcher` class serves as a utility to execute asynchronous actions, manage errors, and handle various scenarios using coroutines. This class offers a range of methods to facilitate asynchronous operations and streamline error handling within tasks.

### Method Details

#### `safeLauncher`

- **Usage**: Executes a provided coroutine block within the `viewModelScope`, managing potential exceptions via a `CoroutineExceptionHandler`.
- **Example**:
  ```kotlin
  safeLauncher {
      // Coroutine block to execute
      // ...
  }
  ```

#### `launchAwait`

- **Usage**: Executes an asynchronous action and emits the result or errors via a Flow, based on specified custom conditions.
- **Example**:

```kotlin
  launchAwait(
  // Action to execute asynchronously with callbacks for continuation and failure
  { onContinuation, onFailure ->
    // Invoke the action and handle results
    // Call onContinuation with the result to continue or onFailure with an error to break
  },
  // Condition to check if continuation or breaking is required based on the result
  { result ->
     // Define conditions for rollback continuation or breaking
     // Return ConditionData(status = ... , throwable = ... )
  }
)
  ```

#### `launchUnitAwait`

- **Usage**: Executes an asynchronous action that returns a Unit result and emits the status via a Flow.
- **Example**:
  ```kotlin
  launchUnitAwait { onContinuation, onFailure ->
      // Invoke the action and handle results
      // Call onContinuation to indicate successful completion or onFailure with an error
  }
  ```

#### `launchFlow`

- **Usage**: Executes an asynchronous action returning a Flow, evaluates conditions for success or breaking, and emits the status accordingly.
- **Example**:
  ```kotlin
  launchFlow(
      // Action returning a Flow
      { flowAction() },
      // Condition to check if continuation or breaking is required based on the Flow's result
      { result ->
          // Define conditions for rollback continuation or breaking
          // Return ConditionData(status = ... , throwable = ... )
      }
  )
  ```

#### `launch`

- **Usage**: Executes a synchronous action, evaluates conditions for success or breaking, and emits the status via a Flow.
- **Example**:
  ```kotlin
  launch(
      // Synchronous action to execute
      { syncAction() },
      // Condition to check if continuation or breaking is required based on the action's result
      { result ->
          // Define conditions for rollback continuation or breaking
          // Return ConditionData(status = ... , throwable = ... )
      }
  )
  ```

## Documentation

For detailed information about classes, methods, and functionalities provided by ControlFlow, refer to the inline comments in the source code.

## Contributing

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement". Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/YourFeatureName`)
3. Commit your Changes (`git commit -m 'Add some YourFeatureName'`)
4. Push to the Branch (`git push origin feature/YourFeatureName`)
5. Open a Pull Request

## LICENSE

The Apache Software License, Version 2.0

http://www.apache.org/licenses/LICENSE-2.0.txt
