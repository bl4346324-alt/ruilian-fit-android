package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 动作库实体（PRD 核心实体 #6）
 * 六大肌群：胸、背、肩、腿、手臂、核心
 * 器械类型：杠铃、哑铃、器械、自重
 * 难度：入门、中级、进阶
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // 动作中文名称
    val nameEn: String = "",             // 英文别名
    val muscleGroup: String,             // 主肌群（胸/背/肩/腿/手臂/核心）
    val secondaryMuscles: String = "",   // 协同肌（逗号分隔）
    val equipment: String,               // 器械类型
    val difficulty: String,              // 难度
    val actionType: String = "复合",     // 复合/孤立
    val keyPoints: String,               // 发力要点（每行一条）
    val mistakes: String,                // 易错提醒（每行一条）
    val breathTip: String,               // 呼吸节奏说明
    val offlineAvailable: Boolean = false, // 是否已离线下载
    val isFavorite: Boolean = false      // 是否收藏
)
