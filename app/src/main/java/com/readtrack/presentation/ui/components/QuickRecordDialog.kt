package com.readtrack.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.ProgressType

/**
 * 快速记录阅读进度弹窗
 * 简洁设计：只显示当前进度和数字输入框，一键确认
 */
@Composable
fun QuickRecordDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onConfirm: (newPage: Double, newChapter: Int) -> Unit
) {
    val isChapterBased = book.progressType == ProgressType.CHAPTER

    var pageInput by remember {
        mutableStateOf(
            if (isChapterBased) ""
            else book.currentPage.toInt().toString()
        )
    }
    var chapterInput by remember {
        mutableStateOf(
            if (isChapterBased) book.currentChapter.toString() else ""
        )
    }

    val totalLabel = if (isChapterBased) {
        "共 ${book.totalChapters ?: 0} 章"
    } else {
        "共 ${book.totalPages.toInt()} 页"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "快速记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Current progress hint
                Text(
                    text = if (isChapterBased) {
                        "当前：第 ${book.currentChapter} 章"
                    } else {
                        "当前：第 ${book.currentPage.toInt()} 页"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isChapterBased) {
                    // Chapter input
                    OutlinedTextField(
                        value = chapterInput,
                        onValueChange = { chapterInput = it.filter { c -> c.isDigit() } },
                        label = { Text("读到第几章") },
                        placeholder = { Text("例如：${book.currentChapter + 1}") },
                        suffix = { Text("/ ${book.totalChapters ?: 0} 章") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    // Page input
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { c -> c.isDigit() } },
                        label = { Text("读到第几页") },
                        placeholder = { Text("例如：${book.currentPage.toInt() + 10}") },
                        suffix = { Text("/ ${book.totalPages.toInt()} 页") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            val newPage = if (isChapterBased) 0.0 else pageInput.toDoubleOrNull() ?: book.currentPage
                            val newChapter = if (isChapterBased) chapterInput.toIntOrNull() ?: book.currentChapter else 0
                            onConfirm(newPage, newChapter)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确认")
                    }
                }
            }
        }
    }
}
