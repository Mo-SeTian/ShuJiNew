package com.readtrack.data.local.database

import androidx.room.TypeConverter
import com.readtrack.domain.model.BookStatus

class Converters {
    @TypeConverter
    fun fromBookStatus(status: BookStatus): String {
        return status.name
    }

    @TypeConverter
    fun toBookStatus(status: String): BookStatus {
        return BookStatus.valueOf(status)
    }
}
