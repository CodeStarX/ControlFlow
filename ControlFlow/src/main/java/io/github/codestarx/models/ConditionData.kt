package io.github.codestarx.models

import androidx.annotation.Keep

@Keep
data class ConditionData(val status: Boolean?, val throwable: Throwable? = null)