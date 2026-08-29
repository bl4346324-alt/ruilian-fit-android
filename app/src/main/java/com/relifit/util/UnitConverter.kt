package com.relifit.util

import kotlin.math.roundToInt

/**
 * 单位换算：kg / lb（PRD 系统设置-单位切换）
 * 数据库始终以 kg 存储，展示层按设置换算
 */
object UnitConverter {

    private const val KG_TO_LB = 2.20462

    /** 按单位显示重量文案（0 视为自重） */
    fun weightText(weightKg: Double, unit: String, decimal: Boolean = true): String {
        if (weightKg <= 0) return "自重"
        return if (unit == "lb") {
            val lb = weightKg * KG_TO_LB
            if (decimal) format(lb) else lb.roundToInt().toString()
        } else {
            format(weightKg)
        }
    }

    /** 重量步进值：kg 步进 2.5，lb 步进 5 */
    fun stepByUnit(unit: String): Double = if (unit == "lb") 5.0 else 2.5

    /** 步进值换算回 kg（数据库以 kg 存储）：lb 模式下 5 磅 ≈ 2.27kg */
    fun stepKgByUnit(unit: String): Double = if (unit == "lb") 5.0 / KG_TO_LB else 2.5

    /** kg 转 lb */
    fun toLb(kg: Double): Double = kg * KG_TO_LB

    /** 格式化：一位小数，去掉多余的 .0 */
    fun format(v: Double): String {
        val r = (v * 10).roundToInt() / 10.0
        return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
    }
}
