package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 餐次实体（饮食记录模块）
 * 一日多餐：早餐/午餐/晚餐/加餐
 * (date, mealType) 唯一约束：同日同餐次只存在一条（初始化幂等，防重复插入导致热量统计翻倍）
 */
@Entity(
    tableName = "meals",
    indices = [Index(value = ["date", "mealType"], unique = true)]
)
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,                    // 所属日期（当日零点 epoch millis）
    val mealType: String,              // 早餐/午餐/晚餐/加餐
    val timeLabel: String = ""         // 时间标签（如 08:00）
)
