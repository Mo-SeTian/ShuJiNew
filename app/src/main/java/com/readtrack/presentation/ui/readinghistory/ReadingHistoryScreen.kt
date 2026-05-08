package com.readtrack.presentation.ui.readinghistory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.readtrack.domain.model.ProgressType
import com.readtrack.presentation.viewmodel.ReadingHistoryDayGroup
import com.readtrack.presentation.viewmodel.ReadingHistoryItem
import com.readtrack.presentation.viewmodel.ReadingHistoryTimeRange
import com.readtrack.presentation.viewmodel.ReadingHistoryUiState
import com.readtrack.presentation.viewmodel.ReadingHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.readtrack.presentation.ui.theme.AbandonedRed
import com.readtrack.presentation.ui.theme.FinishedBlue
import com.readtrack.presentation.ui.theme.OnHoldGray
import com.readtrack.presentation.ui.theme.ReadingOrange
import com.readtrack.presentation.ui.theme.WantToReadGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHistoryScreen(
    onNavigateBack: () -> Unit,
    onBookClick: (Long) -> Unit,
    viewModel: ReadingHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "阅读历史",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                TimeRangeFilterBar(
                    selectedRange = uiState.selectedRange,
                    onRangeSelected = { range ->
                        if (range is ReadingHistoryTimeRange.Custom) {
                            showCustomPicker = true
                        } else {
                            viewModel.selectRange(range)
                        }
                    }
                )
            }
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
                EmptyHistoryContent(modifier = Modifier.padding(padding))
            }
            else -> {
                ReadingHistoryContent(
                    groups = uiState.groups,
                    onBookClick = onBookClick,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    if (showCustomPicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showCustomPicker = false },
            onConfirm = { startMs, endMs ->
                viewModel.selectRange(ReadingHistoryTimeRange.Custom(startMs, endMs))
                showCustomPicker = false
            }
        )
    }
}

@Composable
private fun TimeRangeFilterBar(
    selectedRange: ReadingHistoryTimeRange,
    onRangeSelected: (ReadingHistoryTimeRange) -> Unit
) {
    val fixedRanges = listOf(
        ReadingHistoryTimeRange.Week,
        ReadingHistoryTimeRange.Month,
        ReadingHistoryTimeRange.ThreeMonths,
        ReadingHistoryTimeRange.HalfYear,
        ReadingHistoryTimeRange.All
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fixedRanges.forEach { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        FilterChip(
            selected = selectedRange is ReadingHistoryTimeRange.Custom,
            onClick = { onRangeSelected(ReadingHistoryTimeRange.Custom(0, 0)) },
            label = { Text("自定义") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long) -> Unit
) {
    var selectingStart by remember { mutableStateOf(true) }
    var startDateMs by remember { mutableStateOf<Long?>(null) }
    var endDateMs by remember { mutableStateOf<Long?>(null) }

    val datePickerState = rememberDatePickerState()
    val dateFormatter = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE) }

    val titleText = if (selectingStart) "选择开始日期" else "选择结束日期"
    val confirmText = if (selectingStart) "下一步" else "确定"
    val dismissText = if (selectingStart) "取消" else "上一步"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = {
            Column {
                if (startDateMs != null || endDateMs != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        startDateMs?.let {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "开始: ${dateFormatter.format(Date(it))}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        endDateMs?.let {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "结束: ${dateFormatter.format(Date(it))}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectingStart) {
                        datePickerState.selectedDateMillis?.let { startDateMs = it }
                        selectingStart = false
                    } else {
                        datePickerState.selectedDateMillis?.let { endDateMs = it }
                        if (startDateMs != null && endDateMs != null) {
                            val start = minOf(startDateMs!!, endDateMs!!)
                            val end = maxOf(startDateMs!!, endDateMs!!)
                            onConfirm(start, end)
                        } else if (startDateMs != null) {
                            onConfirm(startDateMs!!, System.currentTimeMillis())
                        }
                    }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            Row {
                if (selectingStart) {
                    TextButton(onClick = onDismiss) { Text(dismissText) }
                } else {
                    TextButton(onClick = { selectingStart = true }) { Text(dismissText) }
                }
            }
        }
    )
}

@Composable
private fun EmptyHistoryContent(modifier: Modifier = Modifier) {
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
                "开始阅读一本书来创建你的阅读历史",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReadingHistoryContent(
    groups: List<ReadingHistoryDayGroup>,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groups.forEachIndexed { groupIndex, group ->
            item(key = "header_${group.dateKey}") {
                HistoryDateHeader(dateLabel = group.dateLabel)
            }

            items(
                items = group.items,
                key = { it.record.id }
            ) { item ->
                HistoryRecordItem(
                    item = item,
                    onBookClick = onBookClick
                )
            }

            if (groupIndex < groups.lastIndex) {
                item(key = "spacer_${group.dateKey}") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryDateHeader(dateLabel: String) {
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
private fun HistoryRecordItem(
    item: ReadingHistoryItem,
    onBookClick: (Long) -> Unit
) {
    val record = item.record
    val snapshot = item.bookSnapshot
    val isBookMissing = snapshot == null
    val isChapterBased = snapshot?.progressType == ProgressType.CHAPTER
    val isStatusRecord = record.recordType != RecordType.NORMAL

    val statusLabel: String = when (record.recordType) {
        RecordType.STATUS_ADDED -> snapshot?.status?.displayName ?: "添加"
        RecordType.STATUS_READING -> "在读"
        RecordType.STATUS_FINISHED -> "已读"
        RecordType.STATUS_DROPPED -> "放弃"
        else -> ""
    }
    val statusColor: Color = when (record.recordType) {
        RecordType.STATUS_ADDED -> when (snapshot?.status) {
            BookStatus.WANT_TO_READ -> WantToReadGreen
            BookStatus.READING -> ReadingOrange
            BookStatus.FINISHED -> FinishedBlue
            BookStatus.ON_HOLD -> OnHoldGray
            BookStatus.ABANDONED -> AbandonedRed
            else -> OnHoldGray
        }
        RecordType.STATUS_READING -> ReadingOrange
        RecordType.STATUS_FINISHED -> FinishedBlue
        RecordType.STATUS_DROPPED -> AbandonedRed
        else -> FinishedBlue
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

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isStatusRecord) statusColor else MaterialTheme.colorScheme.primary)
                    .align(Alignment.Top)
            )

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
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
                    val pagesText = remember(record.pagesRead, record.chaptersRead, isChapterBased) {
                        if (isChapterBased) {
                            val chapters = record.chaptersRead ?: 0
                            if (chapters > 0) "+${chapters} 章" else "在读"
                        } else "${record.pagesRead.toInt()} 页"
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