package com.relifit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.relifit.data.local.entity.LogWithSets
import com.relifit.data.local.entity.SetRecord
import com.relifit.data.local.entity.WorkoutLog
import kotlinx.coroutines.flow.Flow

/**
 * 训练记录 / 训练组 DAO（含统计查询，PRD 数据统计模块核心）
 */
@Dao
interface WorkoutDao {

    // ===== 训练记录 =====
    @Insert
    suspend fun insertLog(log: WorkoutLog): Long

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("SELECT * FROM workout_logs ORDER BY date DESC")
    fun observeLogs(): Flow<List<WorkoutLog>>

    /** 训练记录 + 全部组（可折叠卡片展开用） */
    @Transaction
    @Query("SELECT * FROM workout_logs ORDER BY date DESC")
    fun observeLogsWithSets(): Flow<List<LogWithSets>>

    /** 指定时间区间内的训练记录 */
    @Query("SELECT * FROM workout_logs WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun getLogsBetween(start: Long, end: Long): List<WorkoutLog>

    // ===== 训练组 =====
    @Insert
    suspend fun insertSets(sets: List<SetRecord>)

    @Query("SELECT * FROM set_records WHERE logId = :logId ORDER BY setIndex")
    suspend fun getSets(logId: Long): List<SetRecord>

    // ===== 统计查询（周/月报表）=====
    @Query("SELECT COUNT(*) FROM workout_logs WHERE date BETWEEN :start AND :end")
    suspend fun countInRange(start: Long, end: Long): Int

    @Query("SELECT COALESCE(SUM(durationMin),0) FROM workout_logs WHERE date BETWEEN :start AND :end")
    suspend fun sumDurationInRange(start: Long, end: Long): Long

    @Query("SELECT COALESCE(SUM(totalVolumeKg),0) FROM workout_logs WHERE date BETWEEN :start AND :end")
    suspend fun sumVolumeInRange(start: Long, end: Long): Double

    @Query("SELECT COALESCE(AVG(durationMin),0) FROM workout_logs WHERE date BETWEEN :start AND :end")
    suspend fun avgDurationInRange(start: Long, end: Long): Double

    /** 某动作在时间区间内的历史最大重量（重量进步折线图） */
    @Query("SELECT MAX(weightKg) FROM set_records WHERE exerciseId = :exerciseId AND completed = 1 AND logId IN (SELECT id FROM workout_logs WHERE date BETWEEN :start AND :end)")
    suspend fun maxWeightInRange(exerciseId: Long, start: Long, end: Long): Double?

    /** 某动作按时间排序的每组记录（折线图数据点） */
    @Query("SELECT * FROM set_records WHERE exerciseId = :exerciseId AND completed = 1 ORDER BY logId, setIndex")
    fun observeSetsOfExercise(exerciseId: Long): Flow<List<SetRecord>>

    /** 某动作按时间区间内每组最高重量（折线图，带训练日期） */
    @Query("SELECT s.logId AS logId, MAX(s.weightKg) AS w, MIN(l.date) AS date FROM set_records s JOIN workout_logs l ON l.id = s.logId WHERE s.exerciseId = :exerciseId AND s.completed = 1 AND s.weightKg > 0 AND l.date BETWEEN :start AND :end GROUP BY s.logId ORDER BY s.logId")
    suspend fun maxWeightPerLog(exerciseId: Long, start: Long, end: Long): List<MaxWeightRow>

    /** 肌群训练次数分布（统计页肌群覆盖进度条） */
    @Query("SELECT e.muscleGroup AS muscleGroup, COUNT(s.id) AS cnt FROM set_records s JOIN exercises e ON e.id = s.exerciseId WHERE s.logId IN (SELECT id FROM workout_logs WHERE date BETWEEN :start AND :end) GROUP BY e.muscleGroup ORDER BY cnt DESC")
    suspend fun muscleDistribution(start: Long, end: Long): List<MuscleCountRow>

    /** 训练频率：按自然日分组统计训练次数（柱状图） */
    @Query("SELECT date, COUNT(*) AS cnt FROM workout_logs WHERE date BETWEEN :start AND :end GROUP BY date ORDER BY date")
    suspend fun dailyFrequency(start: Long, end: Long): List<DailyCountRow>
}

/** 查询结果行：某次训练的最大重量 */
data class MaxWeightRow(val logId: Long, val w: Double, val date: Long)

/** 查询结果行：肌群统计 */
data class MuscleCountRow(val muscleGroup: String, val cnt: Int)

/** 查询结果行：每日训练次数 */
data class DailyCountRow(val date: Long, val cnt: Int)
