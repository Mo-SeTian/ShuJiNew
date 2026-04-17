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
import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.repository.DataBackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
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
                preferencesManager.setLastWebDavError("自动备份已跳过：WebDAV 配置不完整")
                return Result.failure()
            }

            val backup = dataBackupRepository.exportAllData().getOrThrow()
            val json = Json.encodeToString(DataBackup.serializer(), backup)
            webDavService.uploadBackup(config, json).getOrThrow()
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
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "webdav_auto_backup"
    }
}
