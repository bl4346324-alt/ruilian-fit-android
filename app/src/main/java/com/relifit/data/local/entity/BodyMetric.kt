package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 身体数据实体
 * 仅记录三项：体重、身高、每日运动数量（按用户需求精简，PRD 围度字段已移除）
 */
@Entity(tableName = "body_metrics")
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,                    // 记录日期
    val weightKg: Double?,             // 体重
    val heightCm: Double?,             // 身高
    val dailyActivity: Double?         // 每日运动数量（步数/时长等，按用户自定义）
)
