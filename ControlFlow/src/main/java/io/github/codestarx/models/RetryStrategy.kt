package io.github.codestarx.models

import androidx.annotation.Keep
import kotlin.reflect.KClass

@Keep
class RetryStrategy {
    var count: Int? = null
    var causes: Set<KClass<*>>? = null
    var delay: Long? = null
}