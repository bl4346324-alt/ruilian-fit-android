package com.relifit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 每日水分摄入追踪卡片 (Water Tracker Card)
 * 健身与减脂场景下高频记录水化状态，支持快捷 +250ml、+500ml、+100ml 与微调撤销
 */
@Composable
fun WaterTrackerCard(
    amountMl: Int,
    goalMl: Int,
    onAddWater: (Int) -> Unit,
    onResetWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (goalMl > 0) (amountMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "water_progress")
    val percent = (progress * 100).roundToInt()
    val isGoalReached = amountMl >= goalMl && goalMl > 0

    val waterCyan = Color(0xFF00ACC1)
    val waterBlue = Color(0xFF1E88E5)

    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = {}
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶栏：标题 + 目标与重置
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(waterCyan.copy(alpha = 0.2f), waterBlue.copy(alpha = 0.2f))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalDrink,
                            contentDescription = "饮水追踪",
                            tint = waterCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "水分摄入",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isGoalReached) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "已达标 🎉",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF43A047),
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "保持肌肉水合与高强度代谢",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 当前值 / 目标
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$amountMl",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = waterBlue
                    )
                    Text(
                        text = " / ${goalMl}ml",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 进度条
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Brush.horizontalGradient(listOf(waterCyan, waterBlue)))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "今日进度 $percent%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (amountMl > 0) {
                        Text(
                            text = "重置",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onResetWater() }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 快捷加水按钮组
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // +250ml (普通水杯)
                QuickWaterButton(
                    label = "+250ml",
                    sub = "一杯水",
                    onClick = { onAddWater(250) },
                    modifier = Modifier.weight(1f),
                    bgColor = waterCyan.copy(alpha = 0.12f),
                    textColor = waterCyan
                )

                // +500ml (一瓶矿泉水)
                QuickWaterButton(
                    label = "+500ml",
                    sub = "一整瓶",
                    onClick = { onAddWater(500) },
                    modifier = Modifier.weight(1f),
                    bgColor = waterBlue.copy(alpha = 0.12f),
                    textColor = waterBlue
                )

                // +100ml (小口啜饮)
                QuickWaterButton(
                    label = "+100ml",
                    sub = "小口补水",
                    onClick = { onAddWater(100) },
                    modifier = Modifier.weight(1f),
                    bgColor = MaterialTheme.colorScheme.surfaceVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // -250ml (回退/撤销)
                if (amountMl > 0) {
                    Box(
                        modifier = Modifier
                            .size(height = 48.dp, width = 42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onAddWater(-250) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "减少250ml",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickWaterButton(
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color,
    textColor: Color
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = sub,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}
