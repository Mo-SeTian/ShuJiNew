package com.readtrack.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
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
            val (scrimBitmap, coverBitmap) = if (book != null) {
                withContext(Dispatchers.IO) {
                    val color = extractCoverColor(book.coverPath)
                    val scrim = if (color != null) {
                        val alphaColor = (102 shl 24) or (color and 0x00FFFFFF)
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).also { it.eraseColor(alphaColor) }
                    } else null
                    val thumb = loadCoverThumbnail(book.coverPath)
                    Pair(scrim, thumb)
                }
            } else {
                Pair(null, null)
            }

            val remoteViews = buildRemoteViews(context, book, bookCount, currentPage, appWidgetId, scrimBitmap, coverBitmap)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun extractCoverColor(coverPath: String?): Int? {
        if (coverPath == null) return null
        return try {
            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
            val source = BitmapFactory.decodeFile(coverPath, options) ?: return null
            val palette = Palette.from(source).generate()
            val swatch = palette.vibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch
            source.recycle()
            swatch?.rgb
        } catch (e: Exception) {
            null
        }
    }

    private fun loadCoverThumbnail(coverPath: String?): Bitmap? {
        if (coverPath == null) return null
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(coverPath, opts)
            val maxDim = maxOf(opts.outWidth, opts.outHeight)
            val targetDim = 180
            opts.inJustDecodeBounds = false
            opts.inSampleSize = (maxDim / targetDim).coerceIn(1, 16)
            val decoded = BitmapFactory.decodeFile(coverPath, opts) ?: return null
            val w = decoded.width
            val h = decoded.height
            val scale = minOf(targetDim.toFloat() / w, targetDim.toFloat() / h)
            Bitmap.createScaledBitmap(decoded, (w * scale).toInt(), (h * scale).toInt(), true)
                .also { if (it != decoded) decoded.recycle() }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildRemoteViews(
        context: Context,
        book: BookEntity?,
        bookCount: Int,
        currentPage: Int,
        appWidgetId: Int,
        scrimBitmap: Bitmap?,
        coverBitmap: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_reading)

        if (scrimBitmap != null) {
            views.setImageViewBitmap(R.id.widget_scrim, scrimBitmap)
        }

        if (book != null) {
            if (coverBitmap != null) {
                views.setImageViewBitmap(R.id.widget_cover, coverBitmap)
            }

            views.setTextViewText(R.id.widget_book_title, book.title)
            val progressPercent = calculateProgressPercent(book)
            views.setTextViewText(R.id.widget_progress_text, "$progressPercent%")
            views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)

            // 点击封面/容器打开详情
            val detailIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_BOOK_DETAIL
                putExtra(MainActivity.EXTRA_BOOK_ID, book.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(R.id.widget_cover,
                PendingIntent.getActivity(context, appWidgetId + 5000, detailIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            views.setOnClickPendingIntent(R.id.widget_container,
                PendingIntent.getActivity(context, appWidgetId, detailIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            // 记录按钮
            val recordIntent = WidgetQuickRecordActivity.createIntent(context, book.id)
            views.setOnClickPendingIntent(R.id.widget_record_button,
                PendingIntent.getActivity(context, appWidgetId + 1000, recordIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            views.setTextViewText(R.id.widget_book_title, "还没有正在读的书")
            views.setTextViewText(R.id.widget_progress_text, "")
            views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            views.setOnClickPendingIntent(R.id.widget_container,
                PendingIntent.getActivity(context, appWidgetId, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            views.setOnClickPendingIntent(R.id.widget_cover,
                PendingIntent.getActivity(context, appWidgetId + 5000, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            views.setOnClickPendingIntent(R.id.widget_record_button,
                PendingIntent.getActivity(context, appWidgetId + 1000, openIntent,
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
