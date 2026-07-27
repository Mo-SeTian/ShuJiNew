package com.readtrack.presentation.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.presentation.ui.components.EmptyStateView
import com.readtrack.presentation.ui.components.ReadingHeatmapCard
import com.readtrack.presentation.viewmodel.MonthlyStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("月度统计", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.months.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.GridView,
                title = "暂无阅读数据",
                description = "开始记录阅读后，这里会以热力图形式展示每日阅读量",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    ReadingHeatmapCard(
                        months = uiState.months,
                        isChapterBased = false,
                        useCombinedValue = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
