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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.readtrack.presentation.ui.components.getContrastColor
import com.readtrack.presentation.viewmodel.CoverSelectionHolder

data class BuiltInCover(val name: String, val url: String, val color: String)

val builtInCovers = listOf(
    BuiltInCover("技术", "emoji://4A90D9|💻|技术", "4A90D9"),
    BuiltInCover("文学", "emoji://8B4513|📚|文学", "8B4513"),
    BuiltInCover("历史", "emoji://DAA520|📜|历史", "DAA520"),
    BuiltInCover("科幻", "emoji://4B0082|🚀|科幻", "4B0082"),
    BuiltInCover("经济", "emoji://228B22|💰|经济", "228B22"),
    BuiltInCover("心理", "emoji://9932CC|🧠|心理", "9932CC"),
    BuiltInCover("哲学", "emoji://2F4F4F|🤔|哲学", "2F4F4F"),
    BuiltInCover("艺术", "emoji://FF6347|🎨|艺术", "FF6347"),
    BuiltInCover("小说", "emoji://DC143C|📖|小说", "DC143C")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPickerScreen(
    initialCoverUri: String?,
    onCoverSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedCover by remember { mutableStateOf(initialCoverUri) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }

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
                    if (selectedCover != null) {
                        TextButton(onClick = onNavigateBack) {
                            Text("完成", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                selectedCover = it.toString()
                onCoverSelected(it.toString())
                // 注意：回调已经包含导航返回，不要再调用 onNavigateBack()
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
            // 本地图片
            item {
                CoverOptionCard(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "本地图片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 网络图片
            item {
                CoverOptionCard(
                    onClick = { showUrlDialog = true },
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "网络图片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 内置封面
            items(builtInCovers) { cover ->
                BuiltInCoverCard(
                    cover = cover,
                    isSelected = selectedCover == cover.url,
                    onClick = {
                        selectedCover = cover.url
                        onCoverSelected(cover.url)
                        // 注意：回调已经包含导航返回
                    }
                )
            }
        }
    }

    // URL输入对话框
    if (showUrlDialog) {
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("输入图片地址", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "请输入图片的网络链接地址",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it; urlError = null },
                        label = { Text("图片URL") },
                        placeholder = { Text("https://example.com/image.jpg") },
                        singleLine = true,
                        isError = urlError != null,
                        supportingText = urlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (urlInput.isNotBlank() && (urlInput.startsWith("http://") || urlInput.startsWith("https://"))) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            AsyncImage(
                                model = urlInput,
                                contentDescription = "图片预览",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            urlInput.isBlank() -> urlError = "请输入图片地址"
                            !urlInput.startsWith("http://") && !urlInput.startsWith("https://") -> urlError = "请输入以 http:// 或 https:// 开头的地址"
                            else -> {
                                selectedCover = urlInput
                                onCoverSelected(urlInput)
                                // 注意：回调已经包含导航返回，关闭对话框即可
                                showUrlDialog = false
                                urlInput = ""
                                urlError = null
                            }
                        }
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false; urlInput = ""; urlError = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CoverOptionCard(
    onClick: () -> Unit,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun BuiltInCoverCard(
    cover: BuiltInCover,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = Color(android.graphics.Color.parseColor("#${cover.color}"))
    // 从URL中解析emoji: emoji://COLOR|EMOJI|TITLE
    val emoji = cover.url.split("|").getOrNull(1) ?: "📖"
    val title = cover.name
    val textColor = getContrastColor(cover.color)
    
    Card(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(width = 3.dp) else null
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = textColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                )
            }
        }
    }
}