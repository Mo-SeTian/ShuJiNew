package com.readtrack.presentation.viewmodel

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
import com.readtrack.worker.WebDavBackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
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
    val showClearConfirmDialog: Boolean = false,
    val importPreview: ImportPreview? = null,
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
    val webDavStatusMessage: String? = null
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
    private val dataBackupRepository: DataBackupRepository,
    private val preferencesManager: PreferencesManager,
    private val okHttpClient: OkHttpClient,
    private val webDavService: WebDavService,
    private val webDavBackupScheduler: WebDavBackupScheduler
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
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            dataBackupRepository.exportAllData()
                .onSuccess { backup ->
                    val json = Json.encodeToString(DataBackup.serializer(), backup)
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportSuccess = true,
                            exportJson = json
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = "导出失败: ${error.message}"
                        )
                    }
                }
        }
    }

    fun prepareImportPreview(jsonContent: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    errorMessage = null,
                    showClearConfirmDialog = false,
                    importPreview = null
                )
            }
            val backup = dataBackupRepository.parseBackupFromJson(jsonContent)
            if (backup == null) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "文件格式无效",
                        importPreview = null
                    )
                }
                return@launch
            }

            dataBackupRepository.previewImport(backup)
                .onSuccess { preview ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            showClearConfirmDialog = true,
                            importPreview = preview
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = "导入预览失败: ${error.message}",
                            importPreview = null
                        )
                    }
                }
        }
    }

    fun importData(jsonContent: String, clearExisting: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    errorMessage = null,
                    showClearConfirmDialog = false
                )
            }
            val backup = dataBackupRepository.parseBackupFromJson(jsonContent)
            if (backup == null) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "文件格式无效",
                        importPreview = null
                    )
                }
                return@launch
            }

            dataBackupRepository.importData(backup, clearExisting)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importSuccess = true,
                            lastImportResult = result,
                            importPreview = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = "导入失败: ${error.message}"
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
                                        webDavStatusMessage = "已上传到 WebDAV，并保留 latest + 历史快照"
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
    }

    fun showWebDavRestoreDialog() {
        _uiState.update { it.copy(showWebDavRestoreDialog = true) }
    }

    fun dismissWebDavRestoreDialog() {
        _uiState.update { it.copy(showWebDavRestoreDialog = false) }
    }

    fun restoreBackupFromWebDav(clearExisting: Boolean) {
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
            webDavService.downloadBackup(config)
                .mapCatching { json ->
                    dataBackupRepository.parseBackupFromJson(json)
                        ?: throw IllegalStateException("远端备份格式无效")
                }
                .fold(
                    onSuccess = { backup ->
                        dataBackupRepository.importData(backup, clearExisting)
                            .onSuccess { result ->
                                preferencesManager.setLastWebDavError(null)
                                _uiState.update {
                                    it.copy(
                                        isSyncingWebDav = false,
                                        importSuccess = true,
                                        lastImportResult = result,
                                        webDavStatusMessage = "已从 WebDAV 恢复最新备份"
                                    )
                                }
                            }
                            .onFailure { error ->
                                preferencesManager.setLastWebDavError(error.message)
                                _uiState.update {
                                    it.copy(
                                        isSyncingWebDav = false,
                                        errorMessage = "恢复失败: ${error.message}"
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
        _uiState.update { it.copy(showClearConfirmDialog = false, importPreview = null) }
    }

    fun clearExportSuccess() {
        _uiState.update { it.copy(exportSuccess = false, exportJson = null) }
    }

    fun clearImportSuccess() {
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
}
