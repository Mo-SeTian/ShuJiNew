package com.readtrack.util

import java.text.SimpleDateFormat
import java.util.*

fun Long.toDateString(pattern: String = "yyyy-MM-dd"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.toTimeString(pattern: String = "HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.toDateTimeString(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(this))
}

fun Double.formatDecimal(decimals: Int = 1): String {
    return "%.${decimals}f".format(this)
}

fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

fun getDaysBetween(start: Long, end: Long): Int {
    return ((end - start) / (1000 * 60 * 60 * 24)).toInt()
}

fun Int.getDayName(): String {
    val dayNames = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    return dayNames.getOrElse(this % 7) { "" }
}
