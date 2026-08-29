package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 训练组实体（PRD 核心实体 #5）
 * 记录每组实际重量/次数；重量精度 0.5kg
 */
@Entity(
    tableName = "set_records",
    foreignKeys = [ForeignKey(
        entity = WorkoutLog::class,
        parentColumns = ["id"],
        childColumns = ["logId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("logId")]
)
data class SetRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId: Long = 0,               // 所属训练记录（保存时由 Repository 填充）
    val exerciseId: Long,              // 动作
    val setIndex: Int,                 // 第几组
    val weightKg: Double,              // 实际重量（kg，0=自重）
    val reps: Int,                     // 实际次数
    val completed: Boolean = true,     // 是否完成（false=失败组）
    val restSec: Int = 0               // 该组后休息秒数
)
