package com.readtrack.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.readtrack.worker.ReadingReportWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 阅读周报/月报推送调度器。
 * 周报：每 7 天一次，首次对齐到本周日 20:00；
 * 月报：每 30 天一次，首次对齐到本月最后一天 20:00（下一自然月 1 号前）。
 */
@Singleton
class ReportScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 根据开关状态更新两个唯一定时任务。用 UPDATE 策略，切换后立即生效。
     */
    fun updateSchedule(weeklyEnabled: Boolean, monthlyEnabled: Boolean) {
        val workManager = WorkManager.getInstance(context)

        if (weeklyEnabled) {
            val request = PeriodicWorkRequestBuilder<ReadingReportWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(nextWeeklyReportDelayMs(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReadingReportWorker.KEY_REPORT_TYPE to ReadingReportWorker.REPORT_TYPE_WEEKLY))
                .build()
            workManager.enqueueUniquePeriodicWork(
                ReadingReportWorker.WEEKLY_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(ReadingReportWorker.WEEKLY_WORK_NAME)
        }

        if (monthlyEnabled) {
            val request = PeriodicWorkRequestBuilder<ReadingReportWorker>(30, TimeUnit.DAYS)
                .setInitialDelay(nextMonthlyReportDelayMs(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReadingReportWorker.KEY_REPORT_TYPE to ReadingReportWorker.REPORT_TYPE_MONTHLY))
                .build()
            workManager.enqueueUniquePeriodicWork(
                ReadingReportWorker.MONTHLY_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(ReadingReportWorker.MONTHLY_WORK_NAME)
        }
    }

    /** 距本周日 20:00 的毫秒数（若今天已过 20:00，顺延到下周日） */
    private fun nextWeeklyReportDelayMs(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 周日 = Calendar.SUNDAY = 1
            var dayOfWeek = get(Calendar.DAY_OF_WEEK)
            while (dayOfWeek != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_MONTH, 1)
                dayOfWeek = get(Calendar.DAY_OF_WEEK)
            }
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_MONTH, 7)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    /** 距本月最后一天 20:00 的毫秒数（若今天已过 20:00，顺延到下月最后一天） */
    private fun nextMonthlyReportDelayMs(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.MONTH, 1)
            target.set(Calendar.DAY_OF_MONTH, target.getActualMaximum(Calendar.DAY_OF_MONTH))
            target.set(Calendar.HOUR_OF_DAY, 20)
            target.set(Calendar.MINUTE, 0)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
