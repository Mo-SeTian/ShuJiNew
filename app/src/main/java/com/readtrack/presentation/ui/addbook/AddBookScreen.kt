package com.readtrack.presentation.ui.addbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.readtrack.presentation.ui.components.BookCover
import com.readtrack.presentation.viewmodel.AddBookViewModel
import com.readtrack.presentation.viewmodel.CoverSelectionHolder
import com.readtrack.presentation.viewmodel.ProgressType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    navController: NavController,
    bookId: Long? = null,
    onNavigateBack: () -> Unit = { navController.popBackStack() }
) {
    val viewModel: AddBookViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 关键修复：监听导航状态变化
    // 当从 CoverPicker 返回时，这个 flow 会 emit 新值，触发 LaunchedEffect
    val currentEntry by navController.currentBackStackEntryFlow.collectAsState()

    // 追踪已处理的封面，避免重复更新
    var processedCover by remember { mutableStateOf<String?>(null) }

    // 当导航返回 AddBookScreen 时，检查并处理封面
    LaunchedEffect(currentEntry) {
        // 只有当前在 AddBookScreen 时才处理
        if (currentEntry?.destination?.route?.startsWith("add_book") == true) {
            val selectedCover = CoverSelectionHolder.consume()
            if (selectedCover != null && selectedCover != processedCover) {
                viewModel.updateCoverUri(selectedCover)
                processedCover = selectedCover
            }
        }
    }

    // 初始化
    LaunchedEffect(bookId) {
        if (bookId != null) {
            viewModel.loadBook(bookId)
        } else {
            viewModel.resetState()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (bookId != null) "编辑书籍" else "添加书籍",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveBook() },
                        enabled = uiState.title.isNotBlank()
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 封面预览
            BookCover(
                coverUri = uiState.coverUri,
                title = uiState.title,
                modifier = Modifier
                    .width(120.dp)
                    .height(160.dp)
                    .clickable {
                        navController.navigate("cover_picker?coverUri=${uiState.coverUri ?: ""}")
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "点击更换封面",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 书名
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("书名 *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 作者
            OutlinedTextField(
                value = uiState.author,
                onValueChange = { viewModel.updateAuthor(it) },
                label = { Text("作者") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 出版社
            OutlinedTextField(
                value = uiState.publisher,
                onValueChange = { viewModel.updatePublisher(it) },
                label = { Text("出版社") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 进度类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "阅读进度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    FilterChip(
                        selected = uiState.progressType == ProgressType.PAGE,
                        onClick = { viewModel.updateProgressType(ProgressType.PAGE) },
                        label = { Text("页码") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = uiState.progressType == ProgressType.CHAPTER,
                        onClick = { viewModel.updateProgressType(ProgressType.CHAPTER) },
                        label = { Text("章节") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 进度输入
            if (uiState.progressType == ProgressType.PAGE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = if (uiState.currentPage > 0) uiState.currentPage.toString() else "",
                        onValueChange = { viewModel.updateCurrentPage(it.toIntOrNull() ?: 0) },
                        label = { Text("当前页") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = if (uiState.totalPages > 0) uiState.totalPages.toString() else "",
                        onValueChange = { viewModel.updateTotalPages(it.toIntOrNull() ?: 0) },
                        label = { Text("总页数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            } else {
                OutlinedTextField(
                    value = if (uiState.currentChapter > 0) uiState.currentChapter.toString() else "",
                    onValueChange = { viewModel.updateCurrentChapter(it.toIntOrNull() ?: 0) },
                    label = { Text("当前章节") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = if (uiState.totalChapters > 0) uiState.totalChapters.toString() else "",
                    onValueChange = { viewModel.updateTotalChapters(it.toIntOrNull() ?: 0) },
                    label = { Text("总章节数") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 阅读状态
            Text(
                text = "阅读状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookStatus.entries.forEach { status ->
                    FilterChip(
                        selected = uiState.status == status,
                        onClick = { viewModel.updateStatus(status) },
                        label = { Text(status.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 笔记
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("笔记") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
