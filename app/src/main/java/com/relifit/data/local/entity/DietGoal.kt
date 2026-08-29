package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日营养目标实体（饮食记录模块）
 * 单例（固定 id=1）；热量与三大营养素目标均可自定义（用户可分别设定克数）
 */
@Entity(tableName = "diet_goal")
data class DietGoal(
    @PrimaryKey val id: Int = 1,
    val dailyKcal: Int = 2200,         // 每日热量目标（kcal）
    val carbsG: Int = 248,             // 每日碳水目标（g）≈2200×45%÷4
    val proteinG: Int = 165,           // 每日蛋白质目标（g）≈2200×30%÷4
    val fatG: Int = 61                 // 每日脂肪目标（g）≈2200×25%÷9
)
