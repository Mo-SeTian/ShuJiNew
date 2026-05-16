package com.readtrack.presentation.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import com.readtrack.data.local.ThemeMode
import com.readtrack.presentation.ui.theme.ReadTrackTheme
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
        setContent {
            ReadTrackTheme(themeMode = ThemeMode.SYSTEM, dynamicColor = false) {
                content()
            }
        }
    }

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
