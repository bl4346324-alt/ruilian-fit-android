package com.relifit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relifit.util.TimeUtils
import java.util.Calendar

data class HeatmapDayData(
    val date: Long,              // 当日零点毫秒数
    val count: Int = 0,          // 当日训练次数
    val volumeKg: Double = 0.0,  // 当日训练总容量
    val level: Int = 0           // 0: 未打卡, 1: 轻度, 2: 中度, 3: 高强度
)

/**
 * GitHub 风格训练打卡热力图组件 (Activity Heatmap)
 * 横向展示近 20 周（约 5 个月）的训练打卡矩阵，支持 Streak（连续天数）统计与单日点选交互
 */
@Composable
fun ActivityHeatmap(
    activityMap: Map<Long, HeatmapDayData>,
    currentStreak: Int,
    longestStreak: Int,
    totalActiveDays: Int,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val todayStart = remember { TimeUtils.startOfDay(System.currentTimeMillis()) }
    val currentWeekMon = remember { TimeUtils.startOfWeek(todayStart) }
    val weeksCount = 20
    val startMon = remember(currentWeekMon) { currentWeekMon - (weeksCount - 1) * 7 * 86400000L }

    var selectedDate by remember { mutableStateOf<Long?>(todayStart) }

    // 默认自动滚到最右侧（最新近期周数）
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = {}
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶栏：打卡勋章统计（连续打卡 + 最长连续 + 累计打卡）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StreakMetricItem(
                    icon = Icons.Filled.ElectricBolt,
                    iconTint = Color(0xFFFF9800),
                    value = "$currentStreak 天",
                    label = "当前连续"
                )
                StreakMetricItem(
                    icon = Icons.Filled.EmojiEvents,
                    iconTint = Color(0xFFFFC107),
                    value = "$longestStreak 天",
                    label = "最长连续"
                )
                StreakMetricItem(
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    iconTint = MaterialTheme.colorScheme.primary,
                    value = "$totalActiveDays 天",
                    label = "近半年打卡"
                )
            }

            Spacer(Modifier.height(16.dp))

            // 矩阵区域：左侧周几标签 + 右侧可横向滑动的矩阵格子
            Row(modifier = Modifier.fillMaxWidth()) {
                // 左侧周几文字标签 (对齐 Mon, Wed, Fri)
                Column(
                    modifier = Modifier
                        .padding(top = 18.dp, end = 6.dp)
                        .height(112.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("一", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("三", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("五", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("日", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 右侧矩阵（月份标头 + 7行方块）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                ) {
                    // 月份标头行
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (w in 0 until weeksCount) {
                            val weekMon = startMon + w * 7 * 86400000L
                            val cal = Calendar.getInstance().apply { timeInMillis = weekMon }
                            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                            // 每月月初所在周显示月份标签
                            val monthLabel = if (dayOfMonth <= 7 || w == 0) "${cal.get(Calendar.MONTH) + 1}月" else ""
                            Box(
                                modifier = Modifier
                                    .width(13.dp)
                                    .height(14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (monthLabel.isNotEmpty()) {
                                    Text(
                                        text = monthLabel,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // 7 行方格矩阵（周一至周日）
                    for (row in 0 until 7) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(vertical = 1.5.dp)
                        ) {
                            for (col in 0 until weeksCount) {
                                val dayTime = startMon + (col * 7 + row) * 86400000L
                                val isFuture = dayTime > todayStart
                                val isToday = dayTime == todayStart
                                val isSelected = selectedDate == dayTime
                                val dayData = activityMap[dayTime] ?: HeatmapDayData(dayTime)

                                val cellColor = when {
                                    isFuture -> Color.Transparent
                                    dayData.level == 3 -> MaterialTheme.colorScheme.primary
                                    dayData.level == 2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                    dayData.level == 1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cellColor)
                                        .then(
                                            when {
                                                isSelected -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                                isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                                                isFuture -> Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                                                else -> Modifier
                                            }
                                        )
                                        .clickable(enabled = !isFuture) {
                                            selectedDate = dayTime
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 底部：图例与单日交互信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 点击日期提示
                val sel = selectedDate
                val selData = if (sel != null) activityMap[sel] else null
                val detailText = if (sel != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = sel }
                    val dateStr = "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
                    if (selData != null && selData.count > 0) {
                        "$dateStr · ${selData.count} 次训练 · ${TimeUtils.thousands(selData.volumeKg)} kg"
                    } else if (sel == todayStart) {
                        "$dateStr (今天) · 尚未开练"
                    } else {
                        "$dateStr · 休息日"
                    }
                } else {
                    "点击格子查看每日详情"
                }

                Text(
                    text = detailText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 图例 (Less -> More)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("少", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(1.dp))
                    Box(modifier = Modifier.size(9.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(9.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(9.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(9.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(1.dp))
                    Text("多", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StreakMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(iconTint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
