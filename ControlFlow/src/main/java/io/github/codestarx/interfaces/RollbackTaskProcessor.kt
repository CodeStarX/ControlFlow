package io.github.codestarx.interfaces

import androidx.annotation.Keep
import io.github.codestarx.models.RollbackInfo
import io.github.codestarx.status.TaskStatus
import kotlinx.coroutines.flow.Flow

@Keep
interface RollbackTaskProcessor : TaskProcessor {
    val rollbackInfo: RollbackInfo
    suspend fun doRollbackProcess(): Flow<TaskStatus>

}