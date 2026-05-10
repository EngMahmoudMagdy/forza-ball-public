package com.forzaball.core.shared_ui_components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun SwipeRefreshSharedComponent(
    modifier: Modifier = Modifier,
    isRefresh: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    val swipeState = rememberPullToRefreshState()
    Box(
        modifier = modifier.pullToRefresh(isRefresh, swipeState, onRefresh = onRefresh),
        content = {
            content()
        },
    )
}