package com.readtrack.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.presentation.ui.theme.AbandonedRed
import com.readtrack.presentation.ui.theme.FinishedBlue
import com.readtrack.presentation.ui.theme.OnHoldGray
import com.readtrack.presentation.ui.theme.ReadingOrange
import com.readtrack.presentation.ui.theme.WantToReadGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReadingRecordRow(
    record: ReadingRecordEntity,
    isChapterBased: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val isStatusRecord = record.recordType != RecordType.NORMAL

    val (statusColor, statusIcon) = when (record.recordType) {
        RecordType.STATUS_ADDED -> WantToReadGreen to Icons.Default.Add
        RecordType.STATUS_READING -> ReadingOrange to Icons.Default.PlayArrow
        RecordType.STATUS_FINISHED -> FinishedBlue to Icons.Default.CheckCircle
        RecordType.STATUS_DROPPED -> when (record.bookSnapshot?.status) {
            com.readtrack.domain.model.BookStatus.ON_HOLD -> OnHoldGray to Icons.Default.Delete
            else -> AbandonedRed to Icons.Default.Delete
        }
        else -> MaterialTheme.colorScheme.primary to Icons.Default.Add
    }
    val statusLabel: String = when (record.recordType) {
        RecordType.STATUS_ADDED -> "添加"
        RecordType.STATUS_READING -> "在读"
        RecordType.STATUS_FINISHED -> "已读"
        RecordType.STATUS_DROPPED -> when (record.bookSnapshot?.status) {
            com.readtrack.domain.model.BookStatus.ON_HOLD -> "闲置"
            else -> "放弃"
        }
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (isStatusRecord) {
                    val noteText = record.note?.takeIf { it.isNotBlank() } ?: ""
                    if (noteText.isNotBlank()) {
                        Text(
                            noteText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        record.note?.takeIf { it.isNotBlank() } ?: "阅读了 ${if (isChapterBased) record.chaptersRead ?: 0 else record.pagesRead.toInt()} ${if (isChapterBased) "章" else "页"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isStatusRecord) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "+${if (isChapterBased) record.chaptersRead ?: 0 else record.pagesRead.toInt()} ${if (isChapterBased) "章" else "页"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
