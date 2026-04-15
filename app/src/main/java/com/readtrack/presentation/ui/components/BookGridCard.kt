package com.readtrack.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.readtrack.data.local.entity.BookEntity

@Composable
fun BookGridCard(
    book: BookEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = getStatusColor(book.status)
    val progressModel = remember(book) { book.toBookProgressUiModel() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Book Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
            ) {
                BookCover(
                    coverPath = book.coverPath,
                    contentDescription = book.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    requestSize = DpSize(160.dp, 228.dp),
                    quality = BookCoverQuality.THUMBNAIL
                )
                
                // Status chip overlay
                BookStatusChip(
                    status = book.status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
            
            // Book Info
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                
                if (!book.author.isNullOrBlank()) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = { progressModel.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                
                Text(
                    text = progressModel.progressLabel.replace(" ", ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyBookPlaceholder(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📚",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "还没有书籍",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onAddClick) {
                Text("添加第一本书")
            }
        }
    }
}
