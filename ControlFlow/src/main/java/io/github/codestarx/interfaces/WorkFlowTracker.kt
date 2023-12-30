package io.github.codestarx.interfaces

import androidx.annotation.Keep
import io.github.codestarx.ControlFlow
import io.github.codestarx.models.TaskFlow
import io.github.codestarx.status.State

@Keep
interface WorkFlowTracker {
    fun started(controlFlow: ControlFlow)
    fun taskStatus(controlFlow: ControlFlow, taskFlow: TaskFlow, state: State)
    fun completed(controlFlow: ControlFlow)
}