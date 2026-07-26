package com.readtrack.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.readtrack.domain.model.PreferencesExport
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class WidgetBookEntry(val widgetId: Int, val bookId: Long)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class StatsUnit {
    CHAPTER,
    PAGE
}

enum class StatsRange(val days: Int, val label: String) {
    WEEK(7, "本周"),
    MONTH(30, "本月"),
    TWO_MONTHS(60, "2个月"),
    THREE_MONTHS(90, "3个月"),
    ALL(-1, "全部")
}

enum class AutoBackupFrequency(val intervalDays: Long) {
    OFF(0),
    DAILY(1),
    WEEKLY(7)
}

// 首页自定义组件
enum class HomeComponent(val id: String, val title: String) {
    HERO("hero", "阅读总览"),
    OVERVIEW("overview", "今日阅读"),
    INSIGHT("insight", "阅读洞察"),
    STATUS("status", "书架状态"),
    RECENT("recent", "最近阅读")
}

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage
) {
    private val dataStore = context.dataStore

    private val webDavPasswordMigrated = java.util.concurrent.atomic.AtomicBoolean(false)

    private suspend fun migrateWebDavPasswordIfNeeded() {
        if (!webDavPasswordMigrated.compareAndSet(false, true)) return
        runCatching {
            val legacy = dataStore.data.first()[WEBDAV_PASSWORD]
            if (!legacy.isNullOrEmpty()) {
                if (secureStorage.webDavPassword.value.isEmpty()) {
                    secureStorage.setWebDavPassword(legacy)
                }
                dataStore.edit { it.remove(WEBDAV_PASSWORD) }
                android.util.Log.i("PreferencesManager", "已迁移 WebDAV 密码到加密存储")
            }
        }.onFailure { android.util.Log.w("PreferencesManager", "WebDAV 密码迁移失败", it) }
    }

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val LAST_READ_BOOK_ID = longPreferencesKey("last_read_book_id")
        val DOUBAN_COOKIE = stringPreferencesKey("douban_cookie")
        val STATS_UNIT = stringPreferencesKey("stats_unit")
        val STATS_RANGE = stringPreferencesKey("stats_range")
        val WEBDAV_SERVER_URL = stringPreferencesKey("webdav_server_url")
        val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
        val WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")
        val WEBDAV_REMOTE_PATH = stringPreferencesKey("webdav_remote_path")
        val WEBDAV_AUTO_BACKUP_FREQUENCY = stringPreferencesKey("webdav_auto_backup_frequency")
        val WEBDAV_LAST_BACKUP_AT = longPreferencesKey("webdav_last_backup_at")
        val WEBDAV_LAST_ERROR = stringPreferencesKey("webdav_last_error")
        val HOME_COMPONENT_ORDER = stringPreferencesKey("home_component_order")
        val UPDATE_SOURCE = stringPreferencesKey("update_source")
        val WIDGET_BOOK_MAP = stringPreferencesKey("widget_book_map")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    data class ReminderConfig(
        val enabled: Boolean = false,
        val hour: Int = 21,
        val minute: Int = 0
    )

    private fun widgetBookIdKey(appWidgetId: Int) = longPreferencesKey("widget_${appWidgetId}_book_id")

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val themeName = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val statsUnit: Flow<StatsUnit> = dataStore.data.map { preferences ->
        val unitName = preferences[STATS_UNIT] ?: StatsUnit.CHAPTER.name
        runCatching { StatsUnit.valueOf(unitName) }.getOrDefault(StatsUnit.CHAPTER)
    }

    val statsRange: Flow<StatsRange> = dataStore.data.map { preferences ->
        val rangeName = preferences[STATS_RANGE] ?: StatsRange.WEEK.name
        runCatching { StatsRange.valueOf(rangeName) }.getOrDefault(StatsRange.WEEK)
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH] ?: true
    }

    val lastReadBookId: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[LAST_READ_BOOK_ID]
    }

    val doubanCookie: Flow<String> = dataStore.data.map { preferences ->
        preferences[DOUBAN_COOKIE] ?: ""
    }

    val webDavServerUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[WEBDAV_SERVER_URL] ?: ""
    }

    val webDavUsername: Flow<String> = dataStore.data.map { preferences ->
        preferences[WEBDAV_USERNAME] ?: ""
    }

    val webDavPassword: Flow<String> = secureStorage.webDavPassword
        .onStart { migrateWebDavPasswordIfNeeded() }

    val webDavRemotePath: Flow<String> = dataStore.data.map { preferences ->
        preferences[WEBDAV_REMOTE_PATH] ?: "ReadTrack"
    }

    val autoBackupFrequency: Flow<AutoBackupFrequency> = dataStore.data.map { preferences ->
        val frequencyName = preferences[WEBDAV_AUTO_BACKUP_FREQUENCY] ?: AutoBackupFrequency.OFF.name
        runCatching { AutoBackupFrequency.valueOf(frequencyName) }.getOrDefault(AutoBackupFrequency.OFF)
    }

    val lastWebDavBackupAt: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[WEBDAV_LAST_BACKUP_AT]
    }

    val lastWebDavError: Flow<String?> = dataStore.data.map { preferences ->
        preferences[WEBDAV_LAST_ERROR]
    }

    val updateSource: Flow<String> = dataStore.data.map { preferences ->
        preferences[UPDATE_SOURCE] ?: "github"
    }

    val reminderConfigFlow: Flow<ReminderConfig> = dataStore.data.map { preferences ->
        ReminderConfig(
            enabled = preferences[REMINDER_ENABLED] ?: false,
            hour = preferences[REMINDER_HOUR] ?: 21,
            minute = preferences[REMINDER_MINUTE] ?: 0
        )
    }

    val homeComponentOrder: Flow<List<String>> = dataStore.data.map { preferences ->
        val stored = preferences[HOME_COMPONENT_ORDER]
        if (stored.isNullOrBlank()) {
            // 默认顺序
            HomeComponent.entries.map { it.id }
        } else {
            stored.split(",").filter { id -> HomeComponent.entries.any { it.id == id } }
                .ifEmpty { HomeComponent.entries.map { it.id } }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH] = isFirst
        }
    }

    suspend fun setLastReadBookId(bookId: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_READ_BOOK_ID] = bookId
        }
    }

    suspend fun setDoubanCookie(cookie: String) {
        dataStore.edit { preferences ->
            preferences[DOUBAN_COOKIE] = cookie
        }
    }

    suspend fun setStatsUnit(unit: StatsUnit) {
        dataStore.edit { preferences ->
            preferences[STATS_UNIT] = unit.name
        }
    }

    suspend fun setStatsRange(range: StatsRange) {
        dataStore.edit { preferences ->
            preferences[STATS_RANGE] = range.name
        }
    }

    suspend fun setWebDavConfig(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String
    ) {
        dataStore.edit { preferences ->
            preferences[WEBDAV_SERVER_URL] = serverUrl.trim()
            preferences[WEBDAV_USERNAME] = username.trim()
            preferences[WEBDAV_REMOTE_PATH] = remotePath.trim().trim('/').ifBlank { "ReadTrack" }
            // 遗留明文一律清除，密码走加密存储
            preferences.remove(WEBDAV_PASSWORD)
        }
        secureStorage.setWebDavPassword(password)
    }

    suspend fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        dataStore.edit { preferences ->
            preferences[WEBDAV_AUTO_BACKUP_FREQUENCY] = frequency.name
        }
    }

    suspend fun setLastWebDavBackupAt(timestamp: Long?) {
        dataStore.edit { preferences ->
            if (timestamp == null) {
                preferences.remove(WEBDAV_LAST_BACKUP_AT)
            } else {
                preferences[WEBDAV_LAST_BACKUP_AT] = timestamp
            }
        }
    }

    suspend fun setLastWebDavError(message: String?) {
        dataStore.edit { preferences ->
            if (message.isNullOrBlank()) {
                preferences.remove(WEBDAV_LAST_ERROR)
            } else {
                preferences[WEBDAV_LAST_ERROR] = message
            }
        }
    }

    suspend fun setUpdateSource(source: String) {
        dataStore.edit { preferences ->
            preferences[UPDATE_SOURCE] = source
        }
    }

    suspend fun setHomeComponentOrder(order: List<String>) {
        dataStore.edit { preferences ->
            preferences[HOME_COMPONENT_ORDER] = order.joinToString(",")
        }
    }

    fun widgetBookIds(appWidgetIds: IntArray): Flow<Map<Int, Long?>> {
        return dataStore.data.map { preferences ->
            appWidgetIds.associateWith { appWidgetId ->
                preferences[widgetBookIdKey(appWidgetId)]
            }
        }
    }

    suspend fun getWidgetBookId(appWidgetId: Int): Long? {
        return dataStore.data.first()[widgetBookIdKey(appWidgetId)]
    }

    private suspend fun readWidgetEntries(): List<WidgetBookEntry> {
        val raw = dataStore.data.first()[WIDGET_BOOK_MAP] ?: return emptyList()
        return try {
            Json.decodeFromString<List<WidgetBookEntry>>(raw)
        } catch (e: Exception) {
            android.util.Log.w("PreferencesManager", "小组件映射反序列化失败: ${e.message}")
            emptyList()
        }
    }

    private suspend fun writeWidgetEntries(entries: List<WidgetBookEntry>) {
        dataStore.edit { preferences ->
            preferences[WIDGET_BOOK_MAP] = Json.encodeToString(entries)
        }
    }

    suspend fun setWidgetBookId(appWidgetId: Int, bookId: Long) {
        dataStore.edit { preferences ->
            preferences[widgetBookIdKey(appWidgetId)] = bookId
        }
        val entries = readWidgetEntries().toMutableList()
        entries.removeAll { it.widgetId == appWidgetId }
        entries.add(WidgetBookEntry(appWidgetId, bookId))
        writeWidgetEntries(entries)
    }

    suspend fun clearWidgetBookId(appWidgetId: Int) {
        dataStore.edit { preferences ->
            preferences.remove(widgetBookIdKey(appWidgetId))
        }
        val entries = readWidgetEntries().toMutableList()
        entries.removeAll { it.widgetId == appWidgetId }
        writeWidgetEntries(entries)
    }

    /** 清理已删除小组件的残留映射 */
    suspend fun pruneStaleWidgetEntries(existingIds: Set<Int>) {
        val entries = readWidgetEntries()
        val stale = entries.filter { it.widgetId !in existingIds }
        if (stale.isEmpty()) return
        val keep = entries.filter { it.widgetId in existingIds }
        dataStore.edit { preferences ->
            stale.forEach { preferences.remove(widgetBookIdKey(it.widgetId)) }
            preferences[WIDGET_BOOK_MAP] = Json.encodeToString(keep)
        }
    }

    suspend fun setReminderConfig(enabled: Boolean, hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[REMINDER_ENABLED] = enabled
            preferences[REMINDER_HOUR] = hour
            preferences[REMINDER_MINUTE] = minute
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    /**
     * 将当前所有偏好设置序列化为 PreferencesExport 模型（不含 lastBackupAt / lastError 等运行时状态）
     */
    suspend fun exportPreferences(): PreferencesExport {
        return dataStore.data.first().let { preferences ->
            PreferencesExport(
                themeMode = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name,
                statsUnit = preferences[STATS_UNIT] ?: StatsUnit.CHAPTER.name,
                statsRange = preferences[STATS_RANGE] ?: StatsRange.WEEK.name,
                doubanCookie = preferences[DOUBAN_COOKIE] ?: "",
                webDavServerUrl = preferences[WEBDAV_SERVER_URL] ?: "",
                webDavUsername = preferences[WEBDAV_USERNAME] ?: "",
                // 安全策略：不导出密码明文，避免分享备份文件时泄露
                webDavPassword = "",
                webDavRemotePath = preferences[WEBDAV_REMOTE_PATH] ?: "ReadTrack",
                webDavAutoBackupFrequency = preferences[WEBDAV_AUTO_BACKUP_FREQUENCY] ?: AutoBackupFrequency.OFF.name,
                updateSource = preferences[UPDATE_SOURCE] ?: "github",
                homeComponentOrder = (preferences[HOME_COMPONENT_ORDER] ?: "").split(",").filter { it.isNotBlank() },
                widgetBookMap = run {
                    val raw = preferences[WIDGET_BOOK_MAP] ?: ""
                    if (raw.isBlank()) emptyMap()
                    else try {
                        Json.decodeFromString<List<WidgetBookEntry>>(raw)
                            .associate { it.widgetId.toString() to it.bookId }
                    } catch (e: Exception) {
                        android.util.Log.w("PreferencesManager", "导出时小组件映射反序列化失败: ${e.message}")
                        emptyMap()
                    }
                }
            )
        }
    }

    /**
     * 将 PreferencesExport 写入 DataStore（不清洗运行时状态字段）
     */
    suspend fun importPreferences(prefs: PreferencesExport) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = prefs.themeMode.takeIf { v -> runCatching { ThemeMode.valueOf(v) }.isSuccess } ?: ThemeMode.SYSTEM.name
            preferences[STATS_UNIT] = prefs.statsUnit.takeIf { v -> runCatching { StatsUnit.valueOf(v) }.isSuccess } ?: StatsUnit.CHAPTER.name
            preferences[STATS_RANGE] = prefs.statsRange.takeIf { v -> runCatching { StatsRange.valueOf(v) }.isSuccess } ?: StatsRange.WEEK.name
            preferences[DOUBAN_COOKIE] = prefs.doubanCookie
            preferences[WEBDAV_SERVER_URL] = prefs.webDavServerUrl
            preferences[WEBDAV_USERNAME] = prefs.webDavUsername
            // WEBDAV_PASSWORD 不再写入 DataStore；导入时若有则写入加密存储
            preferences[WEBDAV_REMOTE_PATH] = prefs.webDavRemotePath.trim('/').ifBlank { "ReadTrack" }
            preferences[WEBDAV_AUTO_BACKUP_FREQUENCY] = prefs.webDavAutoBackupFrequency.takeIf { v -> runCatching { AutoBackupFrequency.valueOf(v) }.isSuccess } ?: AutoBackupFrequency.OFF.name
            preferences[UPDATE_SOURCE] = prefs.updateSource.takeIf { it == "github" || it == "gitee" } ?: "github"
            preferences[HOME_COMPONENT_ORDER] = prefs.homeComponentOrder.filter { id -> HomeComponent.entries.any { it.id == id } }.joinToString(",")
            if (prefs.widgetBookMap.isNotEmpty()) {
                val entries = prefs.widgetBookMap.mapNotNull { (keyStr, bookId) ->
                    keyStr.toIntOrNull()?.let { WidgetBookEntry(it, bookId) }
                }
                preferences[WIDGET_BOOK_MAP] = Json.encodeToString(entries)
                // Also restore individual keys for compatibility
                entries.forEach { entry ->
                    preferences[widgetBookIdKey(entry.widgetId)] = entry.bookId
                }
            }
        }
        if (prefs.webDavPassword.isNotBlank()) {
            secureStorage.setWebDavPassword(prefs.webDavPassword)
        }
    }
}
