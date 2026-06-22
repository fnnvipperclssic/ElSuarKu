package com.example.elsuarku.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.elsuarku.ui.theme.DeepBlue
import com.example.elsuarku.ui.theme.StatusError

/**
 * Standard error dialog with optional retry and secondary action.
 *
 * @param title       Dialog title — defaults to "Terjadi Kesalahan"
 * @param message     Error description body
 * @param onDismiss   Called on dismiss / "Tutup"
 * @param onRetry     Optional retry action — adds "Coba Lagi" button
 * @param secondaryButton Optional (callback, label) pair for a fallback action,
 *                        e.g. "Lanjutkan Tanpa Biometrik"
 */
@Composable
fun ErrorDialog(
    title: String = "Terjadi Kesalahan",
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    secondaryButton: Pair<() -> Unit, String>? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = StatusError
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(
                        "Coba Lagi",
                        color = DeepBlue
                    )
                }
            }
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        dismissButton = if (secondaryButton != null) {
            {
                val (action, label) = secondaryButton
                TextButton(onClick = action) {
                    Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        } else null
    )
}

/**
 * Confirmation dialog for critical actions (e.g. submitting a vote).
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Konfirmasi",
    cancelText: String = "Batal",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDestructive) StatusError
                    else DeepBlue
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        }
    )
}
