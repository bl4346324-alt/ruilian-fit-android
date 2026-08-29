package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 餐次实体（饮食记录模块）
 * 一日多餐：早餐/午餐/晚餐/加餐
 */
@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,                    // 所属日期（当日零点 epoch millis）
    val mealType: String,              // 早餐/午餐/晚餐/加餐
    val timeLabel: String = ""         // 时间标签（如 08:00）
)
