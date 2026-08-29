package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 食物条目实体（饮食记录模块）
 * 份数 servings 支持 Demo 的 +/− 调整，营养值按份数线性计算
 */
@Entity(
    tableName = "food_items",
    foreignKeys = [ForeignKey(
        entity = Meal::class,
        parentColumns = ["id"],
        childColumns = ["mealId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("mealId")]
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,                  // 所属餐次
    val name: String,                  // 食物名称
    val quantity: String,              // 每份量（如 50g / 2 个 / 250ml）
    val kcal: Double,                  // 每份热量（kcal）
    val carbsG: Double,                // 每份碳水（g）
    val proteinG: Double,              // 每份蛋白质（g）
    val fatG: Double,                  // 每份脂肪（g）
    val servings: Int = 1              // 份数（Demo +/− 调整）
)
