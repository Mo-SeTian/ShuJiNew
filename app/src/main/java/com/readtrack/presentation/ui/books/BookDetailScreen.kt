package com.readtrack.presentation.ui.books

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
    var selectedNewStatus by remember { mutableStateOf<BookStatus?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading || uiState.book == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val book = uiState.book!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Book Header
                item {
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

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
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
                            
                            // Current status - clickable to change
                            AssistChip(
                                onClick = { showStatusDialog = true },
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
                                leadingIcon = {
                                    Text("🏷️", style = MaterialTheme.typography.bodyMedium)
                                },
                                trailingIcon = {
                                    Text("▼", style = MaterialTheme.typography.bodySmall)
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

                // Progress Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${book.currentPage.toInt()} / ${book.totalPages.toInt()} 页",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Status Change Card - Now clickable!
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStatusDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "更改状态",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "点击更改书籍状态",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "更改状态",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Quick Status Selection
                item {
                    Text(
                        text = "快速切换状态",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BookStatus.entries.forEach { status ->
                            val isSelected = book.status == status
                            val statusColor = getStatusColor(status)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        selectedNewStatus = status
                                        showStatusDialog = true
                                    }
                                },
                                label = {
                                    Text(
                                        text = when (status) {
                                            BookStatus.WANT_TO_READ -> "想读"
                                            BookStatus.READING -> "阅读中"
                                            BookStatus.FINISHED -> "已读"
                                            BookStatus.ON_HOLD -> "闲置"
                                            BookStatus.ABANDONED -> "放弃"
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = statusColor,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Add Reading Record Button
                item {
                    Button(
                        onClick = { showAddRecordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = book.status == BookStatus.READING
                    ) {
                        Text("添加阅读记录")
                    }
                    if (book.status != BookStatus.READING) {
                        Text(
                            text = "提示：将状态改为"阅读中"后可添加阅读记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Status Change Dialog
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("更改书籍状态") },
            text = {
                Column {
                    Text(
                        text = "请选择新的书籍状态：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BookStatus.entries.forEach { status ->
                        val isSelected = book.status == status
                        val statusColor = getStatusColor(status)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isSelected) {
                                        viewModel.updateStatus(status)
                                        showStatusDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        viewModel.updateStatus(status)
                                        showStatusDialog = false
                                    }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = statusColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = when (status) {
                                        BookStatus.WANT_TO_READ -> "想读"
                                        BookStatus.READING -> "阅读中"
                                        BookStatus.FINISHED -> "已读"
                                        BookStatus.ON_HOLD -> "闲置"
                                        BookStatus.ABANDONED -> "放弃"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (status) {
                                        BookStatus.WANT_TO_READ -> "计划阅读的书籍"
                                        BookStatus.READING -> "正在阅读的书籍"
                                        BookStatus.FINISHED -> "已完成阅读"
                                        BookStatus.ON_HOLD -> "暂时搁置"
                                        BookStatus.ABANDONED -> "决定不再阅读"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Add Reading Record Dialog
    if (showAddRecordDialog) {
        var pagesInput by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddRecordDialog = false },
            title = { Text("添加阅读记录") },
            text = {
                Column {
                    Text(
                        text = "当前进度：第 ${book.currentPage.toInt()} 页",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pagesInput,
                        onValueChange = { pagesInput = it },
                        label = { Text("本次阅读页数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pages = pagesInput.toDoubleOrNull()
                        if (pages != null && pages > 0) {
                            viewModel.addReadingRecord(pages)
                            showAddRecordDialog = false
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRecordDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除《${book.title}》吗？此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBook()
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
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
