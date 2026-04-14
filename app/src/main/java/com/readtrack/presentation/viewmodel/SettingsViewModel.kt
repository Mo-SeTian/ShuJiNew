package com.readtrack.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.ThemeMode
import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.model.ImportResult
import com.readtrack.domain.repository.DataBackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "settings")

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
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
    @ApplicationContext private val context: Context,
    private val dataBackupRepository: DataBackupRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val doubanCookieKey = stringPreferencesKey("douban_cookie")
    
    init {
        viewModelScope.launch {
            context.dataStore.data
                .map { preferences ->
                    val themeName = preferences[themeModeKey] ?: ThemeMode.SYSTEM.name
                    val cookie = preferences[doubanCookieKey] ?: ""
                    Pair(themeName, cookie)
                }
                .collect { (themeName, cookie) ->
                    _uiState.update { state ->
                        state.copy(
                            themeMode = try { ThemeMode.valueOf(themeName) } catch (e: Exception) { ThemeMode.SYSTEM },
                            doubanCookie = cookie
                        )
                    }
                }
        }
    }
    
    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[themeModeKey] = themeMode.name
            }
            _uiState.update { it.copy(themeMode = themeMode) }
        }
    }
    
    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            
            dataBackupRepository.exportAllData()
                .onSuccess { backup ->
                    val json = backupToJson(backup)
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
    
    fun importData(uri: Uri, jsonContent: String, clearExisting: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null, showClearConfirmDialog = false) }
            
            try {
                val backup = jsonToBackup(jsonContent)
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
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isImporting = false,
                        errorMessage = "解析文件失败: ${e.message}"
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
    
    private fun backupToJson(backup: DataBackup): String {
        return JSONObject().apply {
            put("version", backup.version)
            put("exportTime", backup.exportTime)
            put("appVersion", backup.appVersion)
            
            val booksArray = JSONArray()
            backup.books.forEach { book ->
                booksArray.put(JSONObject().apply {
                    put("id", book.id)
                    put("title", book.title)
                    put("author", book.author ?: JSONObject.NULL)
                    put("publisher", book.publisher ?: JSONObject.NULL)
                    put("progressType", book.progressType)
                    put("totalPages", book.totalPages)
                    put("currentPage", book.currentPage)
                    put("totalChapters", book.totalChapters)
                    put("currentChapter", book.currentChapter)
                    put("coverPath", book.coverPath ?: JSONObject.NULL)
                    put("description", book.description ?: JSONObject.NULL)
                    put("status", book.status)
                    put("createdAt", book.createdAt)
                    put("updatedAt", book.updatedAt)
                })
            }
            put("books", booksArray)
            
            val recordsArray = JSONArray()
            backup.readingRecords.forEach { record ->
                recordsArray.put(JSONObject().apply {
                    put("id", record.id)
                    put("bookId", record.bookId)
                    put("bookTitle", record.bookTitle)
                    put("pagesRead", record.pagesRead)
                    put("fromPage", record.fromPage)
                    put("toPage", record.toPage)
                    put("note", record.note ?: JSONObject.NULL)
                    put("date", record.date)
                })
            }
            put("readingRecords", recordsArray)
        }.toString(2)
    }
    
    private fun jsonToBackup(json: String): DataBackup? {
        return try {
            val jsonObject = JSONObject(json)
            val version = jsonObject.optInt("version", 1)
            val exportTime = jsonObject.optLong("exportTime", System.currentTimeMillis())
            val appVersion = jsonObject.optString("appVersion", "1.0.0")
            
            val booksArray = jsonObject.getJSONArray("books")
            val books = mutableListOf<com.readtrack.domain.model.BookExport>()
            for (i in 0 until booksArray.length()) {
                val bookJson = booksArray.getJSONObject(i)
                books.add(com.readtrack.domain.model.BookExport(
                    id = bookJson.getLong("id"),
                    title = bookJson.getString("title"),
                    author = if (bookJson.isNull("author")) null else bookJson.getString("author"),
                    publisher = if (bookJson.isNull("publisher")) null else bookJson.getString("publisher"),
                    progressType = bookJson.getString("progressType"),
                    totalPages = bookJson.getDouble("totalPages"),
                    currentPage = bookJson.getDouble("currentPage"),
                    totalChapters = bookJson.getInt("totalChapters"),
                    currentChapter = bookJson.getInt("currentChapter"),
                    coverPath = if (bookJson.isNull("coverPath")) null else bookJson.getString("coverPath"),
                    description = if (bookJson.isNull("description")) null else bookJson.getString("description"),
                    status = bookJson.getString("status"),
                    createdAt = bookJson.getLong("createdAt"),
                    updatedAt = bookJson.getLong("updatedAt")
                ))
            }
            
            val recordsArray = jsonObject.optJSONArray("readingRecords") ?: JSONArray()
            val records = mutableListOf<com.readtrack.domain.model.ReadingRecordExport>()
            for (i in 0 until recordsArray.length()) {
                val recordJson = recordsArray.getJSONObject(i)
                records.add(com.readtrack.domain.model.ReadingRecordExport(
                    id = recordJson.getLong("id"),
                    bookId = recordJson.getLong("bookId"),
                    bookTitle = recordJson.getString("bookTitle"),
                    pagesRead = recordJson.getDouble("pagesRead"),
                    fromPage = recordJson.getDouble("fromPage"),
                    toPage = recordJson.getDouble("toPage"),
                    date = recordJson.getLong("date"),
                    note = if (recordJson.isNull("note")) null else recordJson.getString("note")
                ))
            }
            
            DataBackup(
                version = version,
                exportTime = exportTime,
                appVersion = appVersion,
                books = books,
                readingRecords = records
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // ========== 豆瓣Cookie设置 ==========
    
    fun updateDoubanCookie(cookie: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[doubanCookieKey] = cookie
            }
            _uiState.update { it.copy(doubanCookie = cookie, cookieTestResult = null) }
        }
    }
    
    fun testDoubanCookie() {
        val cookie = _uiState.value.doubanCookie
        if (cookie.isBlank()) {
            _uiState.update { it.copy(errorMessage = "可先留空；当前搜索功能已可直接使用") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingCookie = true, cookieTestResult = null, errorMessage = null) }
            
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