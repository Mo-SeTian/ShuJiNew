package com.readtrack.data.local.database

import androidx.room.TypeConverter
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.viewmodel.ProgressType

class Converters {
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
}
