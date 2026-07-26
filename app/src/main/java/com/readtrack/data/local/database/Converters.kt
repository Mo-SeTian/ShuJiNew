package com.readtrack.data.local.database

import androidx.room.TypeConverter
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.BookType
import com.readtrack.domain.model.ProgressType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromBookSnapshot(snapshot: BookSnapshot?): String? {
        return snapshot?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toBookSnapshot(jsonStr: String?): BookSnapshot? {
        return jsonStr?.let {
            try {
                json.decodeFromString<BookSnapshot>(it)
            } catch (e: Exception) {
                android.util.Log.w("Converters", "BookSnapshot 反序列化失败: ${e.message}")
                null
            }
        }
    }

    @TypeConverter
    fun fromBookStatus(status: BookStatus): String {
        return status.name
    }

    @TypeConverter
    fun toBookStatus(status: String): BookStatus {
        return try {
            BookStatus.valueOf(status)
        } catch (e: Exception) {
            android.util.Log.w("Converters", "未知 BookStatus '$status'，回退到 WANT_TO_READ")
            BookStatus.WANT_TO_READ
        }
    }

    @TypeConverter
    fun fromProgressType(progressType: ProgressType): String {
        return progressType.name
    }

    @TypeConverter
    fun toProgressType(value: String): ProgressType {
        return try {
            ProgressType.valueOf(value)
        } catch (e: Exception) {
            android.util.Log.w("Converters", "未知 ProgressType '$value'，回退到 PAGE")
            ProgressType.PAGE
        }
    }

    @TypeConverter
    fun fromRecordType(recordType: RecordType): String {
        return recordType.name
    }

    @TypeConverter
    fun toRecordType(value: String): RecordType {
        return try {
            RecordType.valueOf(value)
        } catch (e: Exception) {
            RecordType.NORMAL
        }
    }

    @TypeConverter
    fun fromBookType(bookType: BookType): String {
        return bookType.name
    }

    @TypeConverter
    fun toBookType(value: String): BookType {
        return try {
            BookType.valueOf(value)
        } catch (e: Exception) {
            BookType.NOVEL
        }
    }
}
