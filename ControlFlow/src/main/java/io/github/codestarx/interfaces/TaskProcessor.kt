package io.github.codestarx.interfaces

import androidx.annotation.Keep
import io.github.codestarx.status.TaskStatus
import io.github.codestarx.models.TaskInfo
import kotlinx.coroutines.flow.Flow

@Keep
interface TaskProcessor {
    val info: TaskInfo
    suspend fun doProcess(param: Any?): Flow<TaskStatus>
}