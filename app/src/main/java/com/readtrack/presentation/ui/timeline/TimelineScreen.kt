package com.readtrack.presentation.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.components.BookCover
import com.readtrack.presentation.ui.components.BookCoverQuality
import com.readtrack.presentation.viewmodel.ProgressType
import com.readtrack.presentation.viewmodel.TimelineDayGroup
import com.readtrack.presentation.viewmodel.TimelineItem
import com.readtrack.presentation.viewmodel.TimelineUiState
import com.readtrack.presentation.viewmodel.TimelineViewModel
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onBookClick: (Long) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "阅读历史",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "加载失败: ${uiState.errorMessage}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            uiState.groups.isEmpty() -> {
                EmptyTimelineContent(modifier = Modifier.padding(padding))
            }
            else -> {
                TimelineContent(
                    groups = uiState.groups,
                    onBookClick = onBookClick,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun EmptyTimelineContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("📖", style = MaterialTheme.typography.displaySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "暂无阅读记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "开始阅读一本书来创建你的时间线",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimelineContent(
    groups: List<TimelineDayGroup>,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groups.forEachIndexed { groupIndex, group ->
            // 日期标题
            item(key = "header_${group.dateKey}") {
                TimelineDateHeader(dateLabel = group.dateLabel)
            }

            // 该日期下的记录
            items(
                items = group.items,
                key = { it.record.id }
            ) { item ->
                TimelineRecordItem(
                    item = item,
                    onBookClick = onBookClick
                )
            }

            // 日期组之间的间距（如果是最后一天则不加底部间距）
            if (groupIndex < groups.lastIndex) {
                item(key = "spacer_${group.dateKey}") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TimelineDateHeader(dateLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun TimelineRecordItem(
    item: TimelineItem,
    onBookClick: (Long) -> Unit
) {
    val record = item.record
    val snapshot = item.bookSnapshot
    val isBookMissing = snapshot == null
    val isChapterBased = snapshot?.progressType == ProgressType.CHAPTER
    val isStatusRecord = record.recordType != RecordType.NORMAL

    // 状态记录的颜色和标签（从快照获取状态）
    val statusLabel: String = when (record.recordType) {
        RecordType.STATUS_ADDED -> snapshot?.status?.displayName ?: "添加"
        RecordType.STATUS_READING -> "在读"
        RecordType.STATUS_FINISHED -> "已读"
        RecordType.STATUS_DROPPED -> "放弃"
        else -> ""
    }
    val statusColor: Color = when (record.recordType) {
        RecordType.STATUS_ADDED -> when (snapshot?.status) {
            BookStatus.WANT_TO_READ -> Color(0xFF4CAF50)
            BookStatus.READING -> Color(0xFFFF9800)
            BookStatus.FINISHED -> Color(0xFF2196F3)
            BookStatus.ON_HOLD -> Color(0xFF9E9E9E)
            BookStatus.ABANDONED -> Color(0xFFF44336)
            else -> Color(0xFF9E9E9E)
        }
        RecordType.STATUS_READING -> Color(0xFFFF9800)
        RecordType.STATUS_FINISHED -> Color(0xFF2196F3)
        RecordType.STATUS_DROPPED -> Color(0xFFF44336)
        else -> Color(0xFF2196F3)
    }
    val statusIcon = when (record.recordType) {
        RecordType.STATUS_ADDED -> Icons.Default.Add
        RecordType.STATUS_READING -> Icons.Default.PlayArrow
        RecordType.STATUS_FINISHED -> Icons.Default.CheckCircle
        RecordType.STATUS_DROPPED -> Icons.Default.Delete
        else -> Icons.Default.Add
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 0.dp, end = 0.dp)
            .then(
                if (!isBookMissing && record.bookId != null) {
                    Modifier.clickable { onBookClick(record.bookId) }
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 时间
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(48.dp)
            ) {
                Text(
                    text = item.timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 时间线圆点（状态记录用状态色）
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isStatusRecord) statusColor else MaterialTheme.colorScheme.primary)
                    .align(Alignment.Top)
            )

            // 书籍封面缩略（快照中获取）
            BookCover(
                coverPath = snapshot?.coverPath,
                contentDescription = snapshot?.title ?: "[已删除图书]",
                modifier = Modifier
                    .width(44.dp)
                    .height(66.dp)
                    .clip(RoundedCornerShape(6.dp)),
                requestSize = androidx.compose.ui.unit.DpSize(88.dp, 132.dp),
                quality = BookCoverQuality.THUMBNAIL
            )

            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = snapshot?.title ?: "[已删除图书]",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isBookMissing) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )

                if (!isBookMissing && !snapshot?.author.isNullOrBlank()) {
                    Text(
                        text = snapshot?.author ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isStatusRecord) {
                    // 状态记录：显示彩色标签
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // 备注
                    if (!record.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = record.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // 普通阅读记录：显示阅读量
                    val pagesText = remember(record.pagesRead, isChapterBased) {
                        if (isChapterBased) "${record.pagesRead.toInt()} 章" else "${record.pagesRead.toInt()} 页"
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = pagesText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // 备注
                    if (!record.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = record.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
