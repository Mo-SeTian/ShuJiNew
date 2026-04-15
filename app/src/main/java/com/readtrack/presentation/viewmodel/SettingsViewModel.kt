package com.readtrack.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.ThemeMode
import com.readtrack.domain.model.ImportResult
import com.readtrack.domain.repository.DataBackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val statsUnit: StatsUnit = StatsUnit.CHAPTER,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val lastImportResult: ImportResult? = null,
    val errorMessage: String? = null,
    val exportJson: String? = null,
    val showClearConfirmDialog: Boolean = false,
    // 豆瓣Cookie设置
    val doubanCookie: String = "",
    val isTestingCookie: Boolean = false,
    val cookieTestResult: CookieTestResult? = null
)

enum class CookieTestResult {
    SUCCESS,
    INVALID,
    NETWORK_ERROR
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataBackupRepository: DataBackupRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesManager.themeMode,
                preferencesManager.statsUnit,
                preferencesManager.doubanCookie
            ) { themeMode, statsUnit, cookie ->
                Triple(themeMode, statsUnit, cookie)
            }.collect { (themeMode, statsUnit, cookie) ->
                    _uiState.update { state ->
                        state.copy(
                            themeMode = themeMode,
                            statsUnit = statsUnit,
                            doubanCookie = cookie
                        )
                    }
                }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(themeMode)
            _uiState.update { it.copy(themeMode = themeMode) }
        }
    }

    fun setStatsUnit(unit: StatsUnit) {
        viewModelScope.launch {
            preferencesManager.setStatsUnit(unit)
            _uiState.update { it.copy(statsUnit = unit) }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }

            dataBackupRepository.exportAllData()
                .onSuccess { backup ->
                    val json = kotlinx.serialization.json.Json.encodeToString(
                        com.readtrack.domain.model.DataBackup.serializer(),
                        backup
                    )
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

    fun importData(jsonContent: String, clearExisting: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null, showClearConfirmDialog = false) }

            val backup = dataBackupRepository.parseBackupFromJson(jsonContent)
            if (backup != null) {
                dataBackupRepository.importData(backup, clearExisting)
                    .onSuccess { result ->
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                importSuccess = true,
                                lastImportResult = result
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
            } else {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "文件格式无效"
                    )
                }
            }
        }
    }

    fun showClearConfirmDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = true) }
    }

    fun dismissClearConfirmDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = false) }
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

    // ========== 豆瓣Cookie设置 ==========

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
                val connection = java.net.URL(url).openConnection()
                connection.setRequestProperty("Cookie", cookie)
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                connection.setRequestProperty("Referer", "https://book.douban.com/")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = (connection as java.net.HttpURLConnection).responseCode

                val response = connection.inputStream.bufferedReader().use { it.readText() }

                if (response.contains("window.__DATA__")) {
                    _uiState.update { it.copy(isTestingCookie = false, cookieTestResult = CookieTestResult.SUCCESS) }
                } else if (responseCode == 401 || responseCode == 403) {
                    _uiState.update { it.copy(isTestingCookie = false, cookieTestResult = CookieTestResult.INVALID, errorMessage = "Cookie无效或已过期 (HTTP $responseCode)") }
                } else {
                    _uiState.update { it.copy(isTestingCookie = false, cookieTestResult = CookieTestResult.INVALID, errorMessage = "未能验证 Cookie，请检查格式或稍后重试") }
                }
            } catch (e: java.net.SocketTimeoutException) {
                _uiState.update { it.copy(isTestingCookie = false, cookieTestResult = CookieTestResult.NETWORK_ERROR, errorMessage = "连接超时，请检查网络") }
            } catch (e: java.net.UnknownHostException) {
                _uiState.update { it.copy(isTestingCookie = false, cookieTestResult = CookieTestResult.NETWORK_ERROR, errorMessage = "无法连接豆瓣，请检查网络") }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "未知错误"
                _uiState.update { it.copy(isTestingCookie = false, cookieTestResult = CookieTestResult.NETWORK_ERROR, errorMessage = "网络错误: $errorMsg") }
            }
        }
    }

    fun clearCookieTestResult() {
        _uiState.update { it.copy(cookieTestResult = null) }
    }
}
