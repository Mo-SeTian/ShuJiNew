package com.readtrack.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExtensionsTest {

    @Test
    fun `buildBackupTimestamp 格式为 yyyyMMdd_HHmmss`() {
        // 2026-07-26 12:34:56 (Calendar 月份从 0 开始)
        val cal = Calendar.getInstance().apply {
            set(2026, 6, 26, 12, 34, 56)
            set(Calendar.MILLISECOND, 0)
        }
        val ts = buildBackupTimestamp(cal.timeInMillis)
        assertEquals("20260726_123456", ts)
    }

    @Test
    fun `getStartOfDay 应清零到当日零点`() {
        val cal = Calendar.getInstance().apply {
            set(2026, 6, 26, 12, 34, 56)
        }
        val start = getStartOfDay(cal.timeInMillis)
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))
    }

    @Test
    fun `getEndOfDay 应设置为当日 235959_999`() {
        val cal = Calendar.getInstance().apply {
            set(2026, 6, 26, 0, 0, 0)
        }
        val end = getEndOfDay(cal.timeInMillis)
        val endCal = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `getDaysBetween 应返回天数差`() {
        val cal1 = Calendar.getInstance().apply { set(2026, 6, 20, 0, 0, 0) }
        val cal2 = Calendar.getInstance().apply { set(2026, 6, 26, 0, 0, 0) }
        assertEquals(6, getDaysBetween(cal1.timeInMillis, cal2.timeInMillis))
    }

    @Test
    fun `formatDecimal 应按精度格式化`() {
        assertEquals("3.1", 3.14159.formatDecimal(1))
        assertEquals("3.14", 3.14159.formatDecimal(2))
        assertEquals("3", 3.14159.formatDecimal(0))
    }

    @Test
    fun `getDayName 应返回中文星期`() {
        assertEquals("周日", 0.getDayName())
        assertEquals("周一", 1.getDayName())
        assertEquals("周六", 6.getDayName())
    }
}
