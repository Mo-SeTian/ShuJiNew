package com.readtrack.presentation.ui.booklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.presentation.viewmodel.AddToBookListViewModel

/**
 * 显示书籍添加到书单的对话框。
 * 支持单书模式和批量模式（传入多个 bookIds）。
 * 支持查看当前书籍所属的所有书单，以及创建新书单。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToBookListDialog(
    bookIds: List<Long>,
    onDismiss: () -> Unit,
    viewModel: AddToBookListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    val isBatch = bookIds.size > 1

    LaunchedEffect(bookIds) {
        if (isBatch) {
            viewModel.loadBookListsForBooks(bookIds)
        } else {
            viewModel.loadBookListsForBook(bookIds.first())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBatch) "批量加入书单" else "加入书单")
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                if (isBatch) {
                    Text(
                        "已选择 ${bookIds.size} 本书",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Create new booklist option
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCreateDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "创建新书单",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (uiState.allBookLists.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "还没有书单，创建一个吧",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(
                                items = uiState.allBookLists,
                                key = { it.id }
                            ) { bookList ->
                                val isInList = uiState.bookIdsInList[bookList.id] == true
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isBatch) {
                                                viewModel.toggleBookListMembership(bookList.id)
                                            } else {
                                                viewModel.toggleBookListMembership(bookIds.first(), bookList.id)
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isInList)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Book list cover
                                        if (bookList.coverPath != null) {
                                            AsyncImage(
                                                model = bookList.coverPath,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(36.dp, 50.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Surface(
                                                modifier = Modifier.size(36.dp, 50.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Book,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bookList.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${bookList.bookCount}本书",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (isInList) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "已在书单中",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )

    if (showCreateDialog) {
        CreateBookListDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                if (isBatch) {
                    viewModel.createBookListAndAddBooks(name, description)
                } else {
                    viewModel.createBookListAndAddBook(bookIds.first(), name, description)
                }
                showCreateDialog = false
            }
        )
    }
}
