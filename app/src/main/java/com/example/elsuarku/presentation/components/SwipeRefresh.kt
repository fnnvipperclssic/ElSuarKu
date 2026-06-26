package com.example.elsuarku.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Pull-to-refresh wrapper for list screens.
 *
 * Uses Material3 PullToRefreshBox with the standard indicator.
 * Provides a consistent pull-to-refresh experience across all list screens.
 *
 * @param isRefreshing Whether a refresh is in progress
 * @param onRefresh Callback when user triggers a pull-to-refresh
 * @param content The scrollable content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Pull-to-refresh with optional empty state message.
 * Alias for SwipeRefreshContainer with the same behavior — kept for
 * backward compatibility with existing call sites.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRefreshCompat(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
    content: @Composable () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        content()
    }
}
