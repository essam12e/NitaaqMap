package com.nitaaq.map.presentation

import androidx.annotation.StringRes
import com.nitaaq.map.R
import com.nitaaq.map.core.domain.TripError

/** UI-ready error: the server's own Arabic text wins when it sent one. */
data class UiError(
    @StringRes val messageRes: Int,
    val serverMessage: String? = null,
    val formatArg: Int? = null,
    val retryable: Boolean,
    val isConnectivity: Boolean = false,
)

fun TripError.toUiError(): UiError = when (this) {
    TripError.EmptyLink -> UiError(R.string.error_empty, retryable = false)
    is TripError.InvalidLink -> UiError(R.string.error_invalid, serverMessage, retryable = false)
    TripError.NoConnection -> UiError(R.string.error_offline, retryable = true, isConnectivity = true)
    TripError.Timeout -> UiError(R.string.error_timeout, retryable = true, isConnectivity = true)
    is TripError.Server -> UiError(R.string.error_server, serverMessage, formatArg = httpCode, retryable = true)
    is TripError.UntrustedResponse -> UiError(R.string.error_untrusted, retryable = true)
}
