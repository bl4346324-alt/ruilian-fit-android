package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 训练记录实体（PRD 核心实体 #4）
 * 一次完整训练的结果存档
 */
@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,                    // 训练日期（epoch millis）
    val planId: Long? = null,          // 来源计划（可空=自由训练）
    val workoutDayId: Long? = null,    // 来源训练日（可空）
    val durationMin: Int,              // 实际训练时长（分钟）
    val totalVolumeKg: Double,         // 训练容量 = Σ(重量×次数)
    val totalSets: Int,                // 总组数
    val note: String = "",             // 训练备注
    val status: String = "完成"        // 完成 / 中断
)
