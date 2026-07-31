package com.readtrack.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.readtrack.MainActivity
import com.readtrack.R
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.util.getStartOfDay
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * 阅读周报/月报推送。
 * 按报告类型统计近 7 天 / 当前自然月的阅读量，生成摘要通知。
 */
@HiltWorker
class ReadingReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesManager: PreferencesManager,
    private val readingRecordRepository: ReadingRecordRepository,
    private val bookRepository: BookRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val reportType = inputData.getString(KEY_REPORT_TYPE) ?: return Result.success()
        val enabled = when (reportType) {
            REPORT_TYPE_WEEKLY -> preferencesManager.weeklyReportEnabled.first()
            REPORT_TYPE_MONTHLY -> preferencesManager.monthlyReportEnabled.first()
            else -> false
        }
        if (!enabled) return Result.success()

        val now = System.currentTimeMillis()
        val (start, end) = when (reportType) {
            REPORT_TYPE_WEEKLY -> weeklyRange(now)
            else -> monthlyRange(now)
        }

        val records = readingRecordRepository.getRecordsByDateRange(start, end).first()
        val books = bookRepository.getAllBooks().first()
        val titleLookup = books.associate { it.id to it.title }

        val summary = buildSummary(records, titleLookup)
        val title = if (reportType == REPORT_TYPE_WEEKLY) "阅读周报" else "阅读月报"
        showNotification(title, summary)
        return Result.success()
    }

    private fun weeklyRange(now: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        return getStartOfDay(calendar.timeInMillis) to now
    }

    private fun monthlyRange(now: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis to now
    }

    private fun buildSummary(
        records: List<com.readtrack.data.local.entity.ReadingRecordEntity>,
        titleLookup: Map<Long, String>
    ): String {
        val normalRecords = records.filter { it.recordType == RecordType.NORMAL }
        var pages = 0.0
        var chapters = 0
        val activeDays = HashSet<Long>()
        val amountByBook = mutableMapOf<String, Double>()

        normalRecords.forEach { record ->
            val isChapter = record.bookSnapshot?.progressType == ProgressType.CHAPTER
            val amount = if (isChapter) (record.chaptersRead ?: 0).toDouble() else record.pagesRead
            if (isChapter) chapters += amount.toInt() else pages += amount
            activeDays.add(getStartOfDay(record.date))

            val title = record.bookId?.let { titleLookup[it] }
                ?: record.bookSnapshot?.title
                ?: "未知书籍"
            amountByBook[title] = (amountByBook[title] ?: 0.0) + amount
        }

        val finishedCount = records.count { it.recordType == RecordType.STATUS_FINISHED }
        val parts = mutableListOf<String>()
        if (pages > 0) parts.add("${pages.toInt()} 页")
        if (chapters > 0) parts.add("$chapters 章")
        if (parts.isEmpty()) {
            return if (finishedCount > 0) "没有新增阅读量，读完 $finishedCount 本书" else "没有新增阅读记录，抽空翻开一本书吧"
        }

        val content = StringBuilder()
        content.append("共读 ").append(parts.joinToString("、"))
        content.append("，打卡 ").append(activeDays.size).append(" 天")
        if (finishedCount > 0) content.append("，读完 ").append(finishedCount).append(" 本")
        amountByBook.maxByOrNull { it.value }?.let { (title, _) ->
            content.append("。读得最多的是《").append(title).append("》")
        }
        return content.toString()
    }

    private fun showNotification(title: String, content: String) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_legacy)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "reading_report"
        const val NOTIFICATION_ID = 1002
        const val KEY_REPORT_TYPE = "report_type"
        const val REPORT_TYPE_WEEKLY = "weekly"
        const val REPORT_TYPE_MONTHLY = "monthly"
        const val WEEKLY_WORK_NAME = "reading_report_weekly"
        const val MONTHLY_WORK_NAME = "reading_report_monthly"
    }
}
