package com.readtrack.presentation.ui.books

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.components.getStatusColor
import com.readtrack.presentation.viewmodel.BookDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: Long,
    onNavigateBack: () -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showAddRecordDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书籍详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            uiState.book?.let { book ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { BookHeader(book) }
                    item { ProgressCard(book) }
                    item {
                        Card(onClick = { showStatusDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("更改状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("→", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    item {
                        Text("快速切换状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BookStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = book.status == status,
                                    onClick = { viewModel.updateStatus(status) },
                                    label = { Text(getStatusLabel(status), style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    item {
                        Button(onClick = { showAddRecordDialog = true }, modifier = Modifier.fillMaxWidth(), enabled = book.status == BookStatus.READING) {
                            Text("添加阅读记录")
                        }
                        if (book.status != BookStatus.READING) {
                            Text("提示：将状态改为阅读中后可添加阅读记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item { Text("阅读记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    if (uiState.readingRecords.isEmpty()) {
                        item { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("暂无阅读记录", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    } else {
                        items(uiState.readingRecords) { record ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.date)), style = MaterialTheme.typography.bodyMedium)
                                        Text("第 ${record.fromPage.toInt()} - ${record.toPage.toInt()} 页", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("+${record.pagesRead.toInt()} 页", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStatusDialog && uiState.book != null) {
        val currentStatus = uiState.book!!.status
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("更改书籍状态") },
            text = {
                Column {
                    BookStatus.entries.forEach { status ->
                        val isSelected = status == currentStatus
                        Card(
                            onClick = { viewModel.updateStatus(status); showStatusDialog = false },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) getStatusColor(status).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                            border = if (isSelected) BorderStroke(2.dp, getStatusColor(status)) else null
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(getStatusLabel(status), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (isSelected) { Text("✓", color = getStatusColor(status)) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStatusDialog = false }) { Text("取消") } }
        )
    }

    if (showAddRecordDialog && uiState.book != null) {
        val book = uiState.book!!
        var pagesText by remember { mutableStateOf("") }
        val remaining = book.totalPages - book.currentPage
        AlertDialog(
            onDismissRequest = { showAddRecordDialog = false },
            title = { Text("添加阅读记录") },
            text = {
                Column {
                    Text("当前进度：第 ${book.currentPage.toInt()} 页")
                    Text("剩余页数：${remaining.toInt()} 页")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = pagesText, onValueChange = { pagesText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("本次阅读页数") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { pagesText.toDoubleOrNull()?.let { p -> if (p > 0 && p <= remaining) { viewModel.addReadingRecord(p); showAddRecordDialog = false } } },
                    enabled = pagesText.toDoubleOrNull()?.let { it > 0 && it <= remaining } == true
                ) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showAddRecordDialog = false }) { Text("取消") } }
        )
    }

    if (showDeleteDialog && uiState.book != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${uiState.book!!.title}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteBook(); showDeleteDialog = false; onNavigateBack() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun BookHeader(book: BookEntity) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AsyncImage(model = book.coverPath ?: "https://via.placeholder.com/150x225", contentDescription = book.title, contentScale = ContentScale.Crop, modifier = Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(12.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!book.author.isNullOrBlank()) {
                Text(book.author, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            AssistChip(onClick = { }, label = { Text(getStatusLabel(book.status), fontWeight = FontWeight.Bold) }, colors = AssistChipDefaults.assistChipColors(containerColor = getStatusColor(book.status).copy(alpha = 0.2f)))
            Spacer(Modifier.height(8.dp))
            Text("共 ${book.totalPages.toInt()} 页", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ProgressCard(book: BookEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("阅读进度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val progress = if (book.totalPages > 0) (book.currentPage / book.totalPages).toFloat() else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = getStatusColor(book.status))
            Spacer(Modifier.height(8.dp))
            Text("${book.currentPage.toInt()} / ${book.totalPages.toInt()} 页 (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun getStatusLabel(status: BookStatus): String = when (status) {
    BookStatus.WANT_TO_READ -> "想读"
    BookStatus.READING -> "阅读中"
    BookStatus.FINISHED -> "已读"
    BookStatus.ON_HOLD -> "闲置"
    BookStatus.ABANDONED -> "放弃"
}
