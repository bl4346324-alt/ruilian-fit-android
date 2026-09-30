package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 健身菜谱实体（精准控制油、盐、蛋白质与三大营养素）
 * 解决纯本地拍照识别率低、外卖隐形油盐超标的痛点：通过标准化配方与烹饪步骤精准记账
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                     // 菜谱名称（如：黑椒香煎鸡胸肉）
    val category: String = "高蛋白增肌",    // 分类：高蛋白增肌 / 极低脂减脂 / 均衡轻食
    val prepTimeMin: Int = 15,            // 制作耗时（分钟）
    val difficulty: String = "简单",       // 简单 / 中等 / 进阶
    val oilGram: Double = 3.0,            // 精准控油量（g）
    val saltGram: Double = 1.5,           // 精准控盐量（g）
    val proteinG: Double,                 // 蛋白质（g）
    val carbsG: Double,                   // 碳水（g）
    val fatG: Double,                     // 脂肪（g）
    val kcal: Double,                     // 总热量（kcal）
    val ingredients: String,              // 食材清单及精确克数（换行分隔）
    val instructions: String,             // 烹饪步骤要点
    val isCustom: Boolean = false         // 是否用户自建
)
