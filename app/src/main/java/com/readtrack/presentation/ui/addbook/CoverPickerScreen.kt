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
import androidx.compose.ui.unit.sp
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
                        onClick = {
                            if (selectedCover != null) {
                                onCoverSelected(selectedCover)
                            }
                        },
                        enabled = selectedCover != null
                    ) {
                        Text("完成 ✓", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        // 相册选择器
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                selectedCover = it.toString()
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 从相册选择
            item {
                CoverOptionCard(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    content = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "相册",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "相册",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
            
            // 在线搜索（保留作为备选）
            item {
                CoverOptionCard(
                    onClick = onSearchOnline,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    content = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "在线搜索",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
            }
            
            // 分隔标题 - emoji封面
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "📚 分类封面",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Emoji封面
            items(DefaultCovers.covers) { cover ->
                val isSelected = selectedCover == cover.url
                CoverOptionCard(
                    onClick = { selectedCover = cover.url },
                    backgroundColor = Color(android.graphics.Color.parseColor("#${cover.colorHex}")),
                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    content = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                cover.emoji,
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                cover.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = getContrastColor(cover.colorHex),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )
            }
            
            // 分隔标题 - 纯色
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "🎨 纯色封面",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // 纯色封面
            items(DefaultCovers.solidColors) { colorHex ->
                val isSelected = selectedCover == "color://$colorHex"
                Box(
                    modifier = Modifier
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(android.graphics.Color.parseColor("#$colorHex")))
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedCover = "color://$colorHex"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "已选择",
                            tint = getContrastColor(colorHex),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverOptionCard(
    onClick: () -> Unit,
    backgroundColor: Color,
    borderColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (borderColor != Color.Transparent) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * 根据背景颜色获取对比色（黑或白）
 */
private fun getContrastColor(colorHex: String): Color {
    return try {
        val color = android.graphics.Color.parseColor("#$colorHex")
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        // 计算亮度
        val brightness = (r * 299 + g * 587 + b * 114) / 1000
        if (brightness > 150) Color.Black else Color.White
    } catch (e: Exception) {
        Color.White
    }
}

/**
 * 根据封面路径渲染预览
 */
@Composable
fun CoverPreview(
    coverPath: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = "书籍封面"
) {
    when {
        coverPath.isNullOrBlank() -> {
            // 默认占位符
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("📖", fontSize = 40.sp)
            }
        }
        coverPath.startsWith("emoji://") -> {
            // Emoji封面
            val parts = coverPath.removePrefix("emoji://").split("|")
            val colorHex = parts.getOrElse(0) { "CCCCCC" }
            val emoji = parts.getOrElse(1) { "📖" }
            val title = parts.getOrElse(2) { "" }
            
            Box(
                modifier = modifier.background(Color(android.graphics.Color.parseColor("#$colorHex"))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 48.sp)
                    if (title.isNotEmpty()) {
                        Text(
                            title,
                            fontSize = 14.sp,
                            color = getContrastColor(colorHex),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        coverPath.startsWith("color://") -> {
            // 纯色封面
            val colorHex = coverPath.removePrefix("color://")
            Box(
                modifier = modifier.background(Color(android.graphics.Color.parseColor("#$colorHex"))),
                contentAlignment = Alignment.Center
            ) {}
        }
        coverPath.startsWith("http") -> {
            // 网络图片
            AsyncImage(
                model = coverPath,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        else -> {
            // 本地图片
            AsyncImage(
                model = coverPath,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
    }
}