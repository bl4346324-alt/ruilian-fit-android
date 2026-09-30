package com.relifit.data.repository

import com.relifit.data.local.dao.DietDao
import com.relifit.data.local.entity.DietGoal
import com.relifit.data.local.entity.FoodItem
import com.relifit.data.local.entity.Meal
import com.relifit.data.local.entity.MealWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 饮食记录仓库：餐次/食物/热量目标（PRD 饮食模块）
 */
class DietRepository(private val dao: DietDao) {

    fun observeDay(dayStart: Long): Flow<List<MealWithItems>> = dao.observeDay(dayStart)

    fun observeGoal(): Flow<DietGoal?> = dao.observeGoal()

    suspend fun getGoal(): DietGoal = dao.getGoal() ?: DietGoal()

    /** 初始化今日四个餐次（早餐/午餐/晚餐/加餐） */
    suspend fun ensureTodayMeals(dayStart: Long) {
        val existing = dao.observeDay(dayStart).first()
        if (existing.isEmpty()) {
            val types = listOf("早餐" to "08:00", "午餐" to "12:30", "晚餐" to "19:00", "加餐" to "15:30")
            types.forEach { (type, time) ->
                dao.insertMeal(Meal(date = dayStart, mealType = type, timeLabel = time))
            }
        }
    }

    suspend fun addFood(mealId: Long, food: FoodItem): Long = dao.insertFood(food.copy(mealId = mealId))

    suspend fun removeFood(foodId: Long) = dao.deleteFood(foodId)

    /** Demo +/− 份量调整：减到 0 份自动删除该食物 */
    suspend fun changeServings(foodId: Long, delta: Int) {
        val item = dao.getFoodById(foodId) ?: return
        val new = (item.servings + delta).coerceAtLeast(0)
        if (new <= 0) dao.deleteFood(foodId) else dao.updateServings(foodId, new)
    }

    /** 复制昨天同一餐次的所有食物到当前餐次 */
    suspend fun copyYesterdayMeal(targetMealId: Long, mealType: String, currentDayStart: Long): Int {
        val yesterday = currentDayStart - 24 * 3600 * 1000L
        val yesterdayMeals = dao.observeDay(yesterday).first()
        val sourceMeal = yesterdayMeals.firstOrNull { it.meal.mealType == mealType } ?: return 0
        if (sourceMeal.items.isEmpty()) return 0
        for (item in sourceMeal.items) {
            dao.insertFood(item.copy(id = 0, mealId = targetMealId))
        }
        return sourceMeal.items.size
    }

    suspend fun dailyKcal(start: Long, end: Long) = dao.dailyKcal(start, end)

    suspend fun saveGoal(goal: DietGoal) = dao.saveGoal(goal)

    // ===== 每日饮水记录 =====
    fun observeWater(date: Long): Flow<com.relifit.data.local.entity.WaterRecord?> = dao.observeWater(date)

    suspend fun addWater(date: Long, deltaMl: Int): Int {
        val current = dao.getWater(date) ?: com.relifit.data.local.entity.WaterRecord(date = date, amountMl = 0, goalMl = 2000)
        val newAmount = (current.amountMl + deltaMl).coerceAtLeast(0)
        dao.saveWater(current.copy(amountMl = newAmount))
        return newAmount
    }

    suspend fun resetWater(date: Long) {
        val current = dao.getWater(date) ?: com.relifit.data.local.entity.WaterRecord(date = date, amountMl = 0, goalMl = 2000)
        dao.saveWater(current.copy(amountMl = 0))
    }

    suspend fun setWaterGoal(date: Long, goalMl: Int) {
        val current = dao.getWater(date) ?: com.relifit.data.local.entity.WaterRecord(date = date, amountMl = 0, goalMl = 2000)
        dao.saveWater(current.copy(goalMl = goalMl.coerceAtLeast(500)))
    }
}
