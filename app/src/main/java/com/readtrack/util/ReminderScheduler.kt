package com.readtrack.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.readtrack.data.local.PreferencesManager
import com.readtrack.worker.ReadingReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    suspend fun applyConfig(config: PreferencesManager.ReminderConfig) {
        preferencesManager.setReminderConfig(config.enabled, config.hour, config.minute)
        if (config.enabled) {
            schedule(config.hour, config.minute)
        } else {
            cancel()
        }
    }

    private fun schedule(hour: Int, minute: Int) {
        // 计算初次执行延迟，对齐到目标时间
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<ReadingReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().build())
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                ReadingReminderWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }

    private fun cancel() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(ReadingReminderWorker.UNIQUE_WORK_NAME)
    }
}
