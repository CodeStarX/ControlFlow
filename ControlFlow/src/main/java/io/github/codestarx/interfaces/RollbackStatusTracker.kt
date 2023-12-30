package io.github.codestarx.interfaces

import androidx.annotation.Keep
import io.github.codestarx.ControlFlow
import io.github.codestarx.models.RollbackInfo

@Keep
interface RollbackStatusTracker {
    fun successful(controlFlow: ControlFlow, info: RollbackInfo, result: Any?)
    fun failure(controlFlow: ControlFlow, info: RollbackInfo, errorCause: Throwable?)
}