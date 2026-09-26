package com.relifit.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    @Test
    fun testMmss() {
        assertEquals("00:00", TimeUtils.mmss(0))
        assertEquals("01:05", TimeUtils.mmss(65))
        assertEquals("10:00", TimeUtils.mmss(600))
    }

    @Test
    fun testStartOfDay() {
        val now = System.currentTimeMillis()
        val start = TimeUtils.startOfDay(now)
        val cal = Calendar.getInstance().apply { timeInMillis = start }

        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
        assertTrue(start <= now)
        assertTrue(now - start < 24 * 3600 * 1000L)
    }

    @Test
    fun testStartOfWeek() {
        val now = System.currentTimeMillis()
        val start = TimeUtils.startOfWeek(now)
        val cal = Calendar.getInstance().apply { timeInMillis = start }

        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testEndOfWeek() {
        val now = System.currentTimeMillis()
        val end = TimeUtils.endOfWeek(now)
        val cal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(Calendar.SUNDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
        assertEquals(59, cal.get(Calendar.SECOND))
    }

    @Test
    fun testThousands() {
        assertEquals("1,000", TimeUtils.thousands(1000.0))
        assertEquals("12,345", TimeUtils.thousands(12345.0))
    }
}
