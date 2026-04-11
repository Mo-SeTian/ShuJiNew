package com.readtrack.presentation.ui.addbook

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索封面图片", fontWeight = FontWeight.Bold) },
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
                label = { Text("搜索书名或关键词") },
                placeholder = { Text("输入书名搜索封面") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        if (searchQuery.isNotBlank()) {
                            isLoading = true
                            performSearch(searchQuery) { results, error ->
                                imageResults = results
                                errorMessage = error
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
                        performSearch(searchQuery) { results, error ->
                            imageResults = results
                            errorMessage = error
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
                        Text("正在搜索图片...")
                    }
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(imageResults) { image ->
                        Card(
                            modifier = Modifier
                                .aspectRatio(0.7f)
                                .clickable {
                                    selectedImageUrl = image.url
                                    showConfirmDialog = true
                                },
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (selectedImageUrl == image.url) 8.dp else 2.dp
                            )
                        ) {
                            Box {
                                AsyncImage(
                                    model = image.url,
                                    contentDescription = image.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    onError = {}
                                )
                                if (selectedImageUrl == image.url) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            "✓",
                                            modifier = Modifier.padding(4.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (!isLoading && imageResults.isEmpty() && errorMessage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "🔍",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "输入书名搜索封面图片",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Confirm Dialog
    if (showConfirmDialog && selectedImageUrl != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认选择") },
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
                TextButton(
                    onClick = {
                        onImageSelected(selectedImageUrl!!)
                        showConfirmDialog = false
                    }
                ) {
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

private fun performSearch(query: String, callback: (List<ImageResult>, String?) -> Unit) {
    Thread {
        try {
            val encodedQuery = URLEncoder.encode(query + " 书籍封面", "UTF-8")
            
            // 使用必应图片搜索
            val searchUrl = "https://www.bing.com/images/search?q=$encodedQuery&first=0&count=30"
            val url = URL(searchUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            val response = reader.readText()
            reader.close()
            
            val imageResults = mutableListOf<ImageResult>()
            
            // Parse image URLs from bing response
            val murlPattern = Regex(""""murl"""\s*:\s*""([^""]+)""")
            val thumbnailPattern = Regex(""""turl"""\s*:\s*""([^""]+)""")
            
            val seenUrls = mutableSetOf<String>()
            
            murlPattern.findAll(response).forEach { match ->
                var imgUrl = match.groupValues[1]
                imgUrl = imgUrl.replace("\/", "/")
                if (!seenUrls.contains(imgUrl) && isValidImageUrl(imgUrl)) {
                    seenUrls.add(imgUrl)
                    imageResults.add(ImageResult(url = imgUrl, title = query))
                }
            }
            
            if (imageResults.size < 10) {
                thumbnailPattern.findAll(response).forEach { match ->
                    var imgUrl = match.groupValues[1]
                    imgUrl = imgUrl.replace("\/", "/")
                    if (!seenUrls.contains(imgUrl) && isValidImageUrl(imgUrl)) {
                        seenUrls.add(imgUrl)
                        imageResults.add(ImageResult(url = imgUrl, title = query))
                    }
                }
            }
            
            callback(imageResults.take(30), null)
        } catch (e: Exception) {
            callback(emptyList(), "搜索失败: ${e.message}")
        }
    }.start()
}

private fun isValidImageUrl(url: String): Boolean {
    if (!url.startsWith("http")) return false
    val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")
    return imageExtensions.any { url.lowercase().contains(it) }
}
