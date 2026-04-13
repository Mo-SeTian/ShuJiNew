package com.readtrack.presentation.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readtrack.data.local.ThemeMode
import com.readtrack.presentation.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // 文件选择器 - 导入
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { 
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
            if (content != null) {
                viewModel.showClearConfirmDialog()
                // 保存URI和内容以便后续使用
                pendingImportUri = uri
                pendingImportContent = content
            }
        }
    }
    
    // 存储待导入的数据
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    
    // 文件保存器 - 导出
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { 
            context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                writer.write(uiState.exportJson ?: "")
            }
            viewModel.clearExportSuccess()
        }
    }
    
    // 确认对话框
    if (uiState.showClearConfirmDialog && pendingImportUri != null && pendingImportContent != null) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.dismissClearConfirmDialog()
                pendingImportUri = null
                pendingImportContent = null
            },
            title = { Text("导入选项") },
            text = { 
                Text("是否清空现有数据后再导入？\n\n• 是：删除所有现有书籍和记录\n• 否：保留现有数据，追加导入的数据") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri?.let { uri ->
                            pendingImportContent?.let { content ->
                                viewModel.importData(uri, content, clearExisting = true)
                            }
                        }
                        pendingImportUri = null
                        pendingImportContent = null
                    }
                ) {
                    Text("清空并导入")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { 
                            viewModel.dismissClearConfirmDialog()
                            pendingImportUri = null
                            pendingImportContent = null
                        }
                    ) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            pendingImportUri?.let { uri ->
                                pendingImportContent?.let { content ->
                                    viewModel.importData(uri, content, clearExisting = false)
                                }
                            }
                            pendingImportUri = null
                            pendingImportContent = null
                        }
                    ) {
                        Text("追加导入")
                    }
                }
            }
        )
    }
    
    // 导入成功对话框
    if (uiState.importSuccess && uiState.lastImportResult != null) {
        val result = uiState.lastImportResult!!
        AlertDialog(
            onDismissRequest = { viewModel.clearImportSuccess() },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            title = { Text("导入成功") },
            text = { 
                Column {
                    Text("成功导入：")
                    Text("• 书籍：${result.booksImported} 本")
                    Text("• 阅读记录：${result.recordsImported} 条")
                    if (result.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("警告：", color = MaterialTheme.colorScheme.error)
                        result.errors.take(3).forEach { error ->
                            Text(error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearImportSuccess() }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 错误提示
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            // 显示 Snackbar 或 Toast
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "设置",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "个性化你的阅读体验",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 数据管理 Section
            item {
                SettingsSectionCard(title = "数据管理")
            }
            
            item {
                SettingsClickableCard(
                    icon = Icons.Outlined.Upload,
                    title = "导出数据",
                    subtitle = "将所有书籍和阅读记录导出为JSON文件",
                    onClick = { viewModel.exportData() }
                )
            }
            
            item {
                SettingsClickableCard(
                    icon = Icons.Outlined.Download,
                    title = "导入数据",
                    subtitle = "从JSON文件恢复书籍和阅读记录",
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                )
            }
            
            // 导出进度
            if (uiState.isExporting) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("正在导出数据...")
                        }
                    }
                }
            }
            
            // 导入进度
            if (uiState.isImporting) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("正在导入数据...")
                        }
                    }
                }
            }
            
            // 导出成功后自动触发保存
            LaunchedEffect(uiState.exportSuccess) {
                if (uiState.exportSuccess && uiState.exportJson != null) {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    exportLauncher.launch("readtrack_backup_$timestamp.json")
                }
            }
            
            // Appearance Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionCard(title = "外观")
            }
            
            item {
                SettingsClickableCard(
                    icon = Icons.Outlined.DarkMode,
                    title = "主题模式",
                    subtitle = when (uiState.themeMode) {
                        ThemeMode.SYSTEM -> "跟随系统"
                        ThemeMode.LIGHT -> "浅色模式"
                        ThemeMode.DARK -> "深色模式"
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionCard(title = "关于")
            }

            item {
                SettingsClickableCard(
                    icon = Icons.Outlined.Info,
                    title = "应用信息",
                    subtitle = "版本 1.0.0",
                    onClick = { }
                )
            }
        }
    }

    // 主题选择对话框
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题模式") },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "跟随系统"
                                    ThemeMode.LIGHT -> "浅色模式"
                                    ThemeMode.DARK -> "深色模式"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SettingsClickableCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
