package com.readtrack.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse { throwable ->
        // 极少数设备 Keystore 异常时回退到普通 SharedPreferences，避免直接崩溃
        android.util.Log.e("SecureStorage", "加密存储初始化失败，回退到普通存储", throwable)
        context.getSharedPreferences(SECURE_PREFS_FALLBACK, Context.MODE_PRIVATE)
    }

    private val _webDavPassword = MutableStateFlow(prefs.getString(KEY_WEBDAV_PASSWORD, "").orEmpty())
    val webDavPassword: StateFlow<String> = _webDavPassword.asStateFlow()

    fun setWebDavPassword(password: String) {
        prefs.edit().putString(KEY_WEBDAV_PASSWORD, password).apply()
        _webDavPassword.value = password
    }

    fun clearWebDavPassword() {
        prefs.edit().remove(KEY_WEBDAV_PASSWORD).apply()
        _webDavPassword.value = ""
    }

    companion object {
        private const val SECURE_PREFS_NAME = "readtrack_secure_prefs"
        private const val SECURE_PREFS_FALLBACK = "readtrack_secure_prefs_fallback"
        private const val KEY_WEBDAV_PASSWORD = "webdav_password"
    }
}
