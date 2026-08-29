package com.relifit.data.repository

import com.relifit.data.local.dao.ExerciseDao
import com.relifit.data.local.entity.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * 动作库仓库：分类筛选、搜索、离线标记（Demo 云朵图标交互）
 */
class ExerciseRepository(private val dao: ExerciseDao) {

    fun observeAll(): Flow<List<Exercise>> = dao.observeAll()

    fun observeByGroup(group: String): Flow<List<Exercise>> = dao.observeByGroup(group)

    fun search(kw: String): Flow<List<Exercise>> = dao.search(kw.trim())

    suspend fun getById(id: Long): Exercise? = dao.getById(id)

    suspend fun getAll(): List<Exercise> = dao.getAll()

    /** 离线下载/取消离线（无网络要求，纯本地标记） */
    suspend fun setOffline(id: Long, offline: Boolean) = dao.updateOffline(id, offline)

    suspend fun setFavorite(id: Long, fav: Boolean) = dao.updateFavorite(id, fav)
}
