package com.relifit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.relifit.data.local.entity.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * 动作库 DAO
 */
@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY id")
    fun observeAll(): Flow<List<Exercise>>

    /** 按肌群筛选（"全部"时返回全部） */
    @Query("SELECT * FROM exercises WHERE :group = '全部' OR muscleGroup = :group ORDER BY id")
    fun observeByGroup(group: String): Flow<List<Exercise>>

    /** 搜索：按名称 / 英文名 / 肌群 / 器械 模糊匹配 */
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :kw || '%' OR nameEn LIKE '%' || :kw || '%' OR muscleGroup LIKE '%' || :kw || '%' OR equipment LIKE '%' || :kw || '%' ORDER BY id")
    fun search(kw: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Query("SELECT * FROM exercises ORDER BY id")
    suspend fun getAll(): List<Exercise>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    /** 标记离线状态（Demo：云朵图标切换"已离线"） */
    @Query("UPDATE exercises SET offlineAvailable = :offline WHERE id = :id")
    suspend fun updateOffline(id: Long, offline: Boolean)

    /** 收藏切换 */
    @Query("UPDATE exercises SET isFavorite = :fav WHERE id = :id")
    suspend fun updateFavorite(id: Long, fav: Boolean)
}
