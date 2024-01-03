package io.github.codestarx.models

import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
@Keep
 open class TaskInfo {
     var index: Int? = null
     var name: String? = null
     var retry: RetryStrategy? = null
     var runIn: CoroutineContext = Dispatchers.Default
 }