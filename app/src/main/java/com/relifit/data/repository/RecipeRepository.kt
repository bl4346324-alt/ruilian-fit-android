package com.relifit.data.repository

import com.relifit.data.local.dao.RecipeDao
import com.relifit.data.local.entity.Recipe
import kotlinx.coroutines.flow.Flow

/**
 * 健身菜谱仓库
 */
class RecipeRepository(private val dao: RecipeDao) {

    fun observeAll(): Flow<List<Recipe>> = dao.observeAll()

    fun observeByCategory(category: String): Flow<List<Recipe>> = dao.observeByCategory(category)

    fun search(kw: String): Flow<List<Recipe>> =
        dao.search(kw.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"))

    suspend fun getById(id: Long): Recipe? = dao.getById(id)

    suspend fun getAll(): List<Recipe> = dao.getAll()

    suspend fun addRecipe(recipe: Recipe): Long = dao.insert(recipe)

    suspend fun deleteRecipe(id: Long) = dao.delete(id)

    suspend fun count(): Int = dao.count()

    suspend fun insertAll(list: List<Recipe>) = dao.insertAll(list)
}
