package com.readtrack.presentation.ui.books

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.components.BookCard
import com.readtrack.presentation.ui.components.getStatusColor
import com.readtrack.presentation.viewmodel.BooksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    onBookClick: (Long) -> Unit,
    onAddBookClick: () -> Unit,
    viewModel: BooksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Text(
                        "我的书籍",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                BookStatus.entries.forEach { status ->
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
            if (uiState.filteredBooks.isEmpty()) {
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
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.filteredBooks,
                        key = { it.id }
                    ) { book ->
                        BookCard(
                            book = book,
                            onClick = { onBookClick(book.id) }
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
