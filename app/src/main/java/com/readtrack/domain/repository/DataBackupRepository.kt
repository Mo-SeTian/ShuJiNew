package com.readtrack.domain.repository

import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.model.ImportPreview
import com.readtrack.domain.model.ImportResult
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * 数据备份仓库接口
 */
interface DataBackupRepository {
    /**
     * 导出所有数据
     */
    suspend fun exportAllData(): Result<DataBackup>

    /**
     * 导入数据
     * @param backup 备份数据
     * @param clearExisting 是否清空现有数据
     */
    suspend fun importData(backup: DataBackup, clearExisting: Boolean): Result<ImportResult>

    /**
     * 导入前预览数据，评估追加导入会新增/跳过多少内容
     */
    suspend fun previewImport(backup: DataBackup): Result<ImportPreview>

    /**
     * 从 JSON 字符串解析备份数据
     */
    fun parseBackupFromJson(json: String): DataBackup?

    /**
     * 获取导出数据流（用于保存到文件）
     */
    fun getExportJson(): Flow<String>

    /**
     * 导出为 ZIP 文件（包含 JSON 数据 + 封面图片）
     * @return ZIP 文件
     */
    suspend fun exportToZip(): Result<File>

    /**
     * 从 ZIP 文件预览导入
     */
    suspend fun importFromZipForPreview(zipFile: File): Result<ImportPreview>

    /**
     * 从 ZIP 文件导入（包含封面解压）
     */
    suspend fun importFromZip(zipFile: File, clearExisting: Boolean): Result<ImportResult>
}
