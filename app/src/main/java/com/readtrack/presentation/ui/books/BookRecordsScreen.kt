package com.readtrack.presentation.ui.books

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.ProgressType
import com.readtrack.presentation.ui.components.ReadingRecordRow
import com.readtrack.presentation.viewmodel.BookDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRecordsScreen(
    bookId: Long,
    onNavigateBack: () -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteRecordDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<ReadingRecordEntity?>(null) }
    var showEditRecordDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<ReadingRecordEntity?>(null) }

    // 删除确认对话框
    if (showDeleteRecordDialog && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteRecordDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条阅读记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    recordToDelete?.let { viewModel.deleteReadingRecord(it) }
                    showDeleteRecordDialog = false
                    recordToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRecordDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 编辑记录对话框
    if (showEditRecordDialog && recordToEdit != null) {
        val record = recordToEdit!!
        val isChapterBased = uiState.book?.progressType == ProgressType.CHAPTER
        var editValue by remember(record) {
            mutableStateOf(
                if (isChapterBased) (record.chaptersRead ?: 0).toString()
                else record.pagesRead.toInt().toString()
            )
        }
        var editNote by remember(record) { mutableStateOf(record.note ?: "") }

        AlertDialog(
            onDismissRequest = { showEditRecordDialog = false },
            title = { Text("编辑阅读记录") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text(if (isChapterBased) "章数" else "页数") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("备注（可选）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updatedRecord = record.copy(
                        note = editNote.ifBlank { null }
                    )
                    if (isChapterBased) {
                        val chapters = editValue.toIntOrNull() ?: 0
                        viewModel.updateReadingRecord(
                            updatedRecord.copy(chaptersRead = chapters, pagesRead = chapters.toDouble())
                        )
                    } else {
                        val pages = editValue.toDoubleOrNull() ?: 0.0
                        viewModel.updateReadingRecord(
                            updatedRecord.copy(pagesRead = pages, fromPage = 0.0, toPage = pages)
                        )
                    }
                    showEditRecordDialog = false
                    recordToEdit = null
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRecordDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("阅读记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val records = uiState.readingRecords
        val isChapterBased = uiState.book?.progressType == ProgressType.CHAPTER

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("暂无阅读记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    ReadingRecordRow(
                        record = record,
                        isChapterBased = isChapterBased,
                        onEdit = {
                            recordToEdit = record
                            showEditRecordDialog = true
                        },
                        onDelete = {
                            recordToDelete = record
                            showDeleteRecordDialog = true
                        }
                    )
                }
            }
        }
    }
}
