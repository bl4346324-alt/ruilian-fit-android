package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 动作条目实体（PRD 核心实体 #3）
 * 训练日中的单个动作及其目标参数
 */
@Entity(
    tableName = "exercise_entries",
    foreignKeys = [ForeignKey(
        entity = WorkoutDay::class,
        parentColumns = ["id"],
        childColumns = ["workoutDayId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index("workoutDayId")]
)
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutDayId: Long,           // 所属训练日
    val exerciseId: Long,             // 关联动作库动作
    val sortOrder: Int,               // 动作顺序
    val targetSets: Int = 3,          // 目标组数
    val targetReps: Int = 10,         // 目标次数
    val targetWeight: Double? = null, // 目标重量（可空=自重）
    val restSec: Int = 60             // 组间休息秒数
)
