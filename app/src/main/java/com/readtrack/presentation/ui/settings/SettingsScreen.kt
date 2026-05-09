package com.readtrack.presentation.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.BuildConfig
import com.readtrack.data.local.AutoBackupFrequency
import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.ThemeMode
import com.readtrack.presentation.viewmodel.CookieTestResult
import com.readtrack.presentation.viewmodel.SettingsUiState
import com.readtrack.presentation.viewmodel.SettingsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToBackupSettings: () -> Unit,
    onNavigateToWidgetSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showStatsUnitDialog by remember { mutableStateOf(false) }
    var showWebDavConfigDialog by remember { mutableStateOf(false) }
    var showAutoBackupDialog by remember { mutableStateOf(false) }
    var showUpdateSourceDialog by remember { mutableStateOf(false) }
    var showUpdateResultDialog by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // 先将文件复制到 cache 目录，以便后续 ZIP 流程可以反复读取
            val mimeType = context.contentResolver.getType(it)
            val isZip = mimeType == "application/zip"
            val tempFile = if (isZip) {
                File(context.cacheDir, "import_backup_${System.currentTimeMillis()}.zip").also { f ->
                    context.contentResolver.openInputStream(it)?.use { input -> f.outputStream().use { output -> input.copyTo(output) } }
                }
            } else null

            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
            pendingImportUri = uri
            pendingImportContent = content
            viewModel.prepareImportPreview(content ?: "", zipPath = tempFile?.absolutePath)
        }
    }

    // ZIP 导出：保存到用户指定位置
    val zipExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        // 先读取路径，再清状态（顺序不能反）
        val zipPath = uiState.exportZipPath
        viewModel.clearExportSuccess()
        if (uri != null && zipPath != null) {
            File(zipPath).inputStream().use { input ->
                context.contentResolver.openOutputStream(uri)?.use { output -> input.copyTo(output) }
            }
        }
    }

    // JSON 导出：保存到用户指定位置
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = uiState.exportJson
        viewModel.clearExportSuccess()
        if (uri != null && json != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer -> writer.write(json) }
        }
    }

    // 导出结果准备好后，自动弹出系统文件保存面板
    LaunchedEffect(uiState.exportZipPath, uiState.exportJson) {
        when {
            uiState.exportZipPath != null -> {
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                zipExportLauncher.launch("readtrack_backup_$ts.zip")
            }
            uiState.exportJson != null -> {
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                jsonExportLauncher.launch("readtrack_backup_$ts.json")
            }
        }
    }

    if (uiState.showClearConfirmDialog && (pendingImportContent != null || uiState.pendingZipPath != null)) {
        val importPreview = uiState.importPreview
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissClearConfirmDialog()
                pendingImportUri = null
                pendingImportContent = null
            },
            title = { Text("导入前安全预览") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (importPreview != null) {
                        val backupType = if (uiState.pendingZipPath != null) "（ZIP 含封面）" else "（JSON）"
                        Text("备份时间：${
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(Date(importPreview.exportTime))
                        }，导出自 v${importPreview.appVersion} $backupType")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("即将导入：${importPreview.backupBookCount} 本书、${importPreview.backupRecordCount} 条记录、${importPreview.backupBookListCount} 个书单")
                        Text("追加导入预计新增：${importPreview.appendBookCount} 本书、${importPreview.appendRecordCount} 条记录、${importPreview.appendBookListCount} 个书单")
                        if (importPreview.duplicateBookCount > 0 || importPreview.duplicateRecordCount > 0 || importPreview.duplicateBookListCount > 0) {
                            Text("将跳过重复内容：${importPreview.duplicateBookCount} 本重复书籍、${importPreview.duplicateRecordCount} 条重复记录、${importPreview.duplicateBookListCount} 个重复书单", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (importPreview.skippedOrphanRecordCount > 0) {
                            Text("⚠ 有 ${importPreview.skippedOrphanRecordCount} 条记录因缺少对应书籍而会被跳过", color = MaterialTheme.colorScheme.error)
                        }
                        if (importPreview.willRestorePreferences) {
                            Text("✅ 将同时恢复用户设置（主题、统计单位、WebDAV 配置等）")
                        }
                    }
                    Text("是否清空现有数据后再导入？\n\n• 清空并导入：删除当前书籍、记录和书单后恢复备份\n• 追加导入：保留当前数据，只导入新增内容")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importData(pendingImportContent ?: "", true, uiState.pendingZipPath)
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
                        viewModel.importData(pendingImportContent ?: "", false, uiState.pendingZipPath)
                        pendingImportUri = null
                        pendingImportContent = null
                    }) { Text("追加导入") }
                }
            }
        )
    }

    if (uiState.showWebDavRestoreDialog) {
        if (uiState.isLoadingWebDavBackups) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("加载备份列表...") },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("正在从 WebDAV 获取备份文件列表")
                    }
                },
                confirmButton = { }
            )
        } else if (uiState.webDavBackupFiles.isEmpty()) {
            AlertDialog(
                onDismissRequest = viewModel::dismissWebDavRestoreDialog,
                title = { Text("从 WebDAV 恢复") },
                text = { Text("远端未找到任何备份文件。") },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissWebDavRestoreDialog) {
                        Text("关闭")
                    }
                }
            )
        } else {
            val selectedFile = uiState.selectedWebDavBackupFile
            AlertDialog(
                onDismissRequest = viewModel::dismissWebDavRestoreDialog,
                title = { Text("选择要恢复的备份") },
                text = {
                    Column {
                        Text(
                            "共 ${uiState.webDavBackupFiles.size} 个备份文件，请选择：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.webDavBackupFiles.size) { index ->
                                val file = uiState.webDavBackupFiles[index]
                                val isSelected = file.fileName == selectedFile
                                val dateStr = if (file.lastModified > 0) {
                                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                        .format(Date(file.lastModified))
                                } else { "未知时间" }
                                val sizeStr = if (file.size > 0) {
                                    if (file.size > 1024 * 1024) "%.1f MB".format(file.size / 1024.0 / 1024.0)
                                    else "%.0f KB".format(file.size / 1024.0)
                                } else { "" }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectWebDavBackupFile(file.fileName) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectWebDavBackupFile(file.fileName) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                file.fileName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                "$dateStr${if (sizeStr.isNotBlank()) " · $sizeStr" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (file.fileName == "readtrack_backup_latest.json") {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "最新",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.restoreBackupFromWebDav(true, selectedFile)
                        },
                        enabled = selectedFile != null
                    ) {
                        Text("清空并恢复")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = viewModel::dismissWebDavRestoreDialog) {
                            Text("取消")
                        }
                        TextButton(
                            onClick = {
                                viewModel.restoreBackupFromWebDav(false, selectedFile)
                            },
                            enabled = selectedFile != null
                        ) {
                            Text("追加恢复")
                        }
                    }
                }
            )
        }
    }

    if (uiState.importSuccess && uiState.lastImportResult != null) {
        val result = uiState.lastImportResult!!
        AlertDialog(
            onDismissRequest = viewModel::clearImportSuccess,
            icon = { Icon(Icons.Default.CheckCircle, null) },
            title = { Text("导入成功") },
            text = {
                Column {
                    Text("成功导入：")
                    Text("• 书籍：${result.booksImported} 本")
                    Text("• 阅读记录：${result.recordsImported} 条")
                    if (result.bookListsImported > 0) {
                        Text("• 书单：${result.bookListsImported} 个")
                    }
                    if (result.errors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("警告：", color = MaterialTheme.colorScheme.error)
                        result.errors.take(3).forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::clearImportSuccess) { Text("确定") }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)); SettingsSectionCard("书籍搜索") }
            item { DoubanCookieCard(viewModel, uiState) }

            item { Spacer(Modifier.height(8.dp)); SettingsSectionCard("数据管理") }
            item { SettingsClickableCard(Icons.Outlined.CloudSync, "备份与恢复", "本地备份与 WebDAV 云备份") { onNavigateToBackupSettings() } }

            item { Spacer(Modifier.height(8.dp)); SettingsSectionCard("外观") }
            item {
                SettingsClickableCard(Icons.Outlined.DarkMode, "主题模式", when (uiState.themeMode) {
                    ThemeMode.SYSTEM -> "跟随系统"
                    ThemeMode.LIGHT -> "浅色模式"
                    ThemeMode.DARK -> "深色模式"
                }) { showThemeDialog = true }
            }
            item {
                SettingsClickableCard(Icons.Outlined.Analytics, "统计单位", when (uiState.statsUnit) {
                    StatsUnit.CHAPTER -> "章节数"
                    StatsUnit.PAGE -> "页数"
                }) { showStatsUnitDialog = true }
            }

            item { Spacer(Modifier.height(8.dp)); SettingsSectionCard("桌面小组件") }
            item {
                SettingsClickableCard(
                    Icons.Outlined.Widgets,
                    "桌面小组件",
                    "设置每个桌面小组件对应的书籍"
                ) { onNavigateToWidgetSettings() }
            }

            item { Spacer(Modifier.height(8.dp)); SettingsSectionCard("其他") }
            item {
                SettingsClickableCard(
                    Icons.Outlined.CloudUpload,
                    "标签管理",
                    "创建、删除书籍标签"
                ) { showTagSheet = true }
            }
            item {
                SettingsClickableCard(
                    Icons.Outlined.SettingsEthernet,
                    "更新源",
                    if (uiState.updateSource == "gitee") "Gitee" else "GitHub"
                ) { showUpdateSourceDialog = true }
            }
            item {
                SettingsClickableCard(
                    Icons.Outlined.Download,
                    "检查更新",
                    if (uiState.isCheckingUpdate) "正在检查..." else "当前版本 ${BuildConfig.VERSION_NAME}"
                ) {
                    viewModel.checkForUpdate()
                }
            }
            item { SettingsClickableCard(Icons.Outlined.Info, "关于", "应用信息与隐私声明") { onNavigateToAbout() } }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题模式") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier
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
                            Spacer(Modifier.width(12.dp))
                            Text(when (mode) {
                                ThemeMode.SYSTEM -> "跟随系统"
                                ThemeMode.LIGHT -> "浅色模式"
                                ThemeMode.DARK -> "深色模式"
                            })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("取消") }
            }
        )
    }

    if (showStatsUnitDialog) {
        AlertDialog(
            onDismissRequest = { showStatsUnitDialog = false },
            title = { Text("选择统计单位") },
            text = {
                Column {
                    StatsUnit.entries.forEach { unit ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setStatsUnit(unit)
                                    showStatsUnitDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.statsUnit == unit,
                                onClick = {
                                    viewModel.setStatsUnit(unit)
                                    showStatsUnitDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(when (unit) {
                                StatsUnit.CHAPTER -> "章节数"
                                StatsUnit.PAGE -> "页数"
                            })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsUnitDialog = false }) { Text("取消") }
            }
        )
    }

    if (showWebDavConfigDialog) {
        WebDavConfigDialog(
            uiState = uiState,
            onDismiss = { showWebDavConfigDialog = false },
            onSave = { serverUrl, username, password, remotePath ->
                viewModel.saveWebDavConfig(serverUrl, username, password, remotePath)
                showWebDavConfigDialog = false
            },
            onTest = viewModel::testWebDavConnection
        )
    }

    if (showAutoBackupDialog) {
        AlertDialog(
            onDismissRequest = { showAutoBackupDialog = false },
            title = { Text("自动备份频率") },
            text = {
                Column {
                    AutoBackupFrequency.entries.forEach { frequency ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAutoBackupFrequency(frequency)
                                    showAutoBackupDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.autoBackupFrequency == frequency,
                                onClick = {
                                    viewModel.setAutoBackupFrequency(frequency)
                                    showAutoBackupDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(when (frequency) {
                                AutoBackupFrequency.OFF -> "关闭"
                                AutoBackupFrequency.DAILY -> "每天"
                                AutoBackupFrequency.WEEKLY -> "每周"
                            })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoBackupDialog = false }) { Text("取消") }
            }
        )
    }

    if (showUpdateSourceDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateSourceDialog = false },
            title = { Text("选择更新源") },
            text = {
                Column {
                    listOf("github" to "GitHub", "gitee" to "Gitee").forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setUpdateSource(value)
                                    showUpdateSourceDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.updateSource == value,
                                onClick = {
                                    viewModel.setUpdateSource(value)
                                    showUpdateSourceDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUpdateSourceDialog = false }) { Text("取消") }
            }
        )
    }

    if (showTagSheet) {
        val tagVm: com.readtrack.presentation.viewmodel.TagManagementViewModel = hiltViewModel()
        TagManagementSheet(onDismiss = { showTagSheet = false }, viewModel = tagVm)
    }

    LaunchedEffect(uiState.updateResult) {
        if (uiState.updateResult != null) {
            showUpdateResultDialog = true
        }
    }

    if (showUpdateResultDialog && uiState.updateResult != null) {
        val result = uiState.updateResult!!
        UpdateDialog(
            result = result,
            onDismiss = {
                showUpdateResultDialog = false
                viewModel.clearUpdateResult()
            },
            onOpenDownload = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(result.downloadUrl))
                context.startActivity(intent)
                showUpdateResultDialog = false
                viewModel.clearUpdateResult()
            }
        )
    }
}

@Composable
fun UpdateDialog(
    result: com.readtrack.remote.UpdateResult,
    onDismiss: () -> Unit,
    onOpenDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (result.hasUpdate) "发现新版本" else "已是最新版本")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.hasUpdate) {
                    Text("最新版本：v${result.latestVersion}")
                    Text("当前版本：v${result.currentVersion}")
                    if (result.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "更新内容：",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            result.releaseNotes.take(500),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text("当前已是最新版本 v${result.currentVersion}")
                }
            }
        },
        confirmButton = {
            if (result.hasUpdate) {
                TextButton(onClick = onOpenDownload) {
                    Text("前往下载")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("确定")
                }
            }
        },
        dismissButton = {
            if (result.hasUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("稍后再说")
                }
            }
        }
    )
}

@Composable
private fun LoadingCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(text)
        }
    }
}

@Composable
private fun WebDavStatusCard(uiState: SettingsUiState) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("云备份状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = if (uiState.isWebDavConfigured) "已配置远端目录：${uiState.webDavRemotePath}" else "尚未完成 WebDAV 配置",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "最近成功备份：${uiState.lastWebDavBackupAt?.let(formatter::format) ?: "暂无"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            uiState.webDavStatusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            uiState.lastWebDavError?.takeIf { it.isNotBlank() }?.let { error ->
                Text(
                    text = "最近错误：$error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WebDavConfigDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onTest: (String, String, String, String) -> Unit
) {
    var serverUrl by remember(uiState.webDavServerUrl) { mutableStateOf(uiState.webDavServerUrl) }
    var username by remember(uiState.webDavUsername) { mutableStateOf(uiState.webDavUsername) }
    var password by remember(uiState.webDavPassword) { mutableStateOf(uiState.webDavPassword) }
    var remotePath by remember(uiState.webDavRemotePath) { mutableStateOf(uiState.webDavRemotePath) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置 WebDAV", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "手动备份会上传 latest 文件，并额外保留一个时间戳历史快照。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://dav.example.com/webdav") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "隐藏" else "显示")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = { Text("远程目录") },
                    placeholder = { Text("ReadTrack/backups") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onTest(serverUrl, username, password, remotePath) },
                    enabled = !uiState.isTestingWebDav
                ) {
                    if (uiState.isTestingWebDav) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("测试")
                    }
                }
                TextButton(onClick = { onSave(serverUrl, username, password, remotePath) }) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DoubanCookieCard(
    viewModel: SettingsViewModel,
    uiState: SettingsUiState
) {
    var showCookieDialog by remember { mutableStateOf(false) }
    var cookieInput by remember { mutableStateOf(uiState.doubanCookie) }

    LaunchedEffect(uiState.doubanCookie) {
        cookieInput = uiState.doubanCookie
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showCookieDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Cookie, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("豆瓣 Cookie（可选）", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (uiState.doubanCookie.isNotBlank()) "已配置，可提升兼容性" else "未配置，搜索也可直接使用",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.doubanCookie.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }

    if (showCookieDialog) {
        AlertDialog(
            onDismissRequest = { showCookieDialog = false },
            title = { Text("配置豆瓣 Cookie（可选）", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "现在书籍搜索默认可直接使用。若你有豆瓣登录态，也可以粘贴 Cookie 以提升兼容性。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cookieInput,
                        onValueChange = { cookieInput = it },
                        label = { Text("Cookie") },
                        placeholder = { Text("bid=xxx; dbcl2=xxx...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                    uiState.cookieTestResult?.let { result ->
                        Spacer(Modifier.height(8.dp))
                        val (text, color) = when (result) {
                            CookieTestResult.SUCCESS -> "✓ Cookie有效" to MaterialTheme.colorScheme.primary
                            CookieTestResult.INVALID -> "✗ Cookie无效" to MaterialTheme.colorScheme.error
                            CookieTestResult.NETWORK_ERROR -> "✗ 网络错误" to MaterialTheme.colorScheme.error
                        }
                        Text(text, color = color, style = MaterialTheme.typography.bodySmall)
                    }
                    uiState.errorMessage?.let { error ->
                        Spacer(Modifier.height(4.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { viewModel.testDoubanCookie(cookieInput) },
                        enabled = !uiState.isTestingCookie && cookieInput.isNotBlank()
                    ) {
                        if (uiState.isTestingCookie) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("测试")
                        }
                    }
                    TextButton(onClick = {
                        viewModel.updateDoubanCookie(cookieInput)
                        showCookieDialog = false
                    }) {
                        Text("保存")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCookieDialog = false }) { Text("取消") }
            }
        )
    }

    // 导入导出进度弹窗
    if (uiState.showProgressDialog) {
        AlertDialog(
            onDismissRequest = { /* 进度中不允许关闭 */ },
            confirmButton = {},
            dismissButton = {},
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.progressMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (uiState.progressPercent > 0f) {
                        LinearProgressIndicator(
                            progress = { uiState.progressPercent },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        )
    }

    }
