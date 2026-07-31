package com.readtrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.readtrack.data.local.AutoBackupFrequency
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.remote.WebDavConfig
import com.readtrack.data.remote.WebDavService
import com.readtrack.data.repository.DataBackupRepositoryImpl
import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.repository.DataBackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class WebDavAutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataBackupRepository: DataBackupRepository,
    private val preferencesManager: PreferencesManager,
    private val webDavService: WebDavService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val config = loadConfig()
            if (!config.isValid()) {
                preferencesManager.setLastWebDavError(applicationContext.getString(com.readtrack.R.string.webdav_backup_skipped))
                return Result.failure()
            }

            // 优先使用 ZIP 备份
            val impl = dataBackupRepository as? DataBackupRepositoryImpl
            if (impl != null) {
                val zipFile = impl.exportToZip().getOrThrow()
                try {
                    val zipData = zipFile.readBytes()
                    webDavService.uploadBackupZip(config, zipData).getOrThrow()
                } finally {
                    // 清理临时 ZIP，避免缓存目录持续累积（含封面，体积较大）
                    zipFile.delete()
                }
            } else {
                // 回退到 JSON
                val backup = dataBackupRepository.exportAllData().getOrThrow()
                val json = Json.encodeToString(DataBackup.serializer(), backup)
                webDavService.uploadBackup(config, json).getOrThrow()
            }

            preferencesManager.setLastWebDavBackupAt(System.currentTimeMillis())
            preferencesManager.setLastWebDavError(null)
            Result.success()
        } catch (error: Exception) {
            preferencesManager.setLastWebDavError(error.message ?: "自动备份失败")
            Result.retry()
        }
    }

    private suspend fun loadConfig(): WebDavConfig {
        return WebDavConfig(
            serverUrl = preferencesManager.webDavServerUrl.first(),
            username = preferencesManager.webDavUsername.first(),
            password = preferencesManager.webDavPassword.first(),
            remotePath = preferencesManager.webDavRemotePath.first()
        )
    }
}

@Singleton
class WebDavBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun updateSchedule(frequency: AutoBackupFrequency) {
        val workManager = WorkManager.getInstance(context)
        if (frequency == AutoBackupFrequency.OFF) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<WebDavAutoBackupWorker>(
            frequency.intervalDays,
            TimeUnit.DAYS
        )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            // 用 UPDATE 而非 KEEP：用户修改备份频率（每日↔每周）后必须重新调度才生效
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "webdav_auto_backup"
    }
}
