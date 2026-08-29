package com.relifit.data.repository

import com.relifit.data.local.dao.WorkoutDao
import com.relifit.data.local.entity.LogWithSets
import com.relifit.data.local.entity.SetRecord
import com.relifit.data.local.entity.WorkoutLog
import kotlinx.coroutines.flow.Flow

/**
 * 训练记录仓库：保存训练会话、历史查询、周/月统计（PRD 数据统计核心）
 */
class WorkoutRepository(private val dao: WorkoutDao) {

    fun observeLogsWithSets(): Flow<List<LogWithSets>> = dao.observeLogsWithSets()

    suspend fun getLogsBetween(start: Long, end: Long): List<WorkoutLog> = dao.getLogsBetween(start, end)

    /** 保存一次完整训练：插入 WorkoutLog + 全部 SetRecord */
    suspend fun saveWorkout(log: WorkoutLog, sets: List<SetRecord>): Long {
        val logId = dao.insertLog(log)
        if (sets.isNotEmpty()) {
            dao.insertSets(sets.map { it.copy(logId = logId) })
        }
        return logId
    }

    suspend fun deleteLog(id: Long) = dao.deleteLog(id)

    // ===== 统计 =====
    suspend fun countInRange(start: Long, end: Long): Int = dao.countInRange(start, end)

    suspend fun sumDurationInRange(start: Long, end: Long): Long = dao.sumDurationInRange(start, end)

    suspend fun sumVolumeInRange(start: Long, end: Long): Double = dao.sumVolumeInRange(start, end)

    suspend fun avgDurationInRange(start: Long, end: Long): Double = dao.avgDurationInRange(start, end)

    suspend fun maxWeightPerLog(exerciseId: Long, start: Long, end: Long) = dao.maxWeightPerLog(exerciseId, start, end)

    suspend fun muscleDistribution(start: Long, end: Long) = dao.muscleDistribution(start, end)

    suspend fun dailyFrequency(start: Long, end: Long) = dao.dailyFrequency(start, end)

    fun observeSetsOfExercise(exerciseId: Long): Flow<List<SetRecord>> = dao.observeSetsOfExercise(exerciseId)
}
