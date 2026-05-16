package com.readtrack.presentation.ui.books

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
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
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showTagDropdown by remember { mutableStateOf(false) }
    var showBookListDropdown by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // 展开搜索框时同步状态并自动聚焦
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            searchText = uiState.searchQuery
            focusRequester.requestFocus()
        }
    }

    // 筛选条件变化时，列表回到顶部
    LaunchedEffect(
        uiState.selectedStatuses,
        uiState.selectedTagIds,
        uiState.selectedBookListIds,
        uiState.searchQuery,
        uiState.sortOrder
    ) {
        listState.animateScrollToItem(0)
    }

    val isSelectionMode = selectedBookIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Selection mode top bar
                TopAppBar(
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
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                // Normal top bar
                SmallTopAppBar(
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
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)),
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150))
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showAddToBookListDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("加入书单")
                }
            }
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)),
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150))
            ) {
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
            // Search Bar - Expandable (collapsed when not in use)
            AnimatedVisibility(
                visible = searchExpanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(tween(150))
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.setSearchQuery(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("搜索书名、作者...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchText = ""
                                    viewModel.setSearchQuery("")
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "清除搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    searchExpanded = false
                                    searchText = ""
                                    viewModel.setSearchQuery("")
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "收起搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }

            // Filter Row: search trigger (when collapsed) + Status + Tag + Booklist dropdowns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!searchExpanded) {
                    FilterChip(
                        selected = uiState.searchQuery.isNotEmpty(),
                        onClick = { searchExpanded = true },
                        label = {
                            Text(
                                if (uiState.searchQuery.isNotEmpty()) uiState.searchQuery
                                else "搜索"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                // Status filter
                Box {
                    val statusCount = uiState.selectedStatuses.size
                    FilterChip(
                        selected = statusCount > 0,
                        onClick = { showStatusDropdown = true },
                        label = {
                            Text(
                                if (statusCount > 0) "状态($statusCount)"
                                else "状态筛选"
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    DropdownMenu(
                        expanded = showStatusDropdown,
                        onDismissRequest = { showStatusDropdown = false }
                    ) {
                        BookStatus.entries.forEach { status ->
                            val isSelected = status in uiState.selectedStatuses
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Checkbox) { viewModel.toggleStatusFilter(status) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            getStatusColor(status),
                                            RoundedCornerShape(5.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = status.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (statusCount > 0) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            TextButton(
                                onClick = {
                                    viewModel.clearStatusFilters()
                                    showStatusDropdown = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("清除状态筛选", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Tag filter
                Box {
                    val tagCount = uiState.selectedTagIds.size
                    FilterChip(
                        selected = tagCount > 0,
                        onClick = { showTagDropdown = true },
                        label = {
                            Text(
                                if (tagCount > 0) "标签($tagCount)"
                                else "标签筛选"
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    DropdownMenu(
                        expanded = showTagDropdown,
                        onDismissRequest = { showTagDropdown = false }
                    ) {
                        if (uiState.allTags.isEmpty()) {
                            Text(
                                text = "暂无标签",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        } else {
                            uiState.allTags.forEach { tag ->
                                val isSelected = tag.id in uiState.selectedTagIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(role = Role.Checkbox) { viewModel.toggleTagFilter(tag.id) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (tagCount > 0) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                                TextButton(
                                    onClick = {
                                        viewModel.clearTagFilters()
                                        showTagDropdown = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("清除标签筛选", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Booklist filter
                Box {
                    val bookListCount = uiState.selectedBookListIds.size
                    FilterChip(
                        selected = bookListCount > 0,
                        onClick = { showBookListDropdown = true },
                        label = {
                            Text(
                                if (bookListCount > 0) "书单($bookListCount)"
                                else "书单筛选"
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    DropdownMenu(
                        expanded = showBookListDropdown,
                        onDismissRequest = { showBookListDropdown = false }
                    ) {
                        if (uiState.allBookLists.isEmpty()) {
                            Text(
                                text = "暂无书单",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        } else {
                            uiState.allBookLists.forEach { bookList ->
                                val isSelected = bookList.id in uiState.selectedBookListIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(role = Role.Checkbox) { viewModel.toggleBookListFilter(bookList.id) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = bookList.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (bookListCount > 0) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                                TextButton(
                                    onClick = {
                                        viewModel.clearBookListFilters()
                                        showBookListDropdown = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("清除书单筛选", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                                text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedStatuses.isNotEmpty() || uiState.selectedTagIds.isNotEmpty() || uiState.selectedBookListIds.isNotEmpty())
                                    "没有找到匹配的书籍"
                                else "还没有添加任何书籍",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.searchQuery.isEmpty() && uiState.selectedStatuses.isEmpty() && uiState.selectedTagIds.isEmpty() && uiState.selectedBookListIds.isEmpty()) {
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
                        state = listState,
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
                                } else null,
                                tags = uiState.bookTagMap[book.id] ?: emptyList()
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
