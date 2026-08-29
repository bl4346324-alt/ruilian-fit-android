package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 训练计划实体（PRD 核心实体 #1）
 * 内置 4 套模板：新手增肌、减脂燃脂、力量提升、居家无器械
 * 模板只读（isTemplate=true 不可修改），可复制生成自定义计划
 */
@Entity(tableName = "plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                 // 计划名称
    val type: String = "力量",        // 计划类型：力量 / 核心 / 有氧 / 恢复
    val isTemplate: Boolean = false,  // 是否内置模板（模板不可编辑）
    val cycleWeeks: Int = 4,          // 训练周期 1-12 周
    val targetDurationMin: Int = 60,  // 单次目标时长（分钟）
    val daysPerWeek: Int = 3,         // 每周训练日数
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
