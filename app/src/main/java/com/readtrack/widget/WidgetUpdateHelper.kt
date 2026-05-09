package com.readtrack.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import com.readtrack.MainActivity
import com.readtrack.R
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.presentation.ui.widget.WidgetQuickRecordActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdateHelper @Inject constructor(
    private val bookDao: BookDao,
    private val preferencesManager: PreferencesManager
) {

    companion object {
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

        appWidgetIds.forEach { appWidgetId ->
            val book = withContext(Dispatchers.IO) {
                val bookId = preferencesManager.getWidgetBookId(appWidgetId)
                bookId?.let { bookDao.getBookByIdOnce(it) }
            }
            val (gradientBitmap, coverBitmap) = withContext(Dispatchers.IO) {
                val color = extractCoverColor(book?.coverPath) ?: Color.rgb(38, 36, 64)
                Pair(createGradientBitmap(color), loadCoverBitmap(book?.coverPath))
            }
            val remoteViews = buildRemoteViews(context, book, appWidgetId, gradientBitmap, coverBitmap)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    suspend fun clearWidgetSelections(appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            preferencesManager.clearWidgetBookId(appWidgetId)
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

    private fun createGradientBitmap(baseColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val start = Color.argb(170, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
        val end = Color.argb(225, 10, 10, 18)
        paint.shader = LinearGradient(0f, 0f, 32f, 32f, start, end, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 32f, 32f, paint)
        return bitmap
    }

    private fun loadCoverBitmap(coverPath: String?): Bitmap? {
        if (coverPath == null) return null
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(coverPath, opts)
            val maxDim = maxOf(opts.outWidth, opts.outHeight)
            val targetDim = 160
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
        appWidgetId: Int,
        gradientBitmap: Bitmap,
        coverBitmap: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_reading)
        views.setImageViewBitmap(R.id.widget_gradient_bg, gradientBitmap)
        if (coverBitmap != null) {
            views.setImageViewBitmap(R.id.widget_cover, coverBitmap)
        }

        if (book != null) {
            views.setTextViewText(R.id.widget_book_title, book.title)
            views.setTextViewText(R.id.widget_progress_text, "${calculateProgressPercent(book)}%")
            val recordIntent = WidgetQuickRecordActivity.createIntent(context, book.id)
            views.setOnClickPendingIntent(
                R.id.widget_record_button,
                PendingIntent.getActivity(
                    context,
                    appWidgetId + 1000,
                    recordIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            views.setTextViewText(R.id.widget_book_title, "选择一本书")
            views.setTextViewText(R.id.widget_progress_text, "设置 → 桌面小组件")
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, openPendingIntent)
        if (book == null) {
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
}
