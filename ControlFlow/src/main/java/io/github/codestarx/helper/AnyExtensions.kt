package io.github.codestarx.helper

import androidx.annotation.Keep

@Keep
fun Boolean.Companion.successMode() = true

@Keep
fun Boolean.Companion.failureMode() = false