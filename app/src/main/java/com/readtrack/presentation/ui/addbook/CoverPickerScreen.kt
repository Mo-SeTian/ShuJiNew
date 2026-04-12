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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPickerScreen(
    initialCoverPath: String? = null,
    onCoverSelected: (String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedCover by remember { mutableStateOf(initialCoverPath) }
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
                    TextButton(
                        onClick = {
                            if (selectedCover != null) {
                                onCoverSelected(selectedCover)
                            }
                        },
                        enabled = selectedCover != null
                    ) {
                        Text("完成", fontWeight = FontWeight.Bold)
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
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CoverOptionCard(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Image, "相册", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("相册", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            item {
                CoverOptionCard(
                    onClick = { showUrlDialog = true },
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Link, "URL", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("网络图片", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("分类封面", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            
            items(DefaultCovers.covers) { cover ->
                val isSelected = selectedCover == cover.url
                CoverOptionCard(
                    onClick = { selectedCover = cover.url },
                    backgroundColor = Color(android.graphics.Color.parseColor("#${cover.colorHex}")),
                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Text(cover.emoji, fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(cover.title, style = MaterialTheme.typography.labelSmall, color = getContrastColor(cover.colorHex), fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("纯色封面", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            
            items(DefaultCovers.solidColors) { colorHex ->
                val isSelected = selectedCover == "color://$colorHex"
                Box(
                    modifier = Modifier.aspectRatio(0.75f).clip(RoundedCornerShape(12.dp)).background(Color(android.graphics.Color.parseColor("#$colorHex"))).border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ).clickable { selectedCover = "color://$colorHex" },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, "已选择", tint = getContrastColor(colorHex), modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
    
    if (showUrlDialog) {
        val focusManager = LocalFocusManager.current
        
        AlertDialog(
            onDismissRequest = { showUrlDialog = false; urlInput = ""; urlError = null },
            title = { Text("输入图片地址", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("请输入图片的网络链接地址", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it; urlError = null },
                        label = { Text("图片URL") },
                        placeholder = { Text("https://example.com/image.jpg") },
                        singleLine = true,
                        isError = urlError != null,
                        supportingText = urlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (urlInput.isNotBlank() && (urlInput.startsWith("http://") || urlInput.startsWith("https://"))) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(8.dp)) {
                            AsyncImage(model = urlInput, contentDescription = "图片预览", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        urlInput.isBlank() -> urlError = "请输入图片地址"
                        !urlInput.startsWith("http://") && !urlInput.startsWith("https://") -> urlError = "请输入以 http:// 或 https:// 开头的地址"
                        else -> { selectedCover = urlInput; showUrlDialog = false; urlInput = ""; urlError = null }
                    }
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showUrlDialog = false; urlInput = ""; urlError = null }) { Text("取消") } }
        )
    }
}

@Composable
fun CoverOptionCard(
    onClick: () -> Unit,
    backgroundColor: Color,
    borderColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.aspectRatio(0.75f).clickable(onClick = onClick).border(
            width = 2.dp,
            color = borderColor,
            shape = RoundedCornerShape(12.dp)
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}
