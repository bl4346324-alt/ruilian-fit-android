package com.relifit.data.repository

import com.relifit.data.local.dao.BodyMetricDao
import com.relifit.data.local.entity.BodyMetric
import kotlinx.coroutines.flow.Flow

/**
 * 身体数据仓库：体重/体脂/围度录入与趋势查询（PRD 身体数据模块）
 */
class BodyRepository(private val dao: BodyMetricDao) {

    fun observeAll(): Flow<List<BodyMetric>> = dao.observeAll()

    suspend fun getAll(): List<BodyMetric> = dao.getAll()

    suspend fun getLatest(): BodyMetric? = dao.getLatest()

    suspend fun insert(metric: BodyMetric): Long = dao.insert(metric)

    suspend fun delete(id: Long) = dao.delete(id)
}
