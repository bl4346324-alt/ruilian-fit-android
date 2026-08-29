package com.relifit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.relifit.data.local.entity.BodyMetric
import kotlinx.coroutines.flow.Flow

/**
 * 身体数据 DAO（PRD 身体数据模块）
 */
@Dao
interface BodyMetricDao {

    @Query("SELECT * FROM body_metrics ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics ORDER BY date DESC")
    suspend fun getAll(): List<BodyMetric>

    @Query("SELECT * FROM body_metrics ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): BodyMetric?

    /** 同日记录自动覆盖（date 唯一约束 + REPLACE） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: BodyMetric): Long

    @Query("DELETE FROM body_metrics WHERE id = :id")
    suspend fun delete(id: Long)
}
