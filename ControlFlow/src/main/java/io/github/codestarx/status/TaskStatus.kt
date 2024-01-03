package io.github.codestarx.status

import androidx.annotation.Keep

@Keep
 sealed class TaskStatus {
    @Keep
    data class DoneSuccessfully<T>(val result: T) : TaskStatus()
}
