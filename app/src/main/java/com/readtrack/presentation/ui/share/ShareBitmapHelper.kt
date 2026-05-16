package com.readtrack.presentation.ui.share

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import com.readtrack.presentation.ui.theme.AppShapes
import com.readtrack.presentation.ui.theme.LightColorScheme
import com.readtrack.presentation.ui.theme.Typography
import java.io.File
import java.io.FileOutputStream

fun shareComposable(
    context: Context,
    filename: String,
    content: @Composable () -> Unit,
    onComplete: (() -> Unit)? = null
) {
    val activity = context as? Activity
    if (activity == null) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "分享阅读成就 —— 书迹 App")
        }, "分享阅读成就"))
        onComplete?.invoke()
        return
    }

    val composeView = ComposeView(activity).apply {
        setContent {
            MaterialTheme(
                colorScheme = LightColorScheme,
                typography = Typography,
                shapes = AppShapes,
                content = content
            )
        }
    }

    val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        ?: (activity.window.decorView as ViewGroup)

    val params = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    params.leftMargin = -10000
    root.addView(composeView, params)

    // post 等待 Compose 组合+布局完成，避免 runBlocking 主线程死锁
    composeView.post {
        try {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            composeView.measure(widthSpec, heightSpec)
            composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

            val bitmap = Bitmap.createBitmap(
                composeView.measuredWidth.coerceAtLeast(1),
                composeView.measuredHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            composeView.draw(canvas)

            val file = saveBitmap(activity, bitmap, filename)
            bitmap.recycle()
            shareBitmap(activity, file)
            onComplete?.invoke()
        } finally {
            root.removeView(composeView)
        }
    }
}

private fun saveBitmap(context: Context, bitmap: Bitmap, filename: String): File {
    val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(shareDir, "${filename}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
    }
    return file
}

private fun shareBitmap(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享阅读成就"))
}
