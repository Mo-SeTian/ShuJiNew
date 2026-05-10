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
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
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
                val color = extractCoverColor(book?.coverPath) ?: Color.rgb(38, 36, 64)
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

    private fun createWidgetComposite(baseColor: Int, coverBitmap: Bitmap?): Bitmap {
        val size = 400
        val cornerRadius = size * 0.07f
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 圆角裁剪
        val clipPath = Path().apply {
            addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()),
                cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        // 1. 渐变背景（完全不透明，让颜色可见）
        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = Color.red(baseColor)
        val g = Color.green(baseColor)
        val b = Color.blue(baseColor)
        gradientPaint.shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(Color.rgb(r, g, b), Color.rgb((r * 0.3f).toInt(), (g * 0.3f).toInt(), (b * 0.3f).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), gradientPaint)

        // 2. 封面居中，四周留 margin 露出渐变背景
        coverBitmap?.let { cover ->
            if (!cover.isRecycled && cover.width > 0 && cover.height > 0) {
                val margin = (size * 0.08f).toInt()
                val availW = size - 2 * margin
                val availH = size - 2 * margin
                val scale = minOf(availW.toFloat() / cover.width, availH.toFloat() / cover.height)
                val drawW = (cover.width * scale).toInt()
                val drawH = (cover.height * scale).toInt()
                val left = margin + (availW - drawW) / 2
                val top = margin + (availH - drawH) / 2
                canvas.drawBitmap(cover, null,
                    Rect(left, top, left + drawW, top + drawH),
                    Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }

        // 3. 底部渐变遮罩：从透明过渡到半透明黑
        val scrimHeight = (size * 0.20f).toInt()
        val scrimTop = (size - scrimHeight).toFloat()
        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        scrimPaint.shader = LinearGradient(
            0f, scrimTop, 0f, size.toFloat(),
            intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(210, 0, 0, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, scrimTop, size.toFloat(), size.toFloat(), scrimPaint)

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
        compositeBitmap: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_reading)
        if (compositeBitmap != null) {
            views.setImageViewBitmap(R.id.widget_composite_bg, compositeBitmap)
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
