package io.github.codestarx.interfaces

import androidx.annotation.Keep
import io.github.codestarx.status.TaskStatus
import io.github.codestarx.models.TaskInfo
import kotlinx.coroutines.flow.Flow

@Keep
interface TaskProcessor {
    val info: TaskInfo
    suspend fun doProcess(param: Any?): Flow<TaskStatus>
    companion object {
        var subtasks: MutableMap<Int, MutableList<TaskProcessor>> = mutableMapOf()
    }
    fun then(subtask: TaskProcessor) {
        val key = this@TaskProcessor.hashCode()
        if(!subtasks.containsKey(key)){
            subtasks[key] = mutableListOf(subtask)
        }else {
            val values = subtasks.getValue(key)
            values.add(subtask)
            subtasks[key] = values
        }
    }
}