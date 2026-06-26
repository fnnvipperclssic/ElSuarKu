package com.example.elsuarku.presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Global snackbar message bus.
 *
 * Decouples snackbar display from individual screens — any ViewModel or
 * composable can emit a message, and the root Scaffold's SnackbarHost
 * will display it.
 *
 * ## Setup (in MainActivity or root NavHost)
 * ```kotlin
 * val snackbarHostState = remember { SnackbarHostState() }
 * LaunchedEffect(Unit) {
 *     SnackbarManager.collectMessages(scope = this, hostState = snackbarHostState)
 * }
 * ```
 *
 * ## Usage (anywhere)
 * ```kotlin
 * SnackbarManager.show("Suara berhasil disimpan!")
 * SnackbarManager.showError("Gagal terhubung ke server")
 * ```
 */
object SnackbarManager {

    private val _messages = MutableSharedFlow<SnackbarMessage>(
        extraBufferCapacity = 10
    )
    val messages: SharedFlow<SnackbarMessage> = _messages.asSharedFlow()

    data class SnackbarMessage(
        val text: String,
        val isError: Boolean = false,
        val duration: SnackbarDuration = SnackbarDuration.Short
    )

    /**
     * Collect messages and display them via the SnackbarHostState.
     * Call once at the root composable level.
     */
    fun collectMessages(
        scope: CoroutineScope,
        hostState: SnackbarHostState
    ) {
        scope.launch {
            messages.collect { message ->
                hostState.currentSnackbarData?.dismiss()
                hostState.showSnackbar(
                    message = message.text,
                    duration = message.duration
                )
            }
        }
    }

    /**
     * Show a success message (green toast).
     */
    fun show(message: String) {
        _messages.tryEmit(SnackbarMessage(text = message, isError = false))
    }

    /**
     * Show an error message.
     */
    fun showError(message: String) {
        _messages.tryEmit(
            SnackbarMessage(text = message, isError = true, duration = SnackbarDuration.Long)
        )
    }

    /**
     * Show a temporary status update (during operations).
     */
    fun showStatus(message: String) {
        _messages.tryEmit(
            SnackbarMessage(text = message, duration = SnackbarDuration.Indefinite)
        )
    }
}
