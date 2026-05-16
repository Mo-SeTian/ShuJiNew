package com.readtrack.presentation.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun shareComposable(
    context: Context,
    filename: String,
    content: @Composable () -> Unit
) {
    val bitmap = captureComposable(context, content)
    val file = saveBitmap(context, bitmap, filename)
    bitmap.recycle()
    shareBitmap(context, file)
}

private fun captureComposable(
    context: Context,
    content: @Composable () -> Unit
): Bitmap {
    val composeView = ComposeView(context).apply {
        setContent(content)
        // 测量和布局
        val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        measure(widthSpec, heightSpec)
        layout(0, 0, measuredWidth, measuredHeight)

        // 设置固定大小 LayoutParams
        layoutParams = ViewGroup.LayoutParams(measuredWidth, measuredHeight)
    }

    // 创建 bitmap 并绘制
    val bitmap = Bitmap.createBitmap(
        composeView.measuredWidth.coerceAtLeast(1),
        composeView.measuredHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    composeView.draw(canvas)

    return bitmap
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
