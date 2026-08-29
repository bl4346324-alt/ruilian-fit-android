package com.relifit.data.repository

import com.relifit.data.local.dao.PlanDao
import com.relifit.data.local.entity.DayWithEntries
import com.relifit.data.local.entity.EntryWithExercise
import com.relifit.data.local.entity.ExerciseEntry
import com.relifit.data.local.entity.WorkoutDay
import com.relifit.data.local.entity.WorkoutPlan
import kotlinx.coroutines.flow.Flow

/**
 * 训练计划仓库：模板只读、复制生成自定义计划、训练日与动作条目管理（PRD P0）
 */
class PlanRepository(private val dao: PlanDao) {

    fun observeAllPlans(): Flow<List<WorkoutPlan>> = dao.observeAllPlans()

    fun observePlan(id: Long): Flow<WorkoutPlan?> = dao.observePlan(id)

    fun observeDaysWithEntries(planId: Long): Flow<List<DayWithEntries>> = dao.observeDaysWithEntries(planId)

    fun observeEntriesWithExercise(dayId: Long): Flow<List<EntryWithExercise>> = dao.observeEntriesWithExercise(dayId)

    suspend fun getPlan(id: Long): WorkoutPlan? = dao.getPlan(id)

    suspend fun getDay(id: Long): WorkoutDay? = dao.getDay(id)

    /** 新建自定义计划（含类型分类：力量/核心/有氧/恢复） */
    suspend fun createPlan(name: String, type: String): Long {
        val now = System.currentTimeMillis()
        return dao.insertPlan(
            WorkoutPlan(
                name = name, type = type, isTemplate = false,
                cycleWeeks = 4, targetDurationMin = 60, daysPerWeek = 3,
                createdAt = now, updatedAt = now
            )
        )
    }

    /** 修改计划类型 */
    suspend fun updatePlanType(id: Long, type: String) = dao.updateType(id, type)

    /** 复制模板生成自定义计划（复制全部训练日与动作条目，名称加"（副本）"，DAO 事务原子提交） */
    suspend fun copyTemplate(template: WorkoutPlan) {
        dao.copyTemplateTx(template)
    }

    /** 新增训练日（同步更新计划 daysPerWeek） */
    suspend fun addDay(planId: Long, dayIndex: Int, name: String, restSec: Int): Long {
        val id = dao.insertDay(WorkoutDay(planId = planId, dayIndex = dayIndex, name = name, defaultRestSec = restSec))
        syncDaysPerWeek(planId)
        return id
    }

    /** 删除训练日（同步更新计划 daysPerWeek） */
    suspend fun deleteDay(dayId: Long) {
        val day = dao.getDay(dayId) ?: return
        dao.deleteDay(dayId)
        syncDaysPerWeek(day.planId)
    }

    /** 训练日数量回写计划元数据（列表卡片"每周 N 练"与实际训练日数保持一致） */
    private suspend fun syncDaysPerWeek(planId: Long) {
        dao.updateDaysPerWeek(planId, dao.getDays(planId).size.coerceAtLeast(1))
    }

    /** 新增动作条目（默认 3 组 × 10 次，组间 90s 与设置默认一致） */
    suspend fun addEntry(dayId: Long, exerciseId: Long, sets: Int = 3, reps: Int = 10, restSec: Int = 90) {
        val maxSort = dao.maxSortOrder(dayId) ?: -1
        dao.insertEntry(
            ExerciseEntry(
                workoutDayId = dayId, exerciseId = exerciseId, sortOrder = maxSort + 1,
                targetSets = sets, targetReps = reps, restSec = restSec
            )
        )
    }

    suspend fun removeEntry(entryId: Long) = dao.deleteEntry(entryId)

    suspend fun updateEntry(id: Long, sets: Int, reps: Int, restSec: Int, weight: Double?) =
        dao.updateEntry(id, sets, reps, restSec, weight)

    suspend fun deletePlan(id: Long) = dao.deletePlan(id)
}
