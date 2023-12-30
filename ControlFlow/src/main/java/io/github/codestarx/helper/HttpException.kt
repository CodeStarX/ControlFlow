package io.github.codestarx.helper

import androidx.annotation.Keep

@Keep
// Custom exception for HTTP errors
class HttpException(val code: Int?, message: String?) : Throwable(message)
