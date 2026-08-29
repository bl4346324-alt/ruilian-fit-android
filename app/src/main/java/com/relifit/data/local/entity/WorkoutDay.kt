package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 训练日实体（PRD 核心实体 #2）
 * 一个计划包含多个训练日（如周一推日、周三蹲日、周五拉日）
 * planId 外键级联删除：删除计划时自动清理其训练日（条目再由 ExerciseEntry 级联）
 */
@Entity(
    tableName = "workout_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,                 // 所属计划
    val dayIndex: Int,                // 周内第几天（1-7，1=周一）
    val name: String,                 // 训练日名称（如"上肢推日"）
    val defaultRestSec: Int = 90      // 该日默认组间休息秒数
)
