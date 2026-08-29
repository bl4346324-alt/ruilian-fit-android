package com.relifit.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 时间工具：日期格式化、周/月区间计算（统计页核心）
 */
object TimeUtils {

    /** 格式化日期：2月14日 星期六 */
    fun formatDate(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val weekday = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "星期一"; Calendar.TUESDAY -> "星期二"; Calendar.WEDNESDAY -> "星期三"
            Calendar.THURSDAY -> "星期四"; Calendar.FRIDAY -> "星期五"; Calendar.SATURDAY -> "星期六"
            else -> "星期日"
        }
        return "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日 · $weekday"
    }

    /** 格式化简短日期：2月14日 */
    fun formatShort(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    }

    /** 当日零点 */
    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Material3 DatePicker 返回的是 UTC 零点毫秒 → 转为本地当日零点 */
    fun fromUtcDateMillis(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        val cal = Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }
        return cal.timeInMillis
    }

    /** 本地当日零点 → UTC 零点毫秒（DatePicker 初始值用） */
    fun toUtcDateMillis(localDayStart: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = localDayStart }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
        return utc.timeInMillis
    }

    /** 本周一零点 */
    fun startOfWeek(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val diff = (dow + 5) % 7  // 周一为 0
        cal.add(Calendar.DAY_OF_MONTH, -diff)
        return cal.timeInMillis
    }

    /** 本周日 23:59:59（用 Calendar 计算下周一零点 -1ms，避免固定 7*24h 在夏令时切换周偏差） */
    fun endOfWeek(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = startOfWeek(millis) }
        cal.add(Calendar.DAY_OF_MONTH, 7)
        return cal.timeInMillis - 1
    }

    /** 本月 1 号零点 */
    fun startOfMonth(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** 本月最后一天 23:59:59 */
    fun endOfMonth(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /** 秒 -> mm:ss */
    fun mmss(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    /** 千分位格式化 */
    fun thousands(v: Double): String = String.format(Locale.US, "%,.0f", v)
}
