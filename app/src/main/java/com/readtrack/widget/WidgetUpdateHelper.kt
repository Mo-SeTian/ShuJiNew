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
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
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
import com.readtrack.domain.model.BookStatus
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
                createWidgetComposite(color, cover, book).also {
                    cover?.recycle()
                }
            }
            val remoteViews = buildRemoteViews(context, book, appWidgetId, compositeBitmap)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            compositeBitmap.recycle()
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

        var source: Bitmap? = null
        return try {
            source = loadImageAsBitmap(context, coverPath, 64) ?: return null
            val palette = Palette.from(source).generate()
            val swatch = palette.vibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch
            swatch?.rgb
        } catch (e: Exception) {
            null
        } finally {
            source?.let { if (!it.isRecycled) it.recycle() }
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

    private fun createWidgetComposite(baseColor: Int, coverBitmap: Bitmap?, book: BookEntity?): Bitmap {
        val size = 400
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 柔和渐变背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(brightenColor(baseColor, 1.4f), darkenColor(baseColor, 0.25f)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // 2. 封面：左侧偏上，48% × 68%
        val coverLeft = (size * 0.06f).toInt()
        val coverTop = (size * 0.10f).toInt()
        val coverW = (size * 0.48f).toInt()
        val coverH = (size * 0.68f).toInt()
        val coverDstRect = Rect(coverLeft, coverTop, coverLeft + coverW, coverTop + coverH)

        coverBitmap?.let { cover ->
            if (!cover.isRecycled && cover.width > 0 && cover.height > 0) {
                val scale = maxOf(coverW.toFloat() / cover.width, coverH.toFloat() / cover.height)
                val srcW = (coverW / scale).toInt()
                val srcH = (coverH / scale).toInt()
                val srcLeft = (cover.width - srcW) / 2
                val srcTop = (cover.height - srcH) / 2
                val srcRect = Rect(srcLeft, srcTop, srcLeft + srcW, srcTop + srcH)

                // 封面阴影（圆角矩形，与封面匹配）
                val cornerRadius = 10f
                val shadowLayers = listOf(14 to 20, 8 to 28, 3 to 16)
                for ((pad, alpha) in shadowLayers) {
                    val sp = Paint(Paint.ANTI_ALIAS_FLAG)
                    sp.color = Color.argb(alpha, 0, 0, 0)
                    val shadowRadius = cornerRadius + pad
                    canvas.drawRoundRect(
                        RectF(
                            (coverDstRect.left - pad).toFloat(),
                            (coverDstRect.top - pad).toFloat(),
                            (coverDstRect.right + pad).toFloat(),
                            (coverDstRect.bottom + pad).toFloat()
                        ),
                        shadowRadius, shadowRadius, sp
                    )
                }

                // 封面圆角裁剪
                canvas.save()
                canvas.clipPath(android.graphics.Path().apply {
                    addRoundRect(
                        RectF(coverDstRect), cornerRadius, cornerRadius,
                        android.graphics.Path.Direction.CW
                    )
                })
                canvas.drawBitmap(cover, srcRect, coverDstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            }
        }

        // 3. 右侧横排文字：封面右侧到控件边缘，顶部与封面上沿对齐
        val textStartX = (coverLeft + coverW + (size * 0.05f).toInt()).toFloat()
        val textMaxWidth = (size - textStartX - (size * 0.04f).toInt()).toInt()

        if (book != null) {
            // 书名（最多2行，居中，省略号）
            val titlePaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 30f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val titleLayout = StaticLayout.Builder.obtain(
                book.title, 0, book.title.length, titlePaint, textMaxWidth
            ).setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END).build()

            // 作者（最多1行，居中，省略号）
            val authorLayout = if (!book.author.isNullOrBlank()) {
                val authorTextPaint = TextPaint().apply {
                    color = Color.argb(180, 255, 255, 255)
                    textSize = 22f
                    isAntiAlias = true
                }
                StaticLayout.Builder.obtain(
                    book.author, 0, book.author.length, authorTextPaint, textMaxWidth
                ).setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END).build()
            } else null

            // 进度百分比
            val percent = calculateProgressPercent(book)
            val percentText = "$percent%"
            val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 40f
                isFakeBoldText = true
            }
            val percentW = percentPaint.measureText(percentText)

            // 书名顶部与封面上沿对齐
            val gapTitleAuthor = if (authorLayout != null) 6 else 0
            val gapAuthorPercent = if (authorLayout != null) 12 else 6
            val textTopY = coverTop

            // 绘制书名
            canvas.save()
            canvas.translate(textStartX, textTopY.toFloat())
            titleLayout.draw(canvas)
            canvas.restore()

            var y = textTopY + titleLayout.height + gapTitleAuthor

            // 绘制作者
            if (authorLayout != null) {
                canvas.save()
                canvas.translate(textStartX, y.toFloat())
                authorLayout.draw(canvas)
                canvas.restore()
                y += authorLayout.height + gapAuthorPercent
            } else {
                y += gapAuthorPercent
            }

            // 绘制进度百分比（居中）
            val percentX = textStartX + (textMaxWidth - percentW) / 2
            canvas.drawText(percentText, percentX, y + percentPaint.textSize, percentPaint)
        } else {
            // 空状态：居中提示
            val emptyText = "选择一本书"
            val emptyPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 28f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val emptyLayout = StaticLayout.Builder.obtain(
                emptyText, 0, emptyText.length, emptyPaint, textMaxWidth
            ).setMaxLines(2).build()

            canvas.save()
            canvas.translate(textStartX, coverTop.toFloat())
            emptyLayout.draw(canvas)
            canvas.restore()

            val hintTop = coverTop + emptyLayout.height + 8
            val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(180, 255, 255, 255)
                textSize = 22f
            }
            canvas.drawText("点击设置", textStartX, hintTop + hintPaint.textSize, hintPaint)
        }

        // 5. 底部渐变遮罩（更轻柔）
        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        scrimPaint.shader = LinearGradient(
            0f, size * 0.72f, 0f, size.toFloat(),
            intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(160, 0, 0, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, size * 0.72f, size.toFloat(), size.toFloat(), scrimPaint)

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

        // 书名与进度已绘制在 composite bitmap 上，隐藏原 TextView 避免重叠
        views.setViewVisibility(R.id.widget_book_title, android.view.View.GONE)
        views.setViewVisibility(R.id.widget_progress_text, android.view.View.GONE)

        // 主体点击：有书籍→详情页，无书籍→小组件设置页
        val openIntent = if (book != null) {
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_BOOK_DETAIL
                putExtra(MainActivity.EXTRA_BOOK_ID, book.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_WIDGET_SETTINGS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId * 3 + 2,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, openPendingIntent)

        // 右下角按钮：阅读中→记录进度，其他→提示不可记录
        if (book != null) {
            if (book.status == BookStatus.READING) {
                val recordIntent = WidgetQuickRecordActivity.createIntent(context, book.id)
                views.setOnClickPendingIntent(
                    R.id.widget_record_button,
                    PendingIntent.getActivity(
                        context,
                        appWidgetId * 3 + 1,
                        recordIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                // 非在读书籍也走 WidgetQuickRecordActivity，由其内部 toast 提醒用户
                views.setOnClickPendingIntent(
                    R.id.widget_record_button,
                    PendingIntent.getActivity(
                        context,
                        appWidgetId * 3 + 1,
                        WidgetQuickRecordActivity.createIntent(context, book.id),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        } else {
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
