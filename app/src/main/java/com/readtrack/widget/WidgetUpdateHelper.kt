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
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoaderFactory
import coil.request.ImageRequest
import coil.request.SuccessResult
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
                val color = extractCoverColor(context, book?.coverPath) ?: Color.rgb(80, 100, 180)
                val cover = loadCoverBitmap(context, book?.coverPath)
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

    private suspend fun extractCoverColor(context: Context, coverPath: String?): Int? {
        if (coverPath.isNullOrBlank()) return null
        // 跳过特殊协议封面
        if (coverPath.startsWith("emoji://") || coverPath.startsWith("color://")) return null

        return try {
            val source = loadImageAsBitmap(context, coverPath, 64) ?: return null
            val palette = Palette.from(source).generate()
            val swatch = palette.vibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch
            if (!source.isRecycled) source.recycle()
            swatch?.rgb
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadCoverBitmap(context: Context, coverPath: String?): Bitmap? {
        if (coverPath.isNullOrBlank()) return null
        // 跳过特殊协议封面
        if (coverPath.startsWith("emoji://") || coverPath.startsWith("color://")) return null

        return try {
            loadImageAsBitmap(context, coverPath, 300)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 统一加载图片：本地文件用 BitmapFactory，网络/URI 用 Coil
     */
    private suspend fun loadImageAsBitmap(context: Context, path: String, size: Int): Bitmap? {
        return when {
            path.startsWith("http://") || path.startsWith("https://") -> {
                loadWithCoil(context, path, size)
            }
            path.startsWith("content://") || path.startsWith("file://") -> {
                loadWithCoil(context, path, size)
            }
            else -> {
                // 本地绝对路径
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, opts)
                val maxDim = maxOf(opts.outWidth, opts.outHeight)
                if (maxDim <= 0) return null
                opts.inJustDecodeBounds = false
                opts.inSampleSize = when {
                    maxDim > 2400 -> 8
                    maxDim > 1200 -> 4
                    maxDim > 600 -> 2
                    else -> 1
                }
                BitmapFactory.decodeFile(path, opts)
            }
        }
    }

    private suspend fun loadWithCoil(context: Context, url: String, size: Int): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(size)
            .allowHardware(false)
            .build()
        val imageLoader = (context.applicationContext as ImageLoaderFactory).newImageLoader()
        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> result.drawable.toBitmap()
            else -> null
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

        // 1. 高可见度渐变背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(brightenColor(baseColor, 1.6f), darkenColor(baseColor, 0.15f)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // 2. 封面：centerCrop 裁剪，portrait 比例（接近 2:3）
        coverBitmap?.let { cover ->
            if (!cover.isRecycled && cover.width > 0 && cover.height > 0) {
                val coverW = (size * 0.52f).toInt()
                val coverH = (size * 0.74f).toInt()
                val coverLeft = (size * 0.06f).toInt()
                val coverTop = (size * 0.08f).toInt()
                val dstRect = Rect(coverLeft, coverTop, coverLeft + coverW, coverTop + coverH)

                val scale = maxOf(coverW.toFloat() / cover.width, coverH.toFloat() / cover.height)
                val srcW = (coverW / scale).toInt()
                val srcH = (coverH / scale).toInt()
                val srcLeft = (cover.width - srcW) / 2
                val srcTop = (cover.height - srcH) / 2
                val srcRect = Rect(srcLeft, srcTop, srcLeft + srcW, srcTop + srcH)

                // 阴影/光晕层：略大、低透明度，制造边缘柔化过渡
                val shadowPaint = Paint(Paint.FILTER_BITMAP_FLAG)
                shadowPaint.alpha = 50
                val shadowPad = 24
                val shadowRect = Rect(
                    dstRect.left - shadowPad,
                    dstRect.top - shadowPad,
                    dstRect.right + shadowPad,
                    dstRect.bottom + shadowPad
                )
                canvas.drawBitmap(cover, srcRect, shadowRect, shadowPaint)

                // 实际封面
                canvas.drawBitmap(cover, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }

        // 3. 底部渐变遮罩
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
