package com.relifit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.relifit.data.local.entity.DietGoal
import com.relifit.data.local.entity.FoodItem
import com.relifit.data.local.entity.Meal
import com.relifit.data.local.entity.MealWithItems
import kotlinx.coroutines.flow.Flow

/**
 * 饮食记录 DAO（餐次 / 食物 / 热量目标）
 */
@Dao
interface DietDao {

    // ===== 餐次 =====
    @Insert
    suspend fun insertMeal(meal: Meal): Long

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMeal(id: Long)

    /** 某日全部餐次 + 食物 */
    @Transaction
    @Query("SELECT * FROM meals WHERE date = :dayStart ORDER BY id")
    fun observeDay(dayStart: Long): Flow<List<MealWithItems>>

    /** 近 N 天每日摄入热量（7 天柱状图数据） */
    @Query("SELECT m.date AS dayStart, COALESCE(SUM(f.kcal * f.servings),0) AS kcal FROM meals m LEFT JOIN food_items f ON f.mealId = m.id WHERE m.date BETWEEN :start AND :end GROUP BY m.date ORDER BY m.date")
    suspend fun dailyKcal(start: Long, end: Long): List<DailyKcalRow>

    // ===== 食物 =====
    @Insert
    suspend fun insertFood(food: FoodItem): Long

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodById(id: Long): FoodItem?

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFood(id: Long)

    @Query("UPDATE food_items SET servings = :servings WHERE id = :id")
    suspend fun updateServings(id: Long, servings: Int)

    // ===== 目标 =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGoal(goal: DietGoal)

    @Query("SELECT * FROM diet_goal WHERE id = 1")
    fun observeGoal(): Flow<DietGoal?>

    @Query("SELECT * FROM diet_goal WHERE id = 1")
    suspend fun getGoal(): DietGoal?
}

/** 查询结果行：某日摄入热量 */
data class DailyKcalRow(val dayStart: Long, val kcal: Double)
