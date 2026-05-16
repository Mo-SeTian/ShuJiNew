package com.readtrack.presentation.ui.booklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.TagEntity
import com.readtrack.presentation.ui.components.BookCard
import com.readtrack.presentation.ui.components.getStatusColor
import com.readtrack.presentation.viewmodel.BookListDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListDetailScreen(
    bookListId: Long,
    onNavigateBack: () -> Unit,
    onBookClick: (Long) -> Unit,
    viewModel: BookListDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showEditCoverDialog by remember { mutableStateOf(false) }
    var showAddBooksDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var bookIdToRemove by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(bookListId) {
        viewModel.loadBookList(bookListId)
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.books.isNotEmpty()) {
                SmallFloatingActionButton(
                    onClick = { showAddBooksDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加书籍")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.bookList?.name ?: "书单",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.books.isNotEmpty()) {
                        IconButton(onClick = { showEditCoverDialog = true }) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "更换封面"
                            )
                        }
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.PlaylistRemove,
                                contentDescription = "清空书单",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "更多设置"
                            )
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("添加书籍") },
                                onClick = {
                                    showSettingsMenu = false
                                    showAddBooksDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "书单是空的",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "从书籍详情页添加到书单",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.bookList?.let { bookList ->
                    if (!bookList.description.isNullOrBlank()) {
                        item {
                            Text(
                                text = bookList.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                items(
                    items = uiState.books,
                    key = { it.id }
                ) { book ->
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    bookIdToRemove = book.id
                                    showRemoveDialog = true
                                }
                                false
                            }
                        ),
                        backgroundContent = {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "移除",
                                        modifier = Modifier.padding(end = 24.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        },
                        content = {
                            BookCard(
                                book = book,
                                onClick = { onBookClick(book.id) },
                                tags = uiState.bookTagMap[book.id] ?: emptyList()
                            )
                        }
                    )
                }
            }
        }
    }

    // Clear all books dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空书单") },
            text = { Text("确定要清空「${uiState.bookList?.name}」中的所有书籍吗？书籍不会被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearBookList()
                        showClearDialog = false
                    }
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Remove single book dialog
    if (showRemoveDialog) {
        bookIdToRemove?.let { bookId ->
            val bookName = uiState.books.find { it.id == bookId }?.title ?: ""
            AlertDialog(
                onDismissRequest = {
                    showRemoveDialog = false
                    bookIdToRemove = null
                },
                title = { Text("从书单移除") },
                text = { Text("确定要将「$bookName」从书单中移除吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.removeBookFromList(bookId)
                            showRemoveDialog = false
                            bookIdToRemove = null
                        }
                    ) {
                        Text("移除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRemoveDialog = false
                        bookIdToRemove = null
                    }) {
                        Text("取消")
                    }
                }
            )
        }
    }

    // Edit cover dialog
    if (showEditCoverDialog) {
        EditBookListCoverDialog(
            bookList = uiState.bookList,
            books = uiState.books,
            onDismiss = { showEditCoverDialog = false },
            onSelectCover = { bookId ->
                viewModel.updateCover(bookId)
                showEditCoverDialog = false
            },
            onAutoUpdate = {
                viewModel.updateCoverAuto()
                showEditCoverDialog = false
            },
            onRemoveCover = {
                viewModel.removeCover()
                showEditCoverDialog = false
            }
        )
    }

    // Add books dialog
    if (showAddBooksDialog) {
        AddBooksToListDialog(
            books = uiState.booksNotInList,
            bookTagMap = uiState.bookTagMap,
            onDismiss = { showAddBooksDialog = false },
            onConfirm = { bookIds ->
                viewModel.addBooksToList(bookIds)
                showAddBooksDialog = false
            }
        )
    }
}

@Composable
fun EditBookListCoverDialog(
    bookList: com.readtrack.data.local.entity.BookListEntity?,
    books: List<BookEntity>,
    onDismiss: () -> Unit,
    onSelectCover: (bookId: Long) -> Unit,
    onAutoUpdate: () -> Unit,
    onRemoveCover: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更换封面") },
        text = {
            Column {
                if (bookList?.coverPath != null) {
                    // Show current cover
                    AsyncImage(
                        model = bookList.coverPath,
                        contentDescription = "当前封面",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Auto update option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAutoUpdate() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("自动封面", fontWeight = FontWeight.Medium)
                            Text(
                                "自动选择第一本的封面",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Remove cover option
                if (bookList?.coverPath != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRemoveCover() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "移除封面",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Pick from books
                if (books.isNotEmpty()) {
                    Text(
                        "从书单中选择：",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(books.filter { !it.coverPath.isNullOrBlank() }) { book ->
                            Surface(
                                modifier = Modifier
                                    .size(60.dp, 85.dp)
                                    .clickable { onSelectCover(book.id) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = if (bookList?.coverBookId == book.id) {
                                    BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary
                                    )
                                } else null
                            ) {
                                AsyncImage(
                                    model = book.coverPath,
                                    contentDescription = book.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun AddBooksToListDialog(
    books: List<BookEntity>,
    bookTagMap: Map<Long, List<TagEntity>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredBooks = remember(books, searchQuery) {
        if (searchQuery.isBlank()) books
        else {
            val q = searchQuery.lowercase().trim()
            books.filter {
                it.title.lowercase().contains(q) ||
                    (it.author?.lowercase()?.contains(q) == true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (selectedIds.isEmpty()) "选择要添加的书籍"
                else "已选 ${selectedIds.size} 本"
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索书名、作者...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (books.isEmpty()) {
                    Text(
                        "所有书籍都已在此书单中",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (filteredBooks.isEmpty()) {
                    Text(
                        "没有匹配的书籍",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = filteredBooks,
                            key = { it.id }
                        ) { book ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (book.id in selectedIds) {
                                            selectedIds - book.id
                                        } else {
                                            selectedIds + book.id
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (book.id in selectedIds) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = book.id in selectedIds,
                                        onCheckedChange = {
                                            selectedIds = if (book.id in selectedIds) {
                                                selectedIds - book.id
                                            } else {
                                                selectedIds + book.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    if (book.coverPath != null) {
                                        AsyncImage(
                                            model = book.coverPath,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp, 56.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 状态标签
                                            val statusColor = getStatusColor(book.status)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = statusColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = book.status.displayName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = statusColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            // 标签
                                            val bookTags = bookTagMap[book.id] ?: emptyList()
                                            bookTags.take(3).forEach { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = tag.name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (bookTags.size > 3) {
                                                Text(
                                                    text = "+${bookTags.size - 3}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
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
            TextButton(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("添加${if (selectedIds.isNotEmpty()) "(${selectedIds.size})" else ""}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

