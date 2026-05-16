package com.readtrack.presentation.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.data.local.StatsRange
import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.components.statusColorOf
import com.readtrack.presentation.ui.components.statusLabelOf
import com.readtrack.presentation.ui.components.ShimmerStatCard
import com.readtrack.presentation.ui.theme.*
import com.readtrack.presentation.ui.share.DailyAchievementCard
import com.readtrack.presentation.ui.share.SharePreviewDialog
import com.readtrack.presentation.ui.share.WeeklyAchievementCard
import com.readtrack.presentation.ui.share.shareComposable
import androidx.compose.ui.graphics.Brush
import com.readtrack.presentation.viewmodel.DailyReading
import com.readtrack.domain.model.ProgressType
import com.readtrack.presentation.viewmodel.StatsViewModel
import com.readtrack.presentation.viewmodel.RecordWithBook
import java.text.SimpleDateFormat
import java.util.*
import com.readtrack.presentation.ui.theme.AbandonedRed
import com.readtrack.presentation.ui.theme.FinishedBlue
import com.readtrack.presentation.ui.theme.OnHoldGray
import com.readtrack.presentation.ui.theme.ReadingOrange
import com.readtrack.presentation.ui.theme.WantToReadGreen
import java.util.Calendar
import android.content.Context
import androidx.compose.ui.platform.LocalContext

private fun StatsUnit.label(): String = if (this == StatsUnit.CHAPTER) "章" else "页"
private fun StatsUnit.subLabel(): String = if (this == StatsUnit.CHAPTER) "页" else "章"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onReadingHistoryClick: () -> Unit,
    onYearlyReportClick: (Int) -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableStateOf<DailyReading?>(null) }

    // Day detail dialog
    selectedDay?.let { day ->
        val value = if (uiState.statsUnit == StatsUnit.CHAPTER) day.chapters else day.pages
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text(day.dayOfWeek, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("当天总阅读量: ${value.toInt()} ${uiState.statsUnit.label()}")
                    if (day.chapters > 0 && day.pages > 0) {
                        Text(
                            "包含 ${day.chapters.toInt()} 章 / ${day.pages.toInt()} 页",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (day.bookBreakdowns.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "阅读明细",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        day.bookBreakdowns.forEach { book ->
                            val bookValue = if (uiState.statsUnit == StatsUnit.CHAPTER) book.chapters else book.pages
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = book.bookTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${bookValue.toInt()} ${uiState.statsUnit.label()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (value <= 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "暂无阅读记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDay = null }) {
                    Text("关闭")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Text(
                        "阅读统计",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShimmerStatCard(modifier = Modifier.weight(1f))
                        ShimmerStatCard(modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShimmerStatCard(modifier = Modifier.weight(1f))
                        ShimmerStatCard(modifier = Modifier.weight(1f))
                    }
                }
                item { ShimmerStatCard(modifier = Modifier.fillMaxWidth()) }
                item { ShimmerStatCard(modifier = Modifier.fillMaxWidth()) }
                items(3) { ShimmerStatCard(modifier = Modifier.fillMaxWidth()) }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Reading Summary Cards - Modern Design
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsCardModern(
                            title = "今日阅读",
                            value = "${uiState.todayValue.toInt()}",
                            subtitle = uiState.statsUnit.label(),
                            icon = Icons.Default.MenuBook,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatsCardModern(
                            title = "本周阅读",
                            value = "${uiState.weekValue.toInt()}",
                            subtitle = uiState.statsUnit.label(),
                            icon = Icons.Default.MenuBook,
                            color = FinishedBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsCardModern(
                            title = "本月阅读",
                            value = "${uiState.monthValue.toInt()}",
                            subtitle = uiState.statsUnit.label(),
                            icon = Icons.Default.MenuBook,
                            color = ReadingOrange,
                            modifier = Modifier.weight(1f)
                        )
                        StatsCardModern(
                            title = "累计阅读",
                            value = "${uiState.totalValue.toInt()}",
                            subtitle = uiState.statsUnit.label(),
                            icon = Icons.Default.MenuBook,
                            color = WantToReadGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 年度报告入口横幅
                item {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    Card(
                        onClick = { onYearlyReportClick(currentYear) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF6366F1).copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF6366F1)
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "$currentYear 年度阅读报告",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "查看你的年度阅读总结",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 分享成就
                item {
                    val recentTitles = remember(uiState.recentRecordsWithBooks) {
                        uiState.recentRecordsWithBooks
                            .mapNotNull { it.bookSnapshot?.title ?: it.record.bookSnapshot?.title }
                            .distinct()
                            .take(3)
                    }
                    ShareSection(
                        weekValue = uiState.weekValue,
                        monthValue = uiState.monthValue,
                        statsUnit = uiState.statsUnit,
                        activeDays = uiState.recentRecordsWithBooks
                            .map { it.record.date }
                            .distinct()
                            .count(),
                        streakDays = uiState.recentRecordsWithBooks
                            .map { it.record.date }
                            .distinct()
                            .count(),
                        recentBookTitles = recentTitles
                    )
                }

                // Weekly Chart
                item {
                    WeeklyChartModern(
                        weeklyData = uiState.weeklyReadingData,
                        statsUnit = uiState.statsUnit,
                        onDayClick = { day -> selectedDay = day }
                    )
                }

                // Books by Status
                item {
                    StatusDistributionCard(
                        totalBooks = uiState.totalBooks,
                        booksByStatus = uiState.booksByStatus
                    )
                }

                // 查看全部按钮
                item {
                    OutlinedButton(
                        onClick = onReadingHistoryClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("查看全部阅读记录")
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCardModern(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyChartModern(
    weeklyData: List<DailyReading>,
    statsUnit: StatsUnit = StatsUnit.CHAPTER,
    onDayClick: ((DailyReading) -> Unit)? = null
) {
    val valueSelector: (DailyReading) -> Double = if (statsUnit == StatsUnit.CHAPTER) {
        { it.chapters }
    } else {
        { it.pages }
    }
    val maxValue = remember(weeklyData, statsUnit) {
        weeklyData.maxOfOrNull(valueSelector)?.coerceAtLeast(1.0) ?: 1.0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "近7天阅读趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (onDayClick != null) {
                    Text(
                        text = "点击查看详情",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEachIndexed { index, day ->
                    val dayValue = valueSelector(day)
                    val targetHeight = if (maxValue > 0) {
                        (dayValue / maxValue * 100).coerceIn(4.0, 100.0)
                    } else 4.0

                    // 动画效果
                    var animatedHeight by remember { mutableFloatStateOf(0f) }
                    LaunchedEffect(targetHeight) {
                        animatedHeight = targetHeight.toFloat()
                    }
                    val animatedHeightDp by animateFloatAsState(
                        targetValue = animatedHeight,
                        animationSpec = tween(durationMillis = 400, delayMillis = index * 50),
                        label = "barHeight"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (dayValue > 0) {
                            Text(
                                text = "${dayValue.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(animatedHeightDp.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (dayValue > 0)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (onDayClick != null) {
                                        Modifier.clickable { onDayClick(day) }
                                    } else Modifier
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = day.dayOfWeek,
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
fun StatusDistributionCard(
    totalBooks: Int,
    booksByStatus: Map<BookStatus, Int>
) {
    val statuses = remember { BookStatus.entries.toList() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "书籍状态分布",

                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "共 $totalBooks 本",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Status distribution bars
            statuses.forEach { status ->
                val count = booksByStatus[status] ?: 0
                val percentage = if (totalBooks > 0) count.toFloat() / totalBooks else 0f
                val statusColor = remember(status) { statusColorOf(status) }
                val statusLabel = remember(status) { statusLabelOf(status) }

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = statusColor,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "$count 本",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyRecordsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "暂无阅读记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "开始阅读后会自动记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ReadingRecordItem(recordWithBook: RecordWithBook) {
    val record = recordWithBook.record
    val snapshot = recordWithBook.bookSnapshot
    val isChapterBased = snapshot?.progressType == ProgressType.CHAPTER
    val dateFormatter = remember { SimpleDateFormat("MM月dd日 E", Locale.getDefault()) }
    val dateText = remember(record.date) { dateFormatter.format(Date(record.date)) }
    val isStatusRecord = record.recordType != RecordType.NORMAL

    // 状态记录：根据 recordType 和快照中的状态显示彩色标签
    val bookStatusLabel: String? = snapshot?.status?.displayName
    val (statusColor, statusIcon) = when (record.recordType) {
        RecordType.STATUS_ADDED -> {
            val color: Color = when (snapshot?.status) {
                BookStatus.WANT_TO_READ -> WantToReadGreen
                BookStatus.READING -> ReadingOrange
                BookStatus.FINISHED -> FinishedBlue
                BookStatus.ON_HOLD -> OnHoldGray
                BookStatus.ABANDONED -> AbandonedRed
                else -> MaterialTheme.colorScheme.primary
            }
            color to Icons.Default.Add
        }
        RecordType.STATUS_READING -> ReadingOrange to Icons.Default.PlayArrow
        RecordType.STATUS_FINISHED -> FinishedBlue to Icons.Default.CheckCircle
        RecordType.STATUS_DROPPED -> AbandonedRed to Icons.Default.Delete
        else -> MaterialTheme.colorScheme.primary to Icons.Default.Add
    }
    val statusLabel: String = when (record.recordType) {
        RecordType.STATUS_ADDED -> bookStatusLabel ?: "添加"
        RecordType.STATUS_READING -> "在读"
        RecordType.STATUS_FINISHED -> "已读"
        RecordType.STATUS_DROPPED -> "放弃"
        else -> ""
    }

    val noteText = remember(record.note, record.pagesRead, isChapterBased, isStatusRecord) {
        if (isStatusRecord) {
            record.note?.takeIf { it.isNotBlank() } ?: ""
        } else {
            record.note?.takeIf { it.isNotBlank() } ?: "阅读了 ${if (isChapterBased) record.chaptersRead ?: 0 else record.pagesRead.toInt()} ${if (isChapterBased) "章" else "页"}"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Book title
                Text(
                    text = snapshot?.title ?: "[已删除图书]",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (snapshot == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Date
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (noteText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isStatusRecord) {
                // 状态记录：显示图标 + 彩色标签
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusLabel,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                // 普通阅读记录：显示进度
                val progressText = remember(record.pagesRead, record.chaptersRead, isChapterBased) {
                    val value = if (isChapterBased) record.chaptersRead ?: 0 else record.pagesRead.toInt()
                    "+${value} ${if (isChapterBased) "章" else "页"}"
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = progressText,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareSection(
    weekValue: Double,
    monthValue: Double,
    statsUnit: StatsUnit,
    activeDays: Int,
    streakDays: Int,
    recentBookTitles: List<String>
) {
    val context = LocalContext.current
    val unitLabel = if (statsUnit == StatsUnit.CHAPTER) "章" else "页"
    var showWeeklyPreview by remember { mutableStateOf(false) }
    var showMonthlyPreview by remember { mutableStateOf(false) }
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    val monthLabels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("分享成就", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showWeeklyPreview = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("本周概览", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { showMonthlyPreview = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("月度概览", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showWeeklyPreview) {
        SharePreviewDialog(
            filename = "weekly_achievement",
            onDismiss = { showWeeklyPreview = false },
            content = {
                WeeklyAchievementCard(
                    weekValue = weekValue,
                    unitLabel = unitLabel,
                    activeDays = activeDays,
                    streakDays = streakDays,
                    recentBooks = recentBookTitles
                )
            }
        )
    }

    if (showMonthlyPreview) {
        SharePreviewDialog(
            filename = "monthly_$selectedMonth",
            onDismiss = { showMonthlyPreview = false },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("选择月份", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        monthLabels.forEachIndexed { i, label ->
                            FilterChip(
                                selected = selectedMonth == i + 1,
                                onClick = { selectedMonth = i + 1 },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    MonthlyAchievementCard(
                        month = selectedMonth,
                        monthValue = monthValue,
                        unitLabel = unitLabel
                    )
                }
            }
        )
    }
}

@Composable
private fun MonthlyAchievementCard(
    month: Int,
    monthValue: Double,
    unitLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFA78BFA))),
                RoundedCornerShape(16.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${month}月阅读概览", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${monthValue.toInt()}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text(unitLabel, fontSize = 24.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("—— 书迹 App ——", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
    }
}
