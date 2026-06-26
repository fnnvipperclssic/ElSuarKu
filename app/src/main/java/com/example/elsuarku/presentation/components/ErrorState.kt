package com.example.elsuarku.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.DeepBlueLight
import com.example.elsuarku.ui.theme.StatusError

/**
 * Reusable full-screen error state with icon, title, subtitle, and optional retry button.
 *
 * Matches the design system: GlassCard aesthetic with DeepBlue color scheme.
 *
 * @param title Primary error message
 * @param subtitle Secondary details or recovery suggestion
 * @param onRetry Optional retry callback — hides the button if null
 * @param retryLabel Custom label for the retry button (default: "Coba Lagi")
 * @param isNetworkError If true, shows a Wi-Fi off icon instead of generic error
 */
@Composable
fun ErrorState(
    title: String,
    subtitle: String? = null,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Coba Lagi",
    isNetworkError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isNetworkError) Icons.Filled.WifiOff else Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = StatusError.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DeepBlueDark,
                    textAlign = TextAlign.Center
                )

                if (subtitle != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepBlueLight,
                        textAlign = TextAlign.Center
                    )
                }

                if (onRetry != null) {
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(retryLabel)
                    }
                }
            }
        }
    }
}

/**
 * Compact inline error banner — suitable for use inside LazyColumn or Card layouts.
 * Shows a brief error message with a retry text button.
 */
@Composable
fun ErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StatusError.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = StatusError,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text("Coba Lagi", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
