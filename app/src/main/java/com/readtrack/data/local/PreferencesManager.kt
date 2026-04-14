package com.readtrack.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
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
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val themeName = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
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

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
