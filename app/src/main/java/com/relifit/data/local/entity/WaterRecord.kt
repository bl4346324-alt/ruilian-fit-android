package com.relifit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日饮水记录实体
 * 记录当日累计饮水量与饮水目标，以当日 00:00:00 毫秒时间戳为主键
 */
@Entity(tableName = "water_records")
data class WaterRecord(
    @PrimaryKey val date: Long,      // 当日 00:00:00 毫秒时间戳
    val amountMl: Int = 0,           // 当日已饮水量（毫升 ml）
    val goalMl: Int = 2000           // 每日饮水目标（毫升 ml，默认 2000ml）
)
