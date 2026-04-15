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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpSize
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import coil.size.Precision

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
    showPlaceholder: Boolean = true,
    requestSize: DpSize? = null,
    quality: BookCoverQuality = BookCoverQuality.PREVIEW
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val requestSizePx = remember(requestSize, density) {
        requestSize?.let {
            with(density) {
                it.width.roundToPx() to it.height.roundToPx()
            }
        }
    }
    val imageModel = remember(coverPath, requestSizePx, quality) {
        if (!coverPath.isNullOrBlank() && coverPath.startsWith("http")) {
            buildBookImageRequest(
                context = context,
                imageUrl = coverPath,
                requestedSizePx = requestSizePx,
                quality = quality
            )
        } else {
            coverPath
        }
    }

    // 缓存 emoji 封面解析结果，避免每次 recompose 都重新解析
    val emojiData = remember(coverPath) {
        if (!coverPath.isNullOrBlank() && coverPath.startsWith("emoji://")) {
            val emojiContent = coverPath.removePrefix("emoji://")
            val parts = emojiContent.split("|")
            val isSingleChar = parts.size == 1 && emojiContent.length <= 2
            val colorHex = parts.getOrElse(0) { "CCCCCC" }
            val parsedColor = try {
                android.graphics.Color.parseColor("#$colorHex")
            } catch (e: Exception) {
                0xFFCCCCCC.toInt()
            }
            val contrastColor = getContrastColorInt(colorHex)
            EmojiCoverData(
                isSingleChar = isSingleChar,
                char = if (isSingleChar) emojiContent.first().toString() else null,
                colorHex = colorHex,
                emoji = parts.getOrElse(1) { "📖" },
                title = parts.getOrElse(2) { "" },
                parsedColor = parsedColor,
                contrastColor = contrastColor
            )
        } else null
    }

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
            emojiData != null -> {
                // Emoji封面
                if (emojiData.isSingleChar) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(emojiData.parsedColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emojiData.char ?: "",
                            fontSize = 42.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(emojiData.parsedColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = emojiData.emoji,
                                fontSize = 36.sp
                            )
                            if (emojiData.title.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = emojiData.title,
                                    fontSize = 12.sp,
                                    color = Color(emojiData.contrastColor),
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            coverPath.startsWith("color://") -> {
                // 纯色封面
                val colorHex = coverPath.removePrefix("color://")
                val parsedColor = remember(colorHex) {
                    try {
                        android.graphics.Color.parseColor("#$colorHex")
                    } catch (e: Exception) {
                        0xFFFFFFFF.toInt()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(parsedColor))
                ) {}
            }
            coverPath.startsWith("http") -> {
                // 网络图片
                AsyncImage(
                    model = imageModel,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                // 本地图片URI
                AsyncImage(
                    model = imageModel,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private data class EmojiCoverData(
    val isSingleChar: Boolean,
    val char: String?,
    val colorHex: String,
    val emoji: String,
    val title: String,
    // 预解析的颜色，避免每次 recomposition 都 parseColor
    val parsedColor: Int,
    val contrastColor: Int
)

fun buildBookImageRequest(
    context: android.content.Context,
    imageUrl: String,
    requestedSizePx: Pair<Int, Int>? = null,
    quality: BookCoverQuality = BookCoverQuality.PREVIEW
): ImageRequest {
    val optimizedUrl = optimizeCoverUrl(imageUrl, quality)
    val builder = ImageRequest.Builder(context)
        .data(optimizedUrl)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .crossfade(false)
        .precision(Precision.INEXACT)

    requestedSizePx?.let { (width, height) ->
        if (width > 0 && height > 0) {
            builder.size(width, height)
        }
    }

    if (optimizedUrl.contains("doubanio.com")) {
        builder
            .addHeader("Referer", "https://book.douban.com/")
            .addHeader("User-Agent", "Mozilla/5.0")
            .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
    }

    return builder.build()
}

enum class BookCoverQuality {
    THUMBNAIL,
    PREVIEW,
    FULL
}

private fun optimizeCoverUrl(imageUrl: String, quality: BookCoverQuality): String {
    if (!imageUrl.contains("doubanio.com")) return imageUrl

    return when (quality) {
        BookCoverQuality.THUMBNAIL -> imageUrl.replace("/l/public/", "/m/public/")
        BookCoverQuality.PREVIEW -> imageUrl.replace("/l/public/", "/m/public/")
        BookCoverQuality.FULL -> imageUrl
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
