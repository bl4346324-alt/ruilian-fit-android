package com.relifit.ui.navigation

/**
 * 页面路由常量
 */
object Routes {
    // ===== 顶层 3 个 Tab（首页、饮食、锻炼） =====
    const val HOME_HUB = "home_hub"         // 首页（今日安排 + 数据统计整合）
    const val DIET = "diet"                 // 饮食
    const val WORKOUT_HUB = "workout_hub"   // 锻炼（训练记录 + 动作库整合）

    // 兼容别名
    const val HOME = HOME_HUB
    const val STATS_HUB = HOME_HUB
    const val STATS = HOME_HUB
    const val LIBRARY = WORKOUT_HUB
    const val LOGS = WORKOUT_HUB

    // ===== 子页面（无底部导航） =====
    const val WORKOUT = "workout?planId={planId}&dayId={dayId}&exerciseId={exerciseId}"
    const val WORKOUT_NOARGS = "workout"
    const val EXERCISE = "exercise/{id}"
    const val PLAN = "plan/{planId}"
    const val BODY = "body"
    const val SETTINGS = "settings"
    const val PLANS = "plans"               // 计划列表

    /** 顶层路由集合：用于控制底部导航显隐（三大主界面） */
    val topLevel = listOf(HOME_HUB, DIET, WORKOUT_HUB)

    fun workout(planId: Long?, dayId: Long?, exerciseId: Long?): String {
        return "workout?planId=${planId ?: -1}&dayId=${dayId ?: -1}&exerciseId=${exerciseId ?: -1}"
    }

    fun exercise(id: Long) = "exercise/$id"
    fun plan(id: Long) = "plan/$id"
}
