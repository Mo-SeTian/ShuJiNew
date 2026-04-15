package com.readtrack.data.local.database

import androidx.room.TypeConverter
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.viewmodel.ProgressType
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
            try { json.decodeFromString<BookSnapshot>(it) } catch (e: Exception) { null }
        }
    }

    @TypeConverter
    fun fromBookStatus(status: BookStatus): String {
        return status.name
    }

    @TypeConverter
    fun toBookStatus(status: String): BookStatus {
        return BookStatus.valueOf(status)
    }

    @TypeConverter
    fun fromProgressType(progressType: ProgressType): String {
        return progressType.name
    }

    @TypeConverter
    fun toProgressType(value: String): ProgressType {
        return ProgressType.valueOf(value)
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
}
