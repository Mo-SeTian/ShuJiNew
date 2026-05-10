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
import android.graphics.Rect
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
            val compositeBitmap = withContext(Dispatchers.IO) {
                val color = extractCoverColor(book?.coverPath) ?: Color.rgb(80, 100, 180)
                val cover = loadCoverBitmap(book?.coverPath)
                createWidgetComposite(color, cover).also {
                    cover?.recycle()
                }
            }
            val remoteViews = buildRemoteViews(context, book, appWidgetId, compositeBitmap)
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

    private fun brightenColor(color: Int, factor: Float): Int {
        return Color.rgb(
            (Color.red(color) * factor).toInt().coerceIn(0, 255),
            (Color.green(color) * factor).toInt().coerceIn(0, 255),
            (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        )
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        return Color.rgb(
            (Color.red(color) * factor).toInt().coerceIn(0, 255),
            (Color.green(color) * factor).toInt().coerceIn(0, 255),
            (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        )
    }

    private fun createWidgetComposite(baseColor: Int, coverBitmap: Bitmap?): Bitmap {
        val size = 400
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 高可见度渐变背景（ brighten 1.6x 确保颜色明显）
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(brightenColor(baseColor, 1.6f), darkenColor(baseColor, 0.15f)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // 2. 封面：centerCrop 裁剪，portrait 比例，像书籍列表那样
        coverBitmap?.let { cover ->
            if (!cover.isRecycled && cover.width > 0 && cover.height > 0) {
                // 封面显示区域：portrait，占 widget 中间大部分
                val coverW = (size * 0.68f).toInt()
                val coverH = (size * 0.76f).toInt()
                val coverLeft = (size - coverW) / 2
                val coverTop = (size - coverH) / 2 - 16
                val dstRect = Rect(coverLeft, coverTop, coverLeft + coverW, coverTop + coverH)

                // CenterCrop：按目标区域比例缩放，从中心裁剪
                val scale = maxOf(coverW.toFloat() / cover.width, coverH.toFloat() / cover.height)
                val srcW = (coverW / scale).toInt()
                val srcH = (coverH / scale).toInt()
                val srcLeft = (cover.width - srcW) / 2
                val srcTop = (cover.height - srcH) / 2
                val srcRect = Rect(srcLeft, srcTop, srcLeft + srcW, srcTop + srcH)

                canvas.drawBitmap(cover, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }

        // 3. 底部渐变遮罩：从 60% 位置开始渐变到纯黑，保证文字可读
        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        scrimPaint.shader = LinearGradient(
            0f, size * 0.58f, 0f, size.toFloat(),
            intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(210, 0, 0, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, size * 0.58f, size.toFloat(), size.toFloat(), scrimPaint)

        return bitmap
    }

    private fun loadCoverBitmap(coverPath: String?): Bitmap? {
        if (coverPath == null) return null
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(coverPath, opts)
            val maxDim = maxOf(opts.outWidth, opts.outHeight)
            if (maxDim <= 0) return null
            opts.inJustDecodeBounds = false
            opts.inSampleSize = when {
                maxDim > 2400 -> 8
                maxDim > 1200 -> 4
                maxDim > 600 -> 2
                else -> 1
            }
            BitmapFactory.decodeFile(coverPath, opts)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildRemoteViews(
        context: Context,
        book: BookEntity?,
        appWidgetId: Int,
        compositeBitmap: Bitmap
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_reading)
        views.setImageViewBitmap(R.id.widget_composite_bg, compositeBitmap)

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
