package com.readtrack.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val LAST_READ_BOOK_ID = longPreferencesKey("last_read_book_id")
        val DOUBAN_COOKIE = stringPreferencesKey("douban_cookie")
        val STATS_UNIT = stringPreferencesKey("stats_unit")
        val WEBDAV_SERVER_URL = stringPreferencesKey("webdav_server_url")
        val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
        val WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")
        val WEBDAV_REMOTE_PATH = stringPreferencesKey("webdav_remote_path")
        val WEBDAV_AUTO_BACKUP_FREQUENCY = stringPreferencesKey("webdav_auto_backup_frequency")
        val WEBDAV_LAST_BACKUP_AT = longPreferencesKey("webdav_last_backup_at")
        val WEBDAV_LAST_ERROR = stringPreferencesKey("webdav_last_error")
        val HOME_COMPONENT_ORDER = stringPreferencesKey("home_component_order")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val themeName = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val statsUnit: Flow<StatsUnit> = dataStore.data.map { preferences ->
        val unitName = preferences[STATS_UNIT] ?: StatsUnit.CHAPTER.name
        runCatching { StatsUnit.valueOf(unitName) }.getOrDefault(StatsUnit.CHAPTER)
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

    val webDavPassword: Flow<String> = dataStore.data.map { preferences ->
        preferences[WEBDAV_PASSWORD] ?: ""
    }

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

    suspend fun setWebDavConfig(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String
    ) {
        dataStore.edit { preferences ->
            preferences[WEBDAV_SERVER_URL] = serverUrl.trim()
            preferences[WEBDAV_USERNAME] = username.trim()
            preferences[WEBDAV_PASSWORD] = password
            preferences[WEBDAV_REMOTE_PATH] = remotePath.trim().trim('/').ifBlank { "ReadTrack" }
        }
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

    suspend fun setHomeComponentOrder(order: List<String>) {
        dataStore.edit { preferences ->
            preferences[HOME_COMPONENT_ORDER] = order.joinToString(",")
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
