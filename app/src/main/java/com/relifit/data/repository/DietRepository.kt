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

    suspend fun dailyKcal(start: Long, end: Long) = dao.dailyKcal(start, end)

    suspend fun saveGoal(goal: DietGoal) = dao.saveGoal(goal)
}
