package com.readtrack.presentation.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.readtrack.presentation.ui.theme.AbandonedRed
import com.readtrack.presentation.ui.theme.FinishedBlue
import com.readtrack.presentation.ui.theme.OnHoldGray
import com.readtrack.presentation.ui.theme.ReadingOrange
import com.readtrack.presentation.ui.theme.WantToReadGreen
import kotlinx.coroutines.launch

private fun StatsUnit.label(): String = if (this == StatsUnit.CHAPTER) "章" else "页"

private enum class RecentSortOrder {
    BY_TIME,
    BY_TITLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBookClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var quickRecordBookId by remember { mutableStateOf<Long?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var recentSortOrder by remember { mutableStateOf(RecentSortOrder.BY_TIME) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedRecentBooks = remember(uiState.recentBooks, recentSortOrder) {
        when (recentSortOrder) {
            RecentSortOrder.BY_TIME -> uiState.recentBooks.sortedByDescending { it.lastReadAt ?: 0L }
            RecentSortOrder.BY_TITLE -> uiState.recentBooks.sortedBy { it.title.lowercase() }
        }
    }

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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "最近阅读",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box {
                                                IconButton(onClick = { showSortMenu = true }) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.Sort,
                                                        contentDescription = "排序",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showSortMenu,
                                                    onDismissRequest = { showSortMenu = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("按修改时间" + if (recentSortOrder == RecentSortOrder.BY_TIME) " ✓" else "") },
                                                        onClick = {
                                                            recentSortOrder = RecentSortOrder.BY_TIME
                                                            showSortMenu = false
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("按书名" + if (recentSortOrder == RecentSortOrder.BY_TITLE) " ✓" else "") },
                                                        onClick = {
                                                            recentSortOrder = RecentSortOrder.BY_TITLE
                                                            showSortMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = uiState.latestReadingBookTitle?.let { "最近翻阅：$it" }
                                                ?: "继续你的阅读节奏",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(
                                    items = sortedRecentBooks,
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

    if (isEditMode) {
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
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            EditHomeComponentsContent(
                editableList = editableList,
                onReorder = { from, to ->
                    editableList.apply { add(to, removeAt(from)) }
                },
                onToggle = { index, enabled ->
                    editableList[index] = editableList[index].copy(enabled = enabled)
                },
                onSave = {
                    val newOrder = editableList.filter { it.enabled }.map { it.component.id }
                    viewModel.updateComponentOrder(newOrder)
                    scope.launch {
                        sheetState.hide()
                        isEditMode = false
                    }
                }
            )
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
private fun EditHomeComponentsContent(
    editableList: List<HomeComponentItem>,
    onReorder: (Int, Int) -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "编辑首页组件",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "点击开关启用/禁用组件，使用箭头调整顺序",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (editableList.any { it.enabled }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "已启用 ${editableList.count { it.enabled }} 个组件",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            editableList.forEachIndexed { index, item ->
                HomeComponentRow(
                    item = item,
                    index = index,
                    totalItems = editableList.size,
                    onToggle = { enabled -> onToggle(index, enabled) },
                    onMoveUp = { if (index > 0) onReorder(index, index - 1) },
                    onMoveDown = { if (index < editableList.lastIndex) onReorder(index, index + 1) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("保存更改", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HomeComponentRow(
    item: HomeComponentItem,
    index: Int,
    totalItems: Int,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.enabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "拖动排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.component.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (item.enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = componentDescription(item.component),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (item.enabled) 1f else 0.6f
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0 && item.enabled,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "上移",
                        tint = if (index > 0 && item.enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalItems - 1 && item.enabled,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "下移",
                        tint = if (index < totalItems - 1 && item.enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            Switch(
                checked = item.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
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
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface
    val gradientBrush = remember(primaryContainer, surface) {
        Brush.verticalGradient(colors = listOf(primaryContainer, surface))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(gradientBrush)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

                // 完读率大字 + 进度条
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${uiState.completionRate}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                    Text(
                        text = "完读率",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusOverviewCard(uiState: HomeUiState) {
    val statusOrder = remember {
        listOf(BookStatus.WANT_TO_READ, BookStatus.READING, BookStatus.FINISHED, BookStatus.ON_HOLD, BookStatus.ABANDONED)
    }
    val total = remember(uiState.totalBooks) { uiState.totalBooks.coerceAtLeast(1) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("阅读概览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "共 ${uiState.totalBooks} 本",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 5 状态分布条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                statusOrder.forEach { status ->
                    val count = uiState.statusCounts[status] ?: 0
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .weight(count.toFloat())
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(statusColor(status))
                        )
                    }
                }
            }

            // 5 状态计数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                statusOrder.forEach { status ->
                    val count = uiState.statusCounts[status] ?: 0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor(status))
                            )
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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
        Brush.verticalGradient(
            colors = listOf(
                gradientColors.last().copy(alpha = 0.18f),
                gradientColors.last().copy(alpha = 0.04f)
            )
        )
    }
    val resolvedValueColor = remember(valueColor, gradientColors) { valueColor ?: gradientColors[0] }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradientBrush)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(gradientColors[0].copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = gradientColors[0],
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = resolvedValueColor
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun statusColor(status: BookStatus): Color = when (status) {
    BookStatus.WANT_TO_READ -> WantToReadGreen
    BookStatus.READING -> ReadingOrange
    BookStatus.FINISHED -> FinishedBlue
    BookStatus.ON_HOLD -> OnHoldGray
    BookStatus.ABANDONED -> AbandonedRed
}