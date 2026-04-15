package com.readtrack.domain.repository

import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.model.ImportResult
import kotlinx.coroutines.flow.Flow

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
     * 从 JSON 字符串解析备份数据
     */
    fun parseBackupFromJson(json: String): DataBackup?

    /**
     * 获取导出数据流（用于保存到文件）
     */
    fun getExportJson(): Flow<String>
}
