package com.readtrack.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.AutoBackupFrequency
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.ThemeMode
import com.readtrack.data.remote.WebDavConfig
import com.readtrack.data.remote.WebDavService
import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.model.ImportPreview
import com.readtrack.domain.model.ImportResult
import com.readtrack.domain.repository.DataBackupRepository
import com.readtrack.data.repository.DataBackupRepositoryImpl
import com.readtrack.util.CoverStorageUtil
import com.readtrack.worker.WebDavBackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val statsUnit: StatsUnit = StatsUnit.CHAPTER,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isTestingWebDav: Boolean = false,
    val isSyncingWebDav: Boolean = false,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val lastImportResult: ImportResult? = null,
    val errorMessage: String? = null,
    val exportJson: String? = null,
    val /** 导出时生成的 ZIP 文件路径（用于保存到用户选择的路径）*/
    exportZipPath: String? = null,
    val showClearConfirmDialog: Boolean = false,
    val importPreview: ImportPreview? = null,
    val /** 导入时解析出的 ZIP 文件路径（用于后续 importFromZip）*/
    pendingZipPath: String? = null,
    val showWebDavRestoreDialog: Boolean = false,
    val doubanCookie: String = "",
    val isTestingCookie: Boolean = false,
    val cookieTestResult: CookieTestResult? = null,
    val webDavServerUrl: String = "",
    val webDavUsername: String = "",
    val webDavPassword: String = "",
    val webDavRemotePath: String = "ReadTrack",
    val autoBackupFrequency: AutoBackupFrequency = AutoBackupFrequency.OFF,
    val lastWebDavBackupAt: Long? = null,
    val lastWebDavError: String? = null,
    val webDavStatusMessage: String? = null,
    val webDavBackupFiles: List<WebDavService.BackupFileInfo> = emptyList(),
    val isLoadingWebDavBackups: Boolean = false,
    val selectedWebDavBackupFile: String? = null,
    // 导入导出进度
    val showProgressDialog: Boolean = false,
    val progressMessage: String = "",
    val progressPercent: Float = 0f
) {
    val isWebDavConfigured: Boolean
        get() =
            webDavServerUrl.isNotBlank() &&
                webDavUsername.isNotBlank() &&
                webDavPassword.isNotBlank() &&
                webDavRemotePath.isNotBlank()
}

enum class CookieTestResult {
    SUCCESS,
    INVALID,
    NETWORK_ERROR
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val dataBackupRepository: DataBackupRepository,
    private val preferencesManager: PreferencesManager,
    private val okHttpClient: OkHttpClient,
    private val webDavService: WebDavService,
    private val webDavBackupScheduler: WebDavBackupScheduler,
    private val coverStorageUtil: CoverStorageUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeBaseSettings()
        observeWebDavSettings()
    }

    private fun observeBaseSettings() {
        viewModelScope.launch {
            combine(
                preferencesManager.themeMode,
                preferencesManager.statsUnit,
                preferencesManager.doubanCookie
            ) { themeMode, statsUnit, cookie ->
                Triple(themeMode, statsUnit, cookie)
            }.collect { (themeMode, statsUnit, cookie) ->
                _uiState.update {
                    it.copy(
                        themeMode = themeMode,
                        statsUnit = statsUnit,
                        doubanCookie = cookie
                    )
                }
            }
        }
    }

    private fun observeWebDavSettings() {
        viewModelScope.launch {
            combine(
                preferencesManager.webDavServerUrl,
                preferencesManager.webDavUsername,
                preferencesManager.webDavPassword,
                preferencesManager.webDavRemotePath
            ) { serverUrl, username, password, remotePath ->
                WebDavConfig(
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    remotePath = remotePath
                )
            }.collect { config ->
                _uiState.update {
                    it.copy(
                        webDavServerUrl = config.serverUrl,
                        webDavUsername = config.username,
                        webDavPassword = config.password,
                        webDavRemotePath = config.remotePath
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                preferencesManager.autoBackupFrequency,
                preferencesManager.lastWebDavBackupAt,
                preferencesManager.lastWebDavError
            ) { frequency, lastBackupAt, lastError ->
                Triple(frequency, lastBackupAt, lastError)
            }.collect { (frequency, lastBackupAt, lastError) ->
                _uiState.update {
                    it.copy(
                        autoBackupFrequency = frequency,
                        lastWebDavBackupAt = lastBackupAt,
                        lastWebDavError = lastError
                    )
                }
            }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(themeMode)
        }
    }

    fun setStatsUnit(unit: StatsUnit) {
        viewModelScope.launch {
            preferencesManager.setStatsUnit(unit)
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    errorMessage = null,
                    showProgressDialog = true,
                    progressMessage = "正在准备导出...",
                    progressPercent = 0f
                )
            }
            (dataBackupRepository as? DataBackupRepositoryImpl)?.exportToZip()
                ?.onSuccess { zipFile ->
                    // 不在这里关闭进度弹窗，等 exportLauncher 回调用户选择保存位置后再关闭
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportSuccess = true,
                            exportZipPath = zipFile.absolutePath,
                            exportJson = null,
                            progressMessage = "正在等待选择保存位置...",
                            progressPercent = 0f
                        )
                    }
                }
                ?.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = "导出失败: ${error.message}",
                            showProgressDialog = false,
                            progressMessage = ""
                        )
                    }
                }
                ?: run {
                    // 回退：纯 JSON 导出（不应发生）
                    dataBackupRepository.exportAllData()
                        .onSuccess { backup ->
                            val json = Json.encodeToString(DataBackup.serializer(), backup)
                            // JSON 导出同样等文件保存后再关闭弹窗
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    exportSuccess = true,
                                    exportJson = json,
                                    exportZipPath = null,
                                    progressMessage = "正在等待选择保存位置...",
                                    progressPercent = 0f
                                )
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    errorMessage = "导出失败: ${error.message}",
                                    showProgressDialog = false,
                                    progressMessage = ""
                                )
                            }
                        }
                }
        }
    }

    /**
     * 准备导入预览：自动判断是 JSON 还是 ZIP
     * - JSON 文件：直接预览
     * - ZIP 文件：解压后预览 data.json，并将 ZIP 路径存入 pendingZipPath
     */
    fun prepareImportPreview(content: String, zipPath: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    errorMessage = null,
                    showClearConfirmDialog = false,
                    importPreview = null,
                    pendingZipPath = null,
                    showProgressDialog = true,
                    progressMessage = "正在读取文件...",
                    progressPercent = 0f
                )
            }

            // 尝试解析为纯 JSON
            val backup = dataBackupRepository.parseBackupFromJson(content)
            if (backup != null) {
                _uiState.update {
                    it.copy(
                        progressMessage = "正在分析备份内容...",
                        progressPercent = 0.5f
                    )
                }
                dataBackupRepository.previewImport(backup)
                    .onSuccess { preview ->
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                showClearConfirmDialog = true,
                                importPreview = preview,
                                showProgressDialog = false,
                                progressMessage = "",
                                progressPercent = 1f
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                errorMessage = "导入预览失败: ${error.message}",
                                importPreview = null,
                                showProgressDialog = false,
                                progressMessage = ""
                            )
                        }
                    }
                return@launch
            }

            // 尝试解析为 ZIP
            if (zipPath != null) {
                val zipFile = File(zipPath)
                if (zipFile.exists()) {
                    _uiState.update {
                        it.copy(
                            progressMessage = "正在读取 ZIP 文件...",
                            progressPercent = 0.3f
                        )
                    }
                    (dataBackupRepository as? DataBackupRepositoryImpl)?.importFromZipForPreview(zipFile)
                        ?.onSuccess { preview ->
                            _uiState.update {
                                it.copy(
                                    isImporting = false,
                                    showClearConfirmDialog = true,
                                    importPreview = preview,
                                    pendingZipPath = zipPath,
                                    showProgressDialog = false,
                                    progressMessage = "",
                                    progressPercent = 1f
                                )
                            }
                        }
                        ?.onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isImporting = false,
                                    errorMessage = "读取 ZIP 失败: ${error.message}",
                                    importPreview = null,
                                    showProgressDialog = false,
                                    progressMessage = ""
                                )
                            }
                        }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    isImporting = false,
                    errorMessage = "文件格式无效，请选择有效的备份文件",
                    importPreview = null,
                    showProgressDialog = false,
                    progressMessage = ""
                )
            }
        }
    }

    /**
     * 执行导入：自动根据 pendingZipPath 判断走 ZIP 还是 JSON 路径
     */
    fun importData(content: String, clearExisting: Boolean, zipPath: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    errorMessage = null,
                    showClearConfirmDialog = false,
                    showProgressDialog = true,
                    progressMessage = "正在导入数据...",
                    progressPercent = 0f
                )
            }

            // ZIP 路径存在 → 从 ZIP 导入
            val resolvedZipPath = zipPath ?: _uiState.value.pendingZipPath
            if (resolvedZipPath != null) {
                val zipFile = File(resolvedZipPath)
                if (zipFile.exists()) {
                    _uiState.update {
                        it.copy(
                            progressMessage = "正在解压 ZIP 并恢复封面...",
                            progressPercent = 0.2f
                        )
                    }
                    (dataBackupRepository as? DataBackupRepositoryImpl)?.importFromZip(zipFile, clearExisting)
                        ?.onSuccess { result ->
                            cleanupTempZip(resolvedZipPath)
                            _uiState.update {
                                it.copy(
                                    isImporting = false,
                                    importSuccess = true,
                                    lastImportResult = result,
                                    importPreview = null,
                                    pendingZipPath = null,
                                    showProgressDialog = false,
                                    progressMessage = "",
                                    progressPercent = 1f
                                )
                            }
                            return@launch
                        }
                        ?.onFailure { error ->
                            cleanupTempZip(resolvedZipPath)
                            _uiState.update {
                                it.copy(
                                    isImporting = false,
                                    errorMessage = "导入失败: ${error.message}",
                                    showProgressDialog = false,
                                    progressMessage = ""
                                )
                            }
                            return@launch
                        }
                }
            }

            // JSON 导入
            _uiState.update {
                it.copy(
                    progressMessage = "正在解析备份数据...",
                    progressPercent = 0.2f
                )
            }
            val backup = dataBackupRepository.parseBackupFromJson(content)
            if (backup == null) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "文件格式无效",
                        importPreview = null,
                        showProgressDialog = false,
                        progressMessage = ""
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    progressMessage = "正在恢复数据...",
                    progressPercent = 0.5f
                )
            }
            dataBackupRepository.importData(backup, clearExisting)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importSuccess = true,
                            lastImportResult = result,
                            importPreview = null,
                            showProgressDialog = false,
                            progressMessage = "",
                            progressPercent = 1f
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = "导入失败: ${error.message}",
                            showProgressDialog = false,
                            progressMessage = ""
                        )
                    }
                }
        }
    }

    fun saveWebDavConfig(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String
    ) {
        viewModelScope.launch {
            preferencesManager.setWebDavConfig(serverUrl, username, password, remotePath)
            _uiState.update {
                it.copy(
                    webDavStatusMessage = "WebDAV 配置已保存",
                    errorMessage = null
                )
            }
            if (_uiState.value.autoBackupFrequency != AutoBackupFrequency.OFF) {
                webDavBackupScheduler.updateSchedule(_uiState.value.autoBackupFrequency)
            }
        }
    }

    fun testWebDavConnection(
        serverUrl: String = _uiState.value.webDavServerUrl,
        username: String = _uiState.value.webDavUsername,
        password: String = _uiState.value.webDavPassword,
        remotePath: String = _uiState.value.webDavRemotePath
    ) {
        val config = buildWebDavConfig(serverUrl, username, password, remotePath)
        if (!config.isValid()) {
            _uiState.update { it.copy(errorMessage = "请先填写完整的 WebDAV 地址、用户名、密码和远程目录") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingWebDav = true, errorMessage = null, webDavStatusMessage = null) }
            webDavService.testConnection(config)
                .onSuccess {
                    preferencesManager.setWebDavConfig(serverUrl, username, password, remotePath)
                    preferencesManager.setLastWebDavError(null)
                    _uiState.update {
                        it.copy(
                            isTestingWebDav = false,
                            webDavStatusMessage = "连接成功，远程目录已就绪"
                        )
                    }
                }
                .onFailure { error ->
                    preferencesManager.setLastWebDavError(error.message)
                    _uiState.update {
                        it.copy(
                            isTestingWebDav = false,
                            errorMessage = "WebDAV 连接失败: ${error.message}"
                        )
                    }
                }
        }
    }

    fun uploadBackupToWebDav() {
        val config = currentWebDavConfig()
        if (!config.isValid()) {
            _uiState.update { it.copy(errorMessage = "请先完成 WebDAV 配置") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingWebDav = true, errorMessage = null, webDavStatusMessage = null) }
            // 1. 生成 ZIP 备份
            (dataBackupRepository as? DataBackupRepositoryImpl)?.exportToZip()
                ?.mapCatching { zipFile -> zipFile.readBytes() }
                ?.fold(
                    onSuccess = { zipData ->
                        // 2. 上传到 WebDAV
                        webDavService.uploadBackupZip(config, zipData)
                            .onSuccess {
                                val now = System.currentTimeMillis()
                                preferencesManager.setLastWebDavBackupAt(now)
                                preferencesManager.setLastWebDavError(null)
                                _uiState.update {
                                    it.copy(
                                        isSyncingWebDav = false,
                                        webDavStatusMessage = "已上传 ZIP 备份到 WebDAV（含封面 + 设置）"
                                    )
                                }
                            }
                            .onFailure { error ->
                                preferencesManager.setLastWebDavError(error.message)
                                _uiState.update {
                                    it.copy(
                                        isSyncingWebDav = false,
                                        errorMessage = "上传失败: ${error.message}"
                                    )
                                }
                            }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSyncingWebDav = false,
                                errorMessage = "导出失败: ${error.message}"
                            )
                        }
                    }
                )
                ?: run {
                    // 如果 DataBackupRepositoryImpl 不可用，退回到 JSON 上传
                    uploadBackupJsonToWebDav(config)
                }
        }
    }

    /**
     * 回退：纯 JSON 上传到 WebDAV（DataBackupRepositoryImpl 不可用时）
     */
    private suspend fun uploadBackupJsonToWebDav(config: WebDavConfig) {
        dataBackupRepository.exportAllData()
            .mapCatching { backup -> Json.encodeToString(DataBackup.serializer(), backup) }
            .fold(
                onSuccess = { json ->
                    webDavService.uploadBackup(config, json)
                        .onSuccess {
                            val now = System.currentTimeMillis()
                            preferencesManager.setLastWebDavBackupAt(now)
                            preferencesManager.setLastWebDavError(null)
                            _uiState.update {
                                it.copy(
                                    isSyncingWebDav = false,
                                    webDavStatusMessage = "已上传到 WebDAV（纯 JSON，不含封面）"
                                )
                            }
                        }
                        .onFailure { error ->
                            preferencesManager.setLastWebDavError(error.message)
                            _uiState.update {
                                it.copy(
                                    isSyncingWebDav = false,
                                    errorMessage = "上传失败: ${error.message}"
                                )
                            }
                        }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSyncingWebDav = false,
                            errorMessage = "导出失败: ${error.message}"
                        )
                    }
                }
            )
    }

    fun showWebDavRestoreDialog() {
        val config = currentWebDavConfig()
        if (!config.isValid()) {
            _uiState.update { it.copy(errorMessage = "请先完成 WebDAV 配置") }
            return
        }
        _uiState.update {
            it.copy(
                showWebDavRestoreDialog = true,
                isLoadingWebDavBackups = true,
                webDavBackupFiles = emptyList(),
                selectedWebDavBackupFile = null,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            webDavService.listBackups(config)
                .onSuccess { files ->
                    _uiState.update {
                        it.copy(
                            isLoadingWebDavBackups = false,
                            webDavBackupFiles = files
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingWebDavBackups = false,
                            errorMessage = "加载备份列表失败: ${error.message}"
                        )
                    }
                }
        }
    }

    fun dismissWebDavRestoreDialog() {
        _uiState.update {
            it.copy(
                showWebDavRestoreDialog = false,
                webDavBackupFiles = emptyList(),
                selectedWebDavBackupFile = null,
                isLoadingWebDavBackups = false
            )
        }
    }

    fun selectWebDavBackupFile(fileName: String) {
        _uiState.update { it.copy(selectedWebDavBackupFile = fileName) }
    }

    fun restoreBackupFromWebDav(clearExisting: Boolean, fileName: String? = null) {
        val config = currentWebDavConfig()
        if (!config.isValid()) {
            _uiState.update { it.copy(errorMessage = "请先完成 WebDAV 配置") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncingWebDav = true,
                    errorMessage = null,
                    showWebDavRestoreDialog = false,
                    webDavStatusMessage = null
                )
            }
            // 先尝试 ZIP 恢复
            val isZipFile = fileName == null || fileName.endsWith(".zip")
            if (isZipFile && dataBackupRepository is DataBackupRepositoryImpl) {
                webDavService.downloadBackupZip(config, fileName)
                    .mapCatching { zipData ->
                        val tempFile = File(applicationContext.cacheDir, "webdav_restore_${System.currentTimeMillis()}.zip")
                        tempFile.writeBytes(zipData)
                        // 预览 ZIP 内容
                        val preview = (dataBackupRepository as DataBackupRepositoryImpl).importFromZipForPreview(tempFile)
                        preview to tempFile
                    }
                    .fold(
                        onSuccess = { (previewResult, tempFile) ->
                            previewResult
                                .onSuccess { preview ->
                                    _uiState.update {
                                        it.copy(
                                            isSyncingWebDav = false,
                                            showClearConfirmDialog = true,
                                            importPreview = preview,
                                            pendingZipPath = tempFile.absolutePath
                                        )
                                    }
                                }
                                .onFailure { error ->
                                    tempFile.delete()
                                    // 预览失败，降级到 JSON
                                    restoreBackupJsonPreviewFromWebDav(config, clearExisting, fileName)
                                }
                        },
                        onFailure = { _ ->
                            // ZIP 下载失败，降级到 JSON
                            restoreBackupJsonPreviewFromWebDav(config, clearExisting, fileName)
                        }
                    )
            } else {
                // 文件名以 .json 结尾或 DataBackupRepositoryImpl 不可用 → JSON 预览
                restoreBackupJsonPreviewFromWebDav(config, clearExisting, fileName)
            }
        }
    }

    /**
     * 回退：从 WebDAV 下载 JSON 备份并预览
     */
    private suspend fun restoreBackupJsonPreviewFromWebDav(config: WebDavConfig, clearExisting: Boolean, fileName: String?) {
        webDavService.downloadBackup(config, fileName)
            .mapCatching { json ->
                dataBackupRepository.parseBackupFromJson(json)
                    ?: throw IllegalStateException("远端备份格式无效")
            }
            .fold(
                onSuccess = { backup ->
                    dataBackupRepository.previewImport(backup)
                        .onSuccess { preview ->
                            _uiState.update {
                                it.copy(
                                    isSyncingWebDav = false,
                                    showClearConfirmDialog = true,
                                    importPreview = preview,
                                    pendingZipPath = null
                                )
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isSyncingWebDav = false,
                                    errorMessage = "恢复预览失败: ${error.message}"
                                )
                            }
                        }
                },
                onFailure = { error ->
                    preferencesManager.setLastWebDavError(error.message)
                    _uiState.update {
                        it.copy(
                            isSyncingWebDav = false,
                            errorMessage = "下载失败: ${error.message}"
                        )
                    }
                }
            )
    }

    fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        val config = currentWebDavConfig()
        if (frequency != AutoBackupFrequency.OFF && !config.isValid()) {
            _uiState.update { it.copy(errorMessage = "请先完成 WebDAV 配置，再开启自动备份") }
            return
        }

        viewModelScope.launch {
            preferencesManager.setAutoBackupFrequency(frequency)
            webDavBackupScheduler.updateSchedule(frequency)
            _uiState.update {
                it.copy(
                    webDavStatusMessage = when (frequency) {
                        AutoBackupFrequency.OFF -> "已关闭自动备份"
                        AutoBackupFrequency.DAILY -> "已开启每日自动备份"
                        AutoBackupFrequency.WEEKLY -> "已开启每周自动备份"
                    },
                    errorMessage = null
                )
            }
        }
    }

    fun showClearConfirmDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = true, importPreview = null) }
    }

    fun dismissClearConfirmDialog() {
        // 清理 WebDAV 临时 ZIP 文件
        val zipPath = _uiState.value.pendingZipPath
        if (zipPath != null && zipPath.startsWith(applicationContext.cacheDir.absolutePath)) {
            File(zipPath).delete()
        }
        _uiState.update { it.copy(showClearConfirmDialog = false, importPreview = null, pendingZipPath = null) }
    }

    fun clearExportSuccess() {
        _uiState.update {
            it.copy(
                exportSuccess = false,
                exportZipPath = null,
                exportJson = null,
                showProgressDialog = false,
                progressMessage = ""
            )
        }
    }

    fun clearImportSuccess() {
        val zipPath = _uiState.value.pendingZipPath
        if (zipPath != null) cleanupTempZip(zipPath)
        _uiState.update { it.copy(importSuccess = false, lastImportResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearWebDavStatusMessage() {
        _uiState.update { it.copy(webDavStatusMessage = null) }
    }

    fun updateDoubanCookie(cookie: String) {
        viewModelScope.launch {
            preferencesManager.setDoubanCookie(cookie)
            _uiState.update { it.copy(doubanCookie = cookie, cookieTestResult = null) }
        }
    }

    fun testDoubanCookie(cookieToTest: String = _uiState.value.doubanCookie) {
        val cookie = cookieToTest.trim()
        if (cookie.isBlank()) {
            _uiState.update { it.copy(errorMessage = "可先留空；当前搜索功能已可直接使用") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingCookie = true, cookieTestResult = null, errorMessage = null) }
            preferencesManager.setDoubanCookie(cookie)
            _uiState.update { it.copy(doubanCookie = cookie) }

            try {
                val url = "https://search.douban.com/book/subject_search?search_text=test&cat=1001"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://book.douban.com/")
                    .header("Cookie", cookie)
                    .build()
                val response = okHttpClient.newCall(request).execute()
                val responseCode = response.code
                val responseBody = response.body?.string().orEmpty()

                if (responseBody.contains("window.__DATA__")) {
                    _uiState.update { it.copy(isTestingCookie = false, cookieTestResult = CookieTestResult.SUCCESS) }
                } else if (responseCode == 401 || responseCode == 403) {
                    _uiState.update {
                        it.copy(
                            isTestingCookie = false,
                            cookieTestResult = CookieTestResult.INVALID,
                            errorMessage = "Cookie无效或已过期 (HTTP $responseCode)"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isTestingCookie = false,
                            cookieTestResult = CookieTestResult.INVALID,
                            errorMessage = "未能验证 Cookie，请检查格式或稍后重试"
                        )
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                _uiState.update {
                    it.copy(
                        isTestingCookie = false,
                        cookieTestResult = CookieTestResult.NETWORK_ERROR,
                        errorMessage = "连接超时，请检查网络"
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                _uiState.update {
                    it.copy(
                        isTestingCookie = false,
                        cookieTestResult = CookieTestResult.NETWORK_ERROR,
                        errorMessage = "无法连接豆瓣，请检查网络"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTestingCookie = false,
                        cookieTestResult = CookieTestResult.NETWORK_ERROR,
                        errorMessage = "网络错误: ${e.message ?: "未知错误"}"
                    )
                }
            }
        }
    }

    fun clearCookieTestResult() {
        _uiState.update { it.copy(cookieTestResult = null) }
    }

    private fun currentWebDavConfig(): WebDavConfig {
        return buildWebDavConfig(
            _uiState.value.webDavServerUrl,
            _uiState.value.webDavUsername,
            _uiState.value.webDavPassword,
            _uiState.value.webDavRemotePath
        )
    }

    private fun buildWebDavConfig(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String
    ): WebDavConfig {
        return WebDavConfig(
            serverUrl = serverUrl.trim(),
            username = username.trim(),
            password = password,
            remotePath = remotePath.trim().trim('/').ifBlank { "ReadTrack" }
        )
    }

    /**
     * 清理 cache 目录下的临时 ZIP 文件（WebDAV 下载的）
     */
    private fun cleanupTempZip(path: String) {
        if (path.startsWith(applicationContext.cacheDir.absolutePath)) {
            File(path).delete()
        }
    }

    fun dismissProgressDialog() {
        _uiState.update { it.copy(showProgressDialog = false, progressMessage = "", progressPercent = 0f) }
    }
}
