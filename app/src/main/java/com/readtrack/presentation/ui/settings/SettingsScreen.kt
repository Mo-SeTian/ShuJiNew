package com.readtrack.presentation.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
    
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { 
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
            if (content != null) {
                viewModel.showClearConfirmDialog()
                pendingImportUri = uri
                pendingImportContent = content
            }
        }
    }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { savedUri ->
            uiState.exportJson?.let { json ->
                context.contentResolver.openOutputStream(savedUri)?.bufferedWriter()?.use { writer ->
                    writer.write(json)
                }
            }
            viewModel.clearExportSuccess()
        }
    }
    
    if (uiState.showClearConfirmDialog && pendingImportUri != null && pendingImportContent != null) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.dismissClearConfirmDialog()
                pendingImportUri = null
                pendingImportContent = null
            },
            title = { Text("导入选项") },
            text = { Text("是否清空现有数据后再导入？\n\n• 是：删除所有现有书籍和记录\n• 否：保留现有数据，追加导入") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { uri ->
                        pendingImportContent?.let { content ->
                            viewModel.importData(uri, content, true)
                        }
                    }
                    pendingImportUri = null
                    pendingImportContent = null
                }) { Text("清空并导入") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { 
                        viewModel.dismissClearConfirmDialog()
                        pendingImportUri = null
                        pendingImportContent = null
                    }) { Text("取消") }
                    TextButton(onClick = {
                        pendingImportUri?.let { uri ->
                            pendingImportContent?.let { content ->
                                viewModel.importData(uri, content, false)
                            }
                        }
                        pendingImportUri = null
                        pendingImportContent = null
                    }) { Text("追加导入") }
                }
            }
        )
    }
    
    if (uiState.exportSuccess && uiState.exportJson != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportSuccess() },
            icon = { Icon(Icons.Default.CheckCircle, null) },
            title = { Text("导出成功") },
            text = { Text("数据已准备好，是否保存到文件？") },
            confirmButton = {
                TextButton(onClick = {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    exportLauncher.launch("readtrack_backup_$ts.json")
                }) { Text("保存文件") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExportSuccess() }) { Text("稍后保存") }
            }
        )
    }
    
    if (uiState.importSuccess && uiState.lastImportResult != null) {
        val result = uiState.lastImportResult!!
        AlertDialog(
            onDismissRequest = { viewModel.clearImportSuccess() },
            icon = { Icon(Icons.Default.CheckCircle, null) },
            title = { Text("导入成功") },
            text = {
                Column {
                    Text("成功导入：")
                    Text("• 书籍：${result.booksImported} 本")
                    Text("• 阅读记录：${result.recordsImported} 条")
                    if (result.errors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("警告：", color = MaterialTheme.colorScheme.error)
                        result.errors.take(3).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearImportSuccess() }) { Text("确定") }
            }
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("个性化你的阅读体验", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SettingsSectionCard("数据管理") }
            item { SettingsClickableCard(Icons.Outlined.Upload, "导出数据", "将数据导出为JSON文件") { viewModel.exportData() } }
            item { SettingsClickableCard(Icons.Outlined.Download, "导入数据", "从JSON文件恢复数据") { importLauncher.launch(arrayOf("application/json", "*/*")) } }
            
            if (uiState.isExporting) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("正在导出数据...")
                        }
                    }
                }
            }
            
            if (uiState.isImporting) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("正在导入数据...")
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(8.dp)); SettingsSectionCard("外观") }
            item {
                SettingsClickableCard(Icons.Outlined.DarkMode, "主题模式", when (uiState.themeMode) {
                    ThemeMode.SYSTEM -> "跟随系统"
                    ThemeMode.LIGHT -> "浅色模式"
                    ThemeMode.DARK -> "深色模式"
                }) { showThemeDialog = true }
            }

            item { Spacer(Modifier.height(8.dp)); SettingsSectionCard("关于") }
            item { SettingsClickableCard(Icons.Outlined.Info, "应用信息", "版本 1.0.0") { } }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题模式") },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(mode); showThemeDialog = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = uiState.themeMode == mode, onClick = { viewModel.setThemeMode(mode); showThemeDialog = false })
                            Spacer(Modifier.width(12.dp))
                            Text(when (mode) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "浅色模式"; ThemeMode.DARK -> "深色模式" })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
fun SettingsSectionCard(title: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}