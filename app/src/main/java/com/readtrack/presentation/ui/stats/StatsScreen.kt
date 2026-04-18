package com.readtrack.presentation.ui.stats

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.components.statusColorOf
import com.readtrack.presentation.ui.components.statusLabelOf
import com.readtrack.presentation.ui.theme.*
import com.readtrack.presentation.viewmodel.DailyReading
import com.readtrack.presentation.viewmodel.ProgressType
import com.readtrack.presentation.viewmodel.StatsViewModel
import com.readtrack.presentation.viewmodel.RecordWithBook
import java.text.SimpleDateFormat
import java.util.*

private fun StatsUnit.label(): String = if (this == StatsUnit.CHAPTER) "章" else "页"
private fun StatsUnit.subLabel(): String = if (this == StatsUnit.CHAPTER) "页" else "章"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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

                // Weekly Chart
                item {
                    WeeklyChartModern(weeklyData = uiState.weeklyReadingData, statsUnit = uiState.statsUnit)
                }

                // Books by Status
                item {
                    StatusDistributionCard(
                        totalBooks = uiState.totalBooks,
                        booksByStatus = uiState.booksByStatus
                    )
                }

                // Recent Records
                item {
                    Text(
                        text = "最近阅读记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.recentRecordsWithBooks.isEmpty()) {
                    item {
                        EmptyRecordsCard()
                    }
                } else {
                    items(
                        items = uiState.recentRecordsWithBooks,
                        key = { it.record.id }
                    ) { recordWithBook ->
                        ReadingRecordItem(recordWithBook = recordWithBook)
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
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
fun WeeklyChartModern(weeklyData: List<DailyReading>, statsUnit: StatsUnit = StatsUnit.CHAPTER) {
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
            Text(
                text = "近7天阅读趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEachIndexed { _, day ->
                    val dayValue = valueSelector(day)
                    val height = if (maxValue > 0) {
                        (dayValue / maxValue * 100).coerceIn(4.0, 100.0)
                    } else 4.0

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
                                .height(height.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (dayValue > 0)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
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
                BookStatus.WANT_TO_READ -> Color(0xFF4CAF50)
                BookStatus.READING -> Color(0xFFFF9800)
                BookStatus.FINISHED -> Color(0xFF2196F3)
                BookStatus.ON_HOLD -> Color(0xFF9E9E9E)
                BookStatus.ABANDONED -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.primary
            }
            color to Icons.Default.Add
        }
        RecordType.STATUS_READING -> Color(0xFFFF9800) to Icons.Default.PlayArrow
        RecordType.STATUS_FINISHED -> Color(0xFF2196F3) to Icons.Default.CheckCircle
        RecordType.STATUS_DROPPED -> Color(0xFFF44336) to Icons.Default.Delete
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
            record.note?.takeIf { it.isNotBlank() } ?: "阅读了 ${record.pagesRead.toInt()} ${if (isChapterBased) "章" else "页"}"
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
