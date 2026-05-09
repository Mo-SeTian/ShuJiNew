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
import com.readtrack.presentation.ui.widget.WidgetQuickRecordActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdateHelper @Inject constructor(
    private val bookDao: BookDao
) {

    companion object {
        const val PREFS_NAME = "widget_page_state"
        const val PAGE_KEY_PREFIX = "page_"
        private const val MAX_BOOKS = 5

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, ReadingWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }

    suspend fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ReadingWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        val books = withContext(Dispatchers.IO) {
            bookDao.getRecentReadingBooks(limit = MAX_BOOKS)
        }
        val bookCount = books.size
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        appWidgetIds.forEach { appWidgetId ->
            val savedPage = prefs.getInt("$PAGE_KEY_PREFIX$appWidgetId", 0)
            val currentPage = savedPage.coerceIn(0, (bookCount - 1).coerceAtLeast(0))

            if (currentPage != savedPage) {
                prefs.edit().putInt("$PAGE_KEY_PREFIX$appWidgetId", currentPage).apply()
            }

            val book = if (bookCount > 0) books[currentPage] else null
            val remoteViews = buildRemoteViews(context, book, bookCount, currentPage, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun buildRemoteViews(
        context: Context,
        book: BookEntity?,
        bookCount: Int,
        currentPage: Int,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_reading)

        if (book != null) {
            views.setTextViewText(R.id.widget_book_title, book.title)
            views.setTextViewText(R.id.widget_book_author, book.author ?: "")
            val progressPercent = calculateProgressPercent(book)
            views.setTextViewText(R.id.widget_progress_text, "$progressPercent%")
            views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)

            // 点击书籍区域打开详情
            val detailIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_BOOK_DETAIL
                putExtra(MainActivity.EXTRA_BOOK_ID, book.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(R.id.widget_container,
                PendingIntent.getActivity(context, appWidgetId, detailIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            // 点击记录按钮打开快速记录
            val recordIntent = WidgetQuickRecordActivity.createIntent(context, book.id)
            views.setOnClickPendingIntent(R.id.widget_record_button,
                PendingIntent.getActivity(context, appWidgetId + 1000, recordIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            views.setTextViewText(R.id.widget_book_title, "还没有正在读的书")
            views.setTextViewText(R.id.widget_book_author, "去添加一本吧")
            views.setTextViewText(R.id.widget_progress_text, "")
            views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            views.setOnClickPendingIntent(R.id.widget_container,
                PendingIntent.getActivity(context, appWidgetId, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            views.setOnClickPendingIntent(R.id.widget_record_button,
                PendingIntent.getActivity(context, appWidgetId, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }

        // 分页指示
        views.setTextViewText(R.id.widget_page_dots,
            if (bookCount > 0) "${currentPage + 1}/$bookCount" else "0/0")

        // 翻页按钮
        val canPrev = bookCount > 0 && currentPage > 0
        val canNext = bookCount > 0 && currentPage < bookCount - 1

        views.setTextViewText(R.id.widget_prev_button, if (canPrev) "◀" else "")
        views.setTextViewText(R.id.widget_next_button, if (canNext) "▶" else "")

        if (canPrev) {
            val prevIntent = Intent(context, ReadingWidgetProvider::class.java).apply {
                action = ReadingWidgetProvider.ACTION_PREV_PAGE
                putExtra(ReadingWidgetProvider.EXTRA_WIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(R.id.widget_prev_button,
                PendingIntent.getBroadcast(context, appWidgetId + 2000, prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            views.setOnClickPendingIntent(R.id.widget_prev_button, null)
        }

        if (canNext) {
            val nextIntent = Intent(context, ReadingWidgetProvider::class.java).apply {
                action = ReadingWidgetProvider.ACTION_NEXT_PAGE
                putExtra(ReadingWidgetProvider.EXTRA_WIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(R.id.widget_next_button,
                PendingIntent.getBroadcast(context, appWidgetId + 3000, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            views.setOnClickPendingIntent(R.id.widget_next_button, null)
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
}
