package com.readtrack.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.readtrack.MainActivity
import com.readtrack.R
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.widget.WidgetQuickRecordActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdateHelper @Inject constructor(
    private val bookDao: BookDao
) {

    suspend fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ReadingWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        val books = withContext(Dispatchers.IO) {
            bookDao.getRecentReadingBooks(limit = 1)
        }
        val book = books.firstOrNull()

        appWidgetIds.forEach { appWidgetId ->
            val remoteViews = buildRemoteViews(context, book, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun buildRemoteViews(
        context: Context,
        book: BookEntity?,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_reading)

        if (book != null) {
            views.setTextViewText(R.id.widget_book_title, book.title)
            views.setTextViewText(R.id.widget_book_author, book.author ?: "")

            val progressPercent = calculateProgressPercent(book)
            views.setTextViewText(R.id.widget_progress_text, "$progressPercent%")
            views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)

            // 点击书名区域打开书籍详情
            val detailIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_BOOK_DETAIL
                putExtra(MainActivity.EXTRA_BOOK_ID, book.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val detailPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, detailPendingIntent)

            // 点击记录按钮打开快速记录
            val recordIntent = Intent(context, WidgetQuickRecordActivity::class.java).apply {
                putExtra(WidgetQuickRecordActivity.EXTRA_BOOK_ID, book.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val recordPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 1000,
                recordIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_record_button, recordPendingIntent)
        } else {
            views.setTextViewText(R.id.widget_book_title, "还没有正在读的书")
            views.setTextViewText(R.id.widget_book_author, "去添加一本吧")
            views.setTextViewText(R.id.widget_progress_text, "")
            views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)

            // 点击打开应用
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_record_button, openPendingIntent)
        }

        return views
    }

    private fun calculateProgressPercent(book: BookEntity): Int {
        val chapterBased = (book.totalChapters ?: 0) > 0
        return when {
            chapterBased -> {
                val total = (book.totalChapters ?: 0).coerceAtLeast(1)
                ((book.currentChapter.toFloat() / total) * 100f).toInt()
            }
            book.totalPages > 0 -> ((book.currentPage.toFloat() / book.totalPages.toFloat()) * 100f).toInt()
            else -> 0
        }.coerceIn(0, 100)
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, ReadingWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
