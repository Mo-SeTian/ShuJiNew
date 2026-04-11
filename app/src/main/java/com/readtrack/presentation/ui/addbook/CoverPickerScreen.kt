package com.readtrack.presentation.ui.addbook

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPickerScreen(
    initialCoverPath: String? = null,
    onCoverSelected: (String?) -> Unit,
    onNavigateBack: () -> Unit,
    onSearchOnline: () -> Unit = {}
) {
    var selectedCover by remember { mutableStateOf(initialCoverPath) }
    
    // 颜色选项
    val colorOptions = listOf(
        Color(0xFFFF6B6B), // 红
        Color(0xFFFF9F43), // 橙
        Color(0xFFFFC93C), // 黄
        Color(0xFF6BCB77), // 绿
        Color(0xFF4D96FF), // 蓝
        Color(0xFF9B59B6), // 紫
        Color(0xFFE91E63), // 粉
        Color(0xFF795548), // 棕
        Color(0xFF607D8B), // 灰蓝
        Color(0xFF000000), // 黑
    )
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onCoverSelected(it.toString())
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择封面", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { selectedCover?.let { onCoverSelected(it) } },
                        enabled = selectedCover != null
                    ) {
                        Text("完成", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 从相册选择
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.67f)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "相册",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "相册",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 在线搜索
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.67f)
                        .clickable { onSearchOnline() },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "在线搜索",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // 颜色选项
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.67f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "纯色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            items(colorOptions) { color ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.67f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .clickable {
                            selectedCover = "color://${color.hashCode()}"
                        }
                        .then(
                            if (selectedCover == "color://${color.hashCode()}") {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedCover == "color://${color.hashCode()}") {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "已选择",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            // 图片封面
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.67f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "图片封面",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            items(DefaultCovers.covers.filter { it.url.startsWith("https://") }) { cover ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.67f)
                        .clickable {
                            selectedCover = cover.url
                        },
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedCover == cover.url) 4.dp else 1.dp
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = cover.url,
                            contentDescription = cover.category,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (selectedCover == cover.url) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "已选择",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 根据封面路径生成显示
 */
@Composable
fun CoverPreview(
    coverPath: String?,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit = {}
) {
    if (coverPath.isNullOrBlank()) {
        placeholder()
    } else if (coverPath.startsWith("color://")) {
        val colorHash = coverPath.removePrefix("color://").toIntOrNull() ?: 0
        Box(
            modifier = modifier.background(Color(colorHash)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ColorLens,
                contentDescription = "封面颜色",
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    } else if (coverPath.startsWith("http")) {
        AsyncImage(
            model = coverPath,
            contentDescription = "书籍封面",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        AsyncImage(
            model = coverPath,
            contentDescription = "书籍封面",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}