package com.readtrack.presentation.ui.addbook

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
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
import com.readtrack.presentation.viewmodel.AddBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddBookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        viewModel.updateCoverUri(uri)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加书籍") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveBook() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp)
                        } else {
                            Text("保存")
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cover Image
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { photoPickerLauncher.launch(PickVisualMediaRequest()) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.coverUri != null) {
                        AsyncImage(
                            model = uiState.coverUri,
                            contentDescription = "书籍封面",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "点击选择封面",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("书名 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.errorMessage?.contains("书名") == true,
                supportingText = if (uiState.errorMessage?.contains("书名") == true) {
                    { Text(uiState.errorMessage ?: "") }
                } else null
            )

            // Author
            OutlinedTextField(
                value = uiState.author,
                onValueChange = { viewModel.updateAuthor(it) },
                label = { Text("作者") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Total Pages
            OutlinedTextField(
                value = uiState.totalPages,
                onValueChange = { viewModel.updateTotalPages(it) },
                label = { Text("总页数 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.errorMessage?.contains("页数") == true,
                supportingText = if (uiState.errorMessage?.contains("页数") == true) {
                    { Text(uiState.errorMessage ?: "") }
                } else null
            )

            // Status Selection - Now clickable!
            Text(
                text = "书籍状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            BookStatus.entries.forEach { status ->
                val isSelected = uiState.status == status
                val statusColor = getStatusColor(status)

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateStatus(status) },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) statusColor.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) statusColor else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.updateStatus(status) },
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
                                style = MaterialTheme.typography.bodyLarge,
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
