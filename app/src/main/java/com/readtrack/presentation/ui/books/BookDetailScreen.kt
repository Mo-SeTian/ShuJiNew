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
import androidx.compose.material.icons.filled.Edit
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
    onNavigateBack: () -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    // Capture book value for use in dialogs
    val book = uiState.book

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书籍详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Edit */ }) {
                        Icon(Icons.Default.Edit, "编辑")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "删除")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading || book == null) {
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Book Header
                item { BookHeader(book = book) }

                // Progress Card
                item { ProgressCard(book = book) }

                // Status Change Card
                item {
                    Card(
                        onClick = { showStatusDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "更改状态",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "点击更改当前阅读状态",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Quick Status Selection
                item {
                    Column {
                        Text(
                            text = "快速切换状态",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BookStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = book.status == status,
                                    onClick = { viewModel.updateStatus(status) },
                                    label = {
                                        Text(
                                            text = when (status) {
                                                BookStatus.WANT_TO_READ -> "想读"
                                                BookStatus.READING -> "阅读中"
                                                BookStatus.FINISHED -> "已读"
                                                BookStatus.ON_HOLD -> "闲置"
                                                BookStatus.ABANDONED -> "放弃"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Add Reading Record Button
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = { showAddRecordDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = book.status == BookStatus.READING
                            ) {
                                Text("添加阅读记录")
                            }
                            if (book.status != BookStatus.READING) {
                                Text(
                                    text = "提示：将状态改为「阅读中」后可添加阅读记录",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Reading Records List
                item {
                    Text(
                        text = "阅读记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.readingRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "暂无阅读记录",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.readingRecords) { record ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            .format(Date(record.date)),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "第 ${record.fromPage.toInt()} - ${record.toPage.toInt()} 页",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "+${record.pagesRead.toInt()} 页",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Status Change Dialog
    if (showStatusDialog && book != null) {
        StatusChangeDialog(
            currentStatus = book.status,
            onDismiss = { showStatusDialog = false },
            onStatusSelected = { status ->
                viewModel.updateStatus(status)
                showStatusDialog = false
            }
        )
    }

    // Add Reading Record Dialog
    if (showAddRecordDialog && book != null) {
        AddRecordDialog(
            currentPage = book.currentPage,
            totalPages = book.totalPages,
            onDismiss = { showAddRecordDialog = false },
            onConfirm = { pages ->
                viewModel.addReadingRecord(pages)
                showAddRecordDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && book != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${book.title}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBook()
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun BookHeader(book: BookEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = book.coverPath ?: "https://via.placeholder.com/150x225",
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(120.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = Font
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (!book.author.isNullOrBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = when (book.status) {
                            BookStatus.WANT_TO_READ -> "想读"
                            BookStatus.READING -> "阅读中"
                            BookStatus.FINISHED -> "已读"
                            BookStatus.ON_HOLD -> "闲置"
                            BookStatus.ABANDONED -> "放弃"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = getStatusColor(book.status).copy(alpha = 0.2f)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "共 ${book.totalPages.toInt()} 页",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ProgressCard(book: BookEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "阅读进度",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            val progress = if (book.totalPages > 0)
                (book.currentPage / book.totalPages).toFloat()
            else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = getStatusColor(book.status)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${book.currentPage.toInt()} / ${book.totalPages.toInt()} 页 (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusChangeDialog(
    currentStatus: BookStatus,
    onDismiss: () -> Unit,
    onStatusSelected: (BookStatus) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更改书籍状态") },
        text = {
            Column {
                Text("请选择新的书籍状态：")
                Spacer(modifier = Modifier.height(16.dp))
                BookStatus.entries.forEach { status ->
                    val isSelected = status == currentStatus
                    Card(
                        onClick = { onStatusSelected(status) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                getStatusColor(status).copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected)
                            BorderStroke(2.dp, getStatusColor(status))
                        else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (status) {
                                    BookStatus.WANT_TO_READ -> "想读"
                                    BookStatus.READING -> "阅读中"
                                    BookStatus.FINISHED -> "已读"
                                    BookStatus.ON_HOLD -> "闲置"
                                    BookStatus.ABANDONED -> "放弃"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text("✓", color = getStatusColor(status))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AddRecordDialog(
    currentPage: Double,
    totalPages: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var pagesText by remember { mutableStateOf("") }
    val remainingPages = totalPages - currentPage

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加阅读记录") },
        text = {
            Column {
                Text("当前进度：第 ${currentPage.toInt()} 页")
                Text("剩余页数：${remainingPages.toInt()} 页")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pagesText,
                    onValueChange = { pagesText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("本次阅读页数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    pagesText.toDoubleOrNull()?.let { pages ->
                        if (pages > 0 && pages <= remainingPages) {
                            onConfirm(pages)
                        }
                    }
                },
                enabled = pagesText.toDoubleOrNull()?.let { it > 0 && it <= remainingPages } == true
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

