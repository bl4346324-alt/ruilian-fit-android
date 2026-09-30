package com.relifit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.relifit.data.local.entity.Recipe
import kotlinx.coroutines.flow.Flow

/**
 * 健身菜谱 DAO
 */
@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY isCustom DESC, id ASC")
    fun observeAll(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE category = :category ORDER BY id ASC")
    fun observeByCategory(category: String): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :kw || '%' OR ingredients LIKE '%' || :kw || '%'")
    fun search(kw: String): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): Recipe?

    @Query("SELECT * FROM recipes ORDER BY id ASC")
    suspend fun getAll(): List<Recipe>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: Recipe): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<Recipe>)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int
}
