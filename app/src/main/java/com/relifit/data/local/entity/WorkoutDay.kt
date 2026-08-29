package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 训练日实体（PRD 核心实体 #2）
 * 一个计划包含多个训练日（如周一推日、周三蹲日、周五拉日）
 */
@Entity(tableName = "workout_days")
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,                 // 所属计划
    val dayIndex: Int,                // 周内第几天（1-7，1=周一）
    val name: String,                 // 训练日名称（如"上肢推日"）
    val defaultRestSec: Int = 90      // 该日默认组间休息秒数
)
