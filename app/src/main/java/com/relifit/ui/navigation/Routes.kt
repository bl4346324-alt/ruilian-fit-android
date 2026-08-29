package com.relifit.ui.navigation

/**
 * 页面路由常量
 */
object Routes {
    // ===== 顶层 5 个 Tab（带底部导航） =====
    const val HOME = "home"
    const val LIBRARY = "library"
    const val LOGS = "logs"
    const val DIET = "diet"
    const val STATS = "stats"

    // ===== 子页面（无底部导航） =====
    const val WORKOUT = "workout?planId={planId}&dayId={dayId}&exerciseId={exerciseId}"
    const val WORKOUT_NOARGS = "workout"
    const val EXERCISE = "exercise/{id}"
    const val PLAN = "plan/{planId}"
    const val BODY = "body"
    const val SETTINGS = "settings"
    const val PLANS = "plans"               // 计划列表（训练记录页入口）

    /** 顶层路由集合：用于控制底部导航显隐 */
    val topLevel = listOf(HOME, LIBRARY, LOGS, DIET, STATS)

    fun workout(planId: Long?, dayId: Long?, exerciseId: Long?): String {
        return "workout?planId=${planId ?: -1}&dayId=${dayId ?: -1}&exerciseId=${exerciseId ?: -1}"
    }

    fun exercise(id: Long) = "exercise/$id"
    fun plan(id: Long) = "plan/$id"
}
