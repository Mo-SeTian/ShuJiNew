package com.readtrack.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * 书籍封面组件
 * 支持：
 * - 普通图片URL (http/https)
 * - 本地文件URI (content://)
 * - Emoji封面 (emoji://color|emoji|title)
 * - 纯色封面 (color://hex)
 */
@Composable
fun BookCover(
    coverPath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    showPlaceholder: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            coverPath.isNullOrBlank() -> {
                // 无封面 - 显示占位符
                if (showPlaceholder) {
                    Text(
                        text = "📖",
                        fontSize = 32.sp
                    )
                }
            }
            coverPath.startsWith("emoji://") -> {
                // Emoji封面
                val parts = coverPath.removePrefix("emoji://").split("|")
                val colorHex = parts.getOrElse(0) { "CCCCCC" }
                val emoji = parts.getOrElse(1) { "📖" }
                val title = parts.getOrElse(2) { "" }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(android.graphics.Color.parseColor("#$colorHex"))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 36.sp
                        )
                        if (title.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                color = getContrastColor(colorHex),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            coverPath.startsWith("color://") -> {
                // 纯色封面
                val colorHex = coverPath.removePrefix("color://")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(android.graphics.Color.parseColor("#$colorHex")))
                ) {}
            }
            coverPath.startsWith("http") -> {
                // 网络图片
                AsyncImage(
                    model = coverPath,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                // 本地图片URI
                AsyncImage(
                    model = coverPath,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * 根据背景颜色计算对比色（黑色或白色）
 */
fun getContrastColor(backgroundHex: String): Color {
    return try {
        val color = android.graphics.Color.parseColor("#$backgroundHex")
        val luminance = (0.299 * android.graphics.Color.red(color) +
                       0.587 * android.graphics.Color.green(color) +
                       0.114 * android.graphics.Color.blue(color)) / 255
        if (luminance > 0.5) Color.Black else Color.White
    } catch (e: Exception) {
        Color.White
    }
}

/**
 * 获取颜色的Android Color Int值
 */
fun getContrastColorInt(backgroundHex: String): Int {
    return try {
        val color = android.graphics.Color.parseColor("#$backgroundHex")
        val luminance = (0.299 * android.graphics.Color.red(color) +
                       0.587 * android.graphics.Color.green(color) +
                       0.114 * android.graphics.Color.blue(color)) / 255
        if (luminance > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    } catch (e: Exception) {
        android.graphics.Color.WHITE
    }
}
