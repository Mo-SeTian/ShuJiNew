package com.readtrack.presentation.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.data.local.HomeComponent
import com.readtrack.data.local.StatsUnit
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.components.BookCard
import com.readtrack.presentation.ui.components.QuickRecordDialog
import com.readtrack.presentation.viewmodel.HomeUiState
import com.readtrack.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

private fun StatsUnit.label(): String = if (this == StatsUnit.CHAPTER) "章" else "页"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBookClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var quickRecordBookId by remember { mutableStateOf<Long?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "ReadTrack",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { isEditMode = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑首页组件")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // 根据 componentOrder 渲染组件
                val order = if (uiState.componentOrder.isEmpty()) {
                    HomeComponent.entries.map { it.id }
                } else {
                    uiState.componentOrder
                }

                order.forEach { componentId ->
                    when (componentId) {
                        HomeComponent.HERO.id -> item(key = "hero") { HeroSummaryCard(uiState) }
                        HomeComponent.OVERVIEW.id -> item(key = "overview") { OverviewCardsRow(uiState) }
                        HomeComponent.INSIGHT.id -> item(key = "insight") { ReadingInsightCard(uiState) }
                        HomeComponent.STATUS.id -> item(key = "status") { StatusOverviewCard(uiState) }
                        HomeComponent.RECENT.id -> {
                            if (uiState.recentBooks.isNotEmpty()) {
                                item(key = "recent-header") {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "最近阅读",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = uiState.latestReadingBookTitle?.let { "最近翻阅：$it" }
                                                ?: "继续你的阅读节奏",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(
                                    items = uiState.recentBooks,
                                    key = { "recent-${it.id}" }
                                ) { book ->
                                    BookCard(
                                        book = book,
                                        onClick = { onBookClick(book.id) },
                                        onQuickRecord = { id -> quickRecordBookId = id }
                                    )
                                }
                            } else if (uiState.totalBooks == 0) {
                                item(key = "recent-empty") { EmptyHomeState() }
                            }
                        }
                    }
                }
            }
        }
    }

    // 编辑底部弹窗
    if (isEditMode) {
        // 关键：直接用 uiState.componentOrder 作为 remember key，而非局部 val
        val editableList = remember(uiState.componentOrder) {
            mutableStateListOf<HomeComponentItem>().apply {
                val order = uiState.componentOrder.ifEmpty { HomeComponent.entries.map { it.id } }
                order.forEach { id ->
                    val component = HomeComponent.entries.find { it.id == id } ?: return@forEach
                    add(HomeComponentItem(component, true))
                }
                HomeComponent.entries.filter { c -> order.none { it == c.id } }.forEach { c ->
                    add(HomeComponentItem(c, false))
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { isEditMode = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "编辑首页组件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "选择要在首页显示的组件，并拖动排序",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                editableList.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "拖动",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.component.title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = componentDescription(item.component),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = item.enabled,
                            onCheckedChange = { enabled ->
                                editableList[index] = item.copy(enabled = enabled)
                            }
                        )
                    }
                    if (index < editableList.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                androidx.compose.material3.Button(
                    onClick = {
                        // 保存顺序：仅保留 enabled 的，按当前顺序
                        val newOrder = editableList.filter { it.enabled }.map { it.component.id }
                        viewModel.updateComponentOrder(newOrder)
                        scope.launch {
                            sheetState.hide()
                            isEditMode = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("保存")
                }
            }
        }
    }

    if (quickRecordBookId != null) {
        val book = uiState.recentBooks.find { it.id == quickRecordBookId }
        book?.let {
            QuickRecordDialog(
                book = it,
                onDismiss = { quickRecordBookId = null },
                onConfirm = { newPage, newChapter ->
                    viewModel.quickRecord(quickRecordBookId!!, newPage, newChapter)
                    quickRecordBookId = null
                }
            )
        }
    }
}

private data class HomeComponentItem(
    val component: HomeComponent,
    val enabled: Boolean
)

private fun componentDescription(component: HomeComponent): String = when (component) {
    HomeComponent.HERO -> "完读率总览和书架摘要"
    HomeComponent.OVERVIEW -> "今日阅读和连续阅读天数"
    HomeComponent.INSIGHT -> "月度阅读统计和阅读洞察"
    HomeComponent.STATUS -> "书架书籍状态分布"
    HomeComponent.RECENT -> "最近阅读的书籍列表"
}

@Composable
private fun HeroSummaryCard(uiState: HomeUiState) {
    val progress = remember(uiState.completionRate) { uiState.completionRate.coerceIn(0, 100) / 100f }
    val summaryText = remember(uiState.totalBooks, uiState.readingBooks, uiState.finishedBooks) {
        when {
            uiState.totalBooks == 0 -> "先添加一本书，首页会开始展示你的阅读趋势"
            uiState.readingBooks > 0 -> "当前有 ${uiState.readingBooks} 本书正在推进，已读完 ${uiState.finishedBooks} 本"
            else -> "书架已有 ${uiState.totalBooks} 本书，挑一本继续开始吧"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("阅读仪表盘", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(summaryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("完读率", style = MaterialTheme.typography.bodyMedium)
                    Text("${uiState.completionRate}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                )
            }
        }
    }
}

@Composable
private fun OverviewCardsRow(uiState: HomeUiState) {
    val unitLabel = remember(uiState.statsUnit) { uiState.statsUnit.label() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCardModern(
            title = "今日阅读",
            value = remember(uiState.todayValue, unitLabel) { "${uiState.todayValue.toInt()}$unitLabel" },
            subtitle = remember(uiState.weeklyValue, unitLabel) { "近 7 天 ${uiState.weeklyValue.toInt()}$unitLabel" },
            icon = Icons.AutoMirrored.Filled.MenuBook,
            gradientColors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.weight(1f),
            valueColor = MaterialTheme.colorScheme.primary
        )
        StatCardModern(
            title = "连续阅读",
            value = remember(uiState.streakDays) { uiState.streakDays.toString() },
            subtitle = remember(uiState.activeDaysThisWeek) { "本周活跃 ${uiState.activeDaysThisWeek} 天" },
            icon = Icons.Default.LocalFireDepartment,
            gradientColors = listOf(Color(0xFFFF7043), Color(0xFFFFAB91)),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReadingInsightCard(uiState: HomeUiState) {
    val unitLabel = remember(uiState.statsUnit) { uiState.statsUnit.label() }
    val monthText = remember(uiState.monthlyValue, unitLabel) { "近 30 天 ${uiState.monthlyValue.toInt()}$unitLabel" }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("阅读洞察", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(monthText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (uiState.statsUnit == StatsUnit.PAGE) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InsightMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "总阅读时长",
                        value = uiState.totalReadingTimeLabel,
                        icon = Icons.Default.Schedule,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    InsightMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "当前在读",
                        value = "${uiState.readingBooks} 本",
                        icon = Icons.Default.CheckCircle,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                InsightMetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    label = "当前在读",
                    value = "${uiState.readingBooks} 本",
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun InsightMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusOverviewCard(uiState: HomeUiState) {
    val statusItems = remember(uiState.statusCounts) {
        listOf(
            StatusSummaryItem(BookStatus.WANT_TO_READ, statusLabel(BookStatus.WANT_TO_READ), statusColor(BookStatus.WANT_TO_READ), uiState.statusCounts[BookStatus.WANT_TO_READ] ?: 0),
            StatusSummaryItem(BookStatus.READING, statusLabel(BookStatus.READING), statusColor(BookStatus.READING), uiState.statusCounts[BookStatus.READING] ?: 0),
            StatusSummaryItem(BookStatus.FINISHED, statusLabel(BookStatus.FINISHED), statusColor(BookStatus.FINISHED), uiState.statusCounts[BookStatus.FINISHED] ?: 0),
            StatusSummaryItem(BookStatus.ON_HOLD, statusLabel(BookStatus.ON_HOLD), statusColor(BookStatus.ON_HOLD), uiState.statusCounts[BookStatus.ON_HOLD] ?: 0),
            StatusSummaryItem(BookStatus.ABANDONED, statusLabel(BookStatus.ABANDONED), statusColor(BookStatus.ABANDONED), uiState.statusCounts[BookStatus.ABANDONED] ?: 0)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("阅读概览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatusItem(count = uiState.totalBooks, label = "总书籍", color = MaterialTheme.colorScheme.primary)
                StatusItem(count = uiState.readingBooks, label = "在读", color = statusColor(BookStatus.READING))
                StatusItem(count = uiState.finishedBooks, label = "已读", color = statusColor(BookStatus.FINISHED))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                statusItems.forEach { item ->
                    StatusBarRow(item)
                }
            }
        }
    }
}

@Composable
private fun StatusBarRow(item: StatusSummaryItem) {
    val progress = remember(item.count) { (item.count.coerceAtMost(10)) / 10f }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.label, style = MaterialTheme.typography.bodySmall)
            Text("${item.count}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = item.color,
            trackColor = item.color.copy(alpha = 0.18f)
        )
    }
}

@Composable
private fun EmptyHomeState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有添加书籍",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击底部「我的书籍」开始添加",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatCardModern(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    val gradientBrush = remember(gradientColors) {
        Brush.horizontalGradient(colors = gradientColors.map { it.copy(alpha = 0.15f) })
    }
    val resolvedValueColor = remember(valueColor, gradientColors) { valueColor ?: gradientColors[0] }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradientBrush)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(gradientColors[0].copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = gradientColors[0],
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = resolvedValueColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class StatusSummaryItem(
    val status: BookStatus,
    val label: String,
    val color: Color,
    val count: Int
)

private fun statusColor(status: BookStatus): Color = when (status) {
    BookStatus.WANT_TO_READ -> Color(0xFF4CAF50)
    BookStatus.READING -> Color(0xFFFF9800)
    BookStatus.FINISHED -> Color(0xFF2196F3)
    BookStatus.ON_HOLD -> Color(0xFF9E9E9E)
    BookStatus.ABANDONED -> Color(0xFFF44336)
}

private fun statusLabel(status: BookStatus): String = when (status) {
    BookStatus.WANT_TO_READ -> "想读"
    BookStatus.READING -> "在读"
    BookStatus.FINISHED -> "已读"
    BookStatus.ON_HOLD -> "暂停"
    BookStatus.ABANDONED -> "放弃"
}
