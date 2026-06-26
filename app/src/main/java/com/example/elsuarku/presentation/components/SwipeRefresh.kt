package com.example.elsuarku.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Pull-to-refresh wrapper for list screens.
 *
 * Currently uses a simple loading indicator pattern. Material3 PullToRefresh
 * will be adopted once the experimental API stabilizes in the Compose BOM.
 *
 * @param isRefreshing Whether a refresh is in progress
 * @param onRefresh Callback when user requests refresh (not auto-triggered)
 * @param content The scrollable content
 */
@Composable
fun SwipeRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Simple swipe-to-refresh without Material3 experimental API dependency.
 * Uses a manual trigger-based approach for compatibility with older Compose versions.
 */
@Composable
fun SwipeRefreshCompat(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}
