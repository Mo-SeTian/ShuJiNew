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
 * 输入拦截：章节数不能小于当前进度，不能大于总章节；页数同理
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

    // 校验输入合法性
    val isChapterValid = !isChapterBased || run {
        val ch = chapterInput.toIntOrNull()
        val maxCh = book.totalChapters ?: 0
        ch != null && ch >= book.currentChapter && (maxCh <= 0 || ch <= maxCh)
    }
    val isPageValid = isChapterBased || run {
        val pg = pageInput.toDoubleOrNull()
        val maxPg = book.totalPages
        pg != null && pg >= book.currentPage && (maxPg <= 0 || pg <= maxPg)
    }
    val isInputValid = if (isChapterBased) isChapterValid else isPageValid

    val inputError = when {
        isChapterBased -> {
            val ch = chapterInput.toIntOrNull()
            val maxChapter = book.totalChapters ?: 0
            when {
                chapterInput.isBlank() -> null
                ch == null -> "请输入有效数字"
                ch < book.currentChapter -> "不能小于当前进度（第 ${book.currentChapter} 章）"
                maxChapter > 0 && ch > maxChapter -> "不能大于总章节数（$maxChapter）"
                else -> null
            }
        }
        else -> {
            val pg = pageInput.toDoubleOrNull()
            val maxPage = book.totalPages
            when {
                pageInput.isBlank() -> null
                pg == null -> "请输入有效数字"
                pg < book.currentPage -> "不能小于当前进度（第 ${book.currentPage.toInt()} 页）"
                maxPage > 0 && pg > maxPage -> "不能大于总页数（${maxPage.toInt()}）"
                else -> null
            }
        }
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
                        isError = inputError != null,
                        supportingText = inputError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
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
                        isError = inputError != null,
                        supportingText = inputError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
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
                        enabled = isInputValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isInputValid) "确认" else "输入无效")
                    }
                }
            }
        }
    }
}
