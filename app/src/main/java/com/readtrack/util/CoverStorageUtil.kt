package com.readtrack.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 封面图片持久化存储工具。
 *
 * 用户通过「本地导入」选择的图片会得到一个 content:// URI（如 content://media/...），
 * 该 URI 依赖系统授予的临时读取权限，重启应用后权限失效，无法加载。
 *
 * 此工具将 content:// URI 的图片复制到应用内部私有目录 [filesDir/covers/]，
 * 确保重启后图片依然可访问。
 */
@Singleton
class CoverStorageUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val coversDir: File
        get() = File(context.filesDir, "covers").also { it.mkdirs() }

    /**
     * 将指定的 URI 持久化到本地存储。
     *
     * - 如果 [uriStr] 是 content:// 开头 → 复制到内部目录，返回内部文件路径
     * - 如果 [uriStr] 是 file:// 开头且不在内部目录 → 复制到内部目录
     * - 如果已经是内部 cover 路径 → 不变
     * - 如果是 https?:// 网络 URL → 不变（网络图片由 Coil 缓存）
     *
     * @return 持久化后的本地文件路径，如果传入的是网络 URL 或无法处理则原样返回
     */
    fun persistCover(uriStr: String?): String? {
        if (uriStr.isNullOrBlank()) return uriStr

        // 网络 URL 不需要处理，Coil 会缓存
        if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
            return uriStr
        }

        // 特殊协议封面（emoji, color 等）不需要处理
        if (uriStr.startsWith("emoji://") || uriStr.startsWith("color://")) {
            return uriStr
        }

        // 如果已经是内部 covers 目录的文件，直接返回
        if (uriStr.startsWith(coversDir.absolutePath)) {
            return uriStr
        }

        // 将 URI 复制到内部存储
        return try {
            val uri = Uri.parse(uriStr)
            val fileName = "cover_${UUID.randomUUID()}.jpg"
            val destFile = File(coversDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                // 先解码为 Bitmap 再压缩写入，保证是标准 JPEG
                val bitmap = BitmapFactory.decodeStream(input)
                if (bitmap != null) {
                    FileOutputStream(destFile).use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                    }
                    bitmap.recycle()
                } else {
                    // 如果 BitmapFactory 解码失败（罕见情况），回退到流复制
                    input.close()
                    context.contentResolver.openInputStream(uri)?.use { retryInput ->
                        FileOutputStream(destFile).use { output ->
                            retryInput.copyTo(output)
                        }
                    }
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            // 复制失败时返回原始 URI，至少尝试保留现有行为
            uriStr
        }
    }

    /**
     * 删除指定路径的封面文件（如果它位于内部 covers 目录）。
     */
    fun deleteCover(coverPath: String?) {
        if (coverPath.isNullOrBlank()) return
        if (!coverPath.startsWith(coversDir.absolutePath)) return
        File(coverPath).delete()
    }
}
