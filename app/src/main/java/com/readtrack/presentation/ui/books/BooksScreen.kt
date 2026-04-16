package com.readtrack.presentation.ui.books

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.components.BookCard
import com.readtrack.presentation.ui.components.getStatusColor
import com.readtrack.presentation.viewmodel.BookSortOrder
import com.readtrack.presentation.viewmodel.BooksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    onBookClick: (Long) -> Unit,
    onAddBookClick: () -> Unit,
    onBookListClick: () -> Unit = {},
    onBatchAddToBookList: (List<Long>) -> Unit = {},
    viewModel: BooksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedBookIds by remember { mutableStateOf(setOf<Long>()) }
    var showAddToBookListDialog by remember { mutableStateOf(false) }

    val isSelectionMode = selectedBookIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Selection mode top bar
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text("${selectedBookIds.size} 本已选择")
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedBookIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showAddToBookListDialog = true
                        }) {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = "添加到书单"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                // Normal top bar
                SmallTopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            "我的书籍",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        Box {
                            TextButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = "排序",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(uiState.sortOrder.displayName)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                BookSortOrder.entries.forEach { sortOrder ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(sortOrder.displayName)
                                                if (uiState.sortOrder == sortOrder) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSortOrder(sortOrder)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onBookListClick) {
                            Icon(
                                Icons.Default.CollectionsBookmark,
                                contentDescription = "书单收藏夹",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            if (isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { showAddToBookListDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("加入书单")
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = onAddBookClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加书籍")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar - Modern Style
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("搜索书名、作者...") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Status Filter Chips - Modern Style
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = uiState.selectedStatus == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("全部") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                items(
                    items = BookStatus.entries,
                    key = { it.name }
                ) { status ->
                    FilterChip(
                        selected = uiState.selectedStatus == status,
                        onClick = { viewModel.setStatusFilter(status) },
                        label = { Text(status.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getStatusColor(status),
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Book List
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.filteredBooks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedStatus != null)
                                    "没有找到匹配的书籍"
                                else "还没有添加任何书籍",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.searchQuery.isEmpty() && uiState.selectedStatus == null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "点击右下角「添加书籍」开始",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.filteredBooks,
                            key = { it.id }
                        ) { book ->
                            val isSelected = book.id in selectedBookIds
                            BookCard(
                                book = book,
                                selected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedBookIds = if (isSelected) {
                                            selectedBookIds - book.id
                                        } else {
                                            selectedBookIds + book.id
                                        }
                                    } else {
                                        onBookClick(book.id)
                                    }
                                },
                                onLongClick = if (!isSelectionMode && book.id !in selectedBookIds) {
                                    { selectedBookIds = selectedBookIds + book.id }
                                } else null
                            )
                        }
                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddToBookListDialog && selectedBookIds.isNotEmpty()) {
        com.readtrack.presentation.ui.booklist.AddToBookListDialog(
            bookIds = selectedBookIds.toList(),
            onDismiss = {
                showAddToBookListDialog = false
                selectedBookIds = emptySet()
            }
        )
    }
}
