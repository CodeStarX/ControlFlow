package io.github.codestarx.models

import androidx.annotation.Keep

@Keep
class TaskFlow {
    var taskIndex: Int? = null
    var taskName: String? = null
    var isRollback: Boolean? = false
}