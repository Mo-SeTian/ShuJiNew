package com.readtrack.presentation.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoadingContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    },
    content: @Composable () -> Unit
) {
    Crossfade(targetState = isLoading, animationSpec = tween(300), label = "loading") { loading ->
        if (loading) loadingContent() else content()
    }
}
