package com.readtrack.presentation.ui.addbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.*

data class ImageResult(
    val url: String,
    val title: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverSearchScreen(
    initialQuery: String = "",
    onImageSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var imageResults by remember { mutableStateOf<List<ImageResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索书籍封面", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入书名搜索封面") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        if (searchQuery.isNotBlank()) {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                val results = withContext(Dispatchers.IO) {
                                    searchBookCovers(searchQuery)
                                }
                                imageResults = results.first
                                errorMessage = results.second
                                isLoading = false
                            }
                        }
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Search button
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (searchQuery.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val results = withContext(Dispatchers.IO) {
                                searchBookCovers(searchQuery)
                            }
                            imageResults = results.first
                            errorMessage = results.second
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchQuery.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("搜索")
                }
            }

            // Error message
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Loading indicator
            if (isLoading && imageResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在搜索书籍封面...")
                    }
                }
            }

            // Results
            if (imageResults.isNotEmpty() && !isLoading) {
                Text(
                    "搜索结果（点击选择封面）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(imageResults) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.67f)
                                .clickable {
                                    selectedImageUrl = result.url
                                    showConfirmDialog = true
                                },
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = result.url,
                                    contentDescription = result.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }

            // Empty state
            if (!isLoading && imageResults.isEmpty() && errorMessage == null && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "没有找到封面图片",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "可以尝试输入更完整的书名",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Initial state
            if (searchQuery.isBlank() && imageResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "搜索书籍封面",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "输入书名获取相关封面",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog && selectedImageUrl != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认封面", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("确定使用此图片作为书籍封面？")
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = selectedImageUrl,
                        contentDescription = "预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onImageSelected(selectedImageUrl!!)
                    showConfirmDialog = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 搜索书籍封面 - 使用Google Books API
 */
private fun searchBookCovers(query: String): Pair<List<ImageResult>, String?> {
    return try {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=20"
        
        val url = URL(searchUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
        val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
        val response = reader.readText()
        reader.close()
        
        val imageResults = mutableListOf<ImageResult>()
        
        // 解析 Google Books JSON
        // 匹配 volumeInfo.imageLinks
        val volumeInfoPattern = Regex("\"volumeInfo\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\})*[^}]*)\\}")
        val imageLinksPattern = Regex("\"imageLinks\"\\s*:\\s*\\{([^}]+)\\}")
        val thumbnailPattern = Regex("\"thumbnail\"\\s*:\\s*\"([^\"]+)\"")
        val titlePattern = Regex("\"title\"\\s*:\\s*\"([^\"]+)\"")
        
        // 提取每个volume的imageLinks和title
        val seenUrls = mutableSetOf<String>()
        
        volumeInfoPattern.findAll(response).forEach { volumeMatch ->
            val volumeInfo = volumeMatch.groupValues[1]
            val titleMatch = titlePattern.find(volumeInfo)
            val title = titleMatch?.groupValues?.get(1) ?: query
            
            val imageLinksMatch = imageLinksPattern.find(volumeInfo)
            imageLinksMatch?.let { linksMatch ->
                val imageLinks = linksMatch.groupValues[1]
                thumbnailPattern.find(imageLinks)?.let { thumbMatch ->
                    var imgUrl = thumbMatch.groupValues[1]
                    // 替换http为https
                    imgUrl = imgUrl.replace("http://", "https://")
                    // 去掉zoom参数获取更大图片
                    imgUrl = imgUrl.replace("&zoom=1", "").replace("&zoom=0.5", "")
                    
                    if (!seenUrls.contains(imgUrl) && imgUrl.isNotBlank()) {
                        seenUrls.add(imgUrl)
                        imageResults.add(ImageResult(url = imgUrl, title = title))
                    }
                }
            }
        }
        
        if (imageResults.isEmpty()) {
            Pair(emptyList(), "未找到相关书籍封面，请尝试其他关键词")
        } else {
            Pair(imageResults, null)
        }
        
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(emptyList(), "搜索失败: ${e.message}")
    }
}
