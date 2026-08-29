package com.relifit.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 训练记录 + 全部训练组（一对多关系查询结果）
 */
data class LogWithSets(
    @Embedded val log: WorkoutLog,
    @Relation(parentColumn = "id", entityColumn = "logId")
    val sets: List<SetRecord> = emptyList()
)

/**
 * 训练日 + 动作条目（一对多关系查询结果）
 */
data class DayWithEntries(
    @Embedded val day: WorkoutDay,
    @Relation(parentColumn = "id", entityColumn = "workoutDayId")
    val entries: List<ExerciseEntry> = emptyList()
)

/**
 * 餐次 + 食物条目（一对多关系查询结果）
 */
data class MealWithItems(
    @Embedded val meal: Meal,
    @Relation(parentColumn = "id", entityColumn = "mealId")
    val items: List<FoodItem> = emptyList()
)

/**
 * 动作条目 + 动作详情（用于展示训练日动作列表）
 */
data class EntryWithExercise(
    @Embedded val entry: ExerciseEntry,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise? = null
)
