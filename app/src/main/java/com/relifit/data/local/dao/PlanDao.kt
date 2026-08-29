package com.relifit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.relifit.data.local.entity.DayWithEntries
import com.relifit.data.local.entity.EntryWithExercise
import com.relifit.data.local.entity.ExerciseEntry
import com.relifit.data.local.entity.WorkoutDay
import com.relifit.data.local.entity.WorkoutPlan
import kotlinx.coroutines.flow.Flow

/**
 * 训练计划 DAO（计划 / 训练日 / 动作条目）
 */
@Dao
interface PlanDao {

    // ===== 计划 =====
    @Query("SELECT * FROM plans ORDER BY isTemplate DESC, id")
    fun observeAllPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM plans WHERE id = :id")
    fun observePlan(id: Long): Flow<WorkoutPlan?>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getPlan(id: Long): WorkoutPlan?

    @Query("SELECT * FROM plans WHERE isTemplate = 1")
    suspend fun getTemplates(): List<WorkoutPlan>

    @Insert
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Query("UPDATE plans SET type = :type, updatedAt = :now WHERE id = :id")
    suspend fun updateType(id: Long, type: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: Long)

    // ===== 训练日 =====
    @Query("SELECT * FROM workout_days WHERE planId = :planId ORDER BY dayIndex")
    fun observeDays(planId: Long): Flow<List<WorkoutDay>>

    @Query("SELECT * FROM workout_days WHERE planId = :planId ORDER BY dayIndex")
    suspend fun getDays(planId: Long): List<WorkoutDay>

    @Query("SELECT * FROM workout_days WHERE id = :id")
    suspend fun getDay(id: Long): WorkoutDay?

    @Insert
    suspend fun insertDay(day: WorkoutDay): Long

    @Query("DELETE FROM workout_days WHERE id = :id")
    suspend fun deleteDay(id: Long)

    /** 训练日 + 动作条目（事务查询） */
    @Transaction
    @Query("SELECT * FROM workout_days WHERE planId = :planId ORDER BY dayIndex")
    fun observeDaysWithEntries(planId: Long): Flow<List<DayWithEntries>>

    // ===== 动作条目 =====
    @Query("SELECT * FROM exercise_entries WHERE workoutDayId = :dayId ORDER BY sortOrder")
    fun observeEntries(dayId: Long): Flow<List<ExerciseEntry>>

    @Query("SELECT * FROM exercise_entries WHERE workoutDayId = :dayId ORDER BY sortOrder")
    suspend fun getEntries(dayId: Long): List<ExerciseEntry>

    @Insert
    suspend fun insertEntry(entry: ExerciseEntry): Long

    @Query("DELETE FROM exercise_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("UPDATE exercise_entries SET targetSets = :sets, targetReps = :reps, restSec = :restSec, targetWeight = :weight WHERE id = :id")
    suspend fun updateEntry(id: Long, sets: Int, reps: Int, restSec: Int, weight: Double?)

    @Query("SELECT MAX(sortOrder) FROM exercise_entries WHERE workoutDayId = :dayId")
    suspend fun maxSortOrder(dayId: Long): Int?

    /** 训练日动作列表（含动作详情） */
    @Transaction
    @Query("SELECT * FROM exercise_entries WHERE workoutDayId = :dayId ORDER BY sortOrder")
    fun observeEntriesWithExercise(dayId: Long): Flow<List<EntryWithExercise>>
}
