package com.example.elsuarku.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.StatusError

/**
 * Reusable confirmation dialog with consistent styling.
 *
 * Used for destructive actions (delete election, remove candidate, sign out)
 * and critical operations (submit vote, change role).
 *
 * ## Usage
 * ```kotlin
 * ConfirmationDialog(
 *     title = "Hapus Pemilihan?",
 *     message = "Semua data suara dalam pemilihan ini akan dihapus permanen.",
 *     confirmLabel = "Hapus",
 *     onConfirm = { deleteElection() },
 *     onDismiss = { showDialog = false },
 *     isDestructive = true
 * )
 * ```
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
    dismissLabel: String = "Batal"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepBlueDark
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
