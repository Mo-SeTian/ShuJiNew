package com.readtrack.presentation.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.util.toDateString

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: BookEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 是否显示选中态（用于批量选择模式） */
    selected: Boolean = false,
    /** 长按回调（用于进入选择模式），传入 null 则不响应长按 */
    onLongClick: (() -> Unit)? = null,
    /** 快速记录阅读进度回调，参数为 bookId */
    onQuickRecord: ((Long) -> Unit)? = null,
    /** 标记读完回调，参数为 bookId */
    onQuickFinish: ((Long) -> Unit)? = null
) {
    val statusColor = statusColorOf(book.status)
    val progressModel = remember(book) { book.toBookProgressUiModel() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick ?: {}
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Selection indicator (batch mode)
            if (selected) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Book Cover
            BookCover(
                coverPath = book.coverPath,
                contentDescription = book.title,
                modifier = Modifier
                    .width(70.dp)
                    .height(105.dp),
                requestSize = DpSize(70.dp, 105.dp),
                quality = BookCoverQuality.THUMBNAIL
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Book Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Author
                if (!book.author.isNullOrBlank()) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status + Rating inline row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = book.status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Rating stars (if rated)
                    if (book.rating != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (index < book.rating!!)
                                        Color(0xFFFFB400)
                                    else
                                        Color(0xFFFFB400).copy(alpha = 0.3f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = String.format("%.1f", book.rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB400),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "进度",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${progressModel.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progressModel.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = progressModel.progressLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (book.lastReadAt != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = book.lastReadAt!!.toDateString("MM/dd"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Right side quick action buttons
            if (onQuickRecord != null || onQuickFinish != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.height(105.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (onQuickRecord != null && book.status == com.readtrack.domain.model.BookStatus.READING) {
                        QuickActionButton(
                            icon = Icons.Default.Add,
                            label = "记录",
                            containerColor = statusColor.copy(alpha = 0.1f),
                            contentColor = statusColor,
                            onClick = { onQuickRecord(book.id) }
                        )
                    }
                    if (onQuickFinish != null &&
                        (book.status == com.readtrack.domain.model.BookStatus.READING || book.status == com.readtrack.domain.model.BookStatus.WANT_TO_READ)) {
                        QuickActionButton(
                            icon = Icons.Default.Done,
                            label = "读完",
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            contentColor = Color(0xFF4CAF50),
                            onClick = { onQuickFinish(book.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = Modifier.size(width = 52.dp, height = 40.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
