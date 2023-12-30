package io.github.codestarx.interfaces

import androidx.annotation.Keep
import io.github.codestarx.ControlFlow
import io.github.codestarx.models.TaskInfo

@Keep
interface TaskStatusTracker {
    fun successful(controlFlow: ControlFlow, info: TaskInfo, result: Any?)
    fun failure(controlFlow: ControlFlow, info: TaskInfo, errorCause: Throwable?)

}