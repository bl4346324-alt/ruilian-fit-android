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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 杠铃片规格定义（经典奥运奥林匹克色彩编码）
 */
private data class PlateSpec(
    val weight: Double,
    val color: Color,
    val textColor: Color = Color.White,
    val heightDp: Int
)

private val STANDARD_PLATES = listOf(
    PlateSpec(25.0, Color(0xFFE53935), Color.White, 96),   // 红色 25kg
    PlateSpec(20.0, Color(0xFF1E88E5), Color.White, 90),   // 蓝色 20kg
    PlateSpec(15.0, Color(0xFFFBC02D), Color.Black, 80),   // 黄色 15kg
    PlateSpec(10.0, Color(0xFF43A047), Color.White, 70),   // 绿色 10kg
    PlateSpec(5.0, Color(0xFFEEEEEE), Color(0xFF212121), 60), // 白色 5kg
    PlateSpec(2.5, Color(0xFF37474F), Color.White, 52),   // 黑色 2.5kg
    PlateSpec(1.25, Color(0xFF90A4AE), Color(0xFF212121), 46) // 银色 1.25kg
)

/**
 * 杠铃片计算器交互弹窗（健身房现场刚需神器）
 * 输入总重量与杆重，自动计算并可视化展示单侧与双侧所需挂载的杠铃片
 */
@Composable
fun PlateCalculatorDialog(
    initialWeight: Double,
    unit: String = "kg",
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var targetWeight by remember { mutableDoubleStateOf(if (initialWeight > 0.0) initialWeight else 60.0) }
    var barWeight by remember { mutableDoubleStateOf(20.0) } // 标准男子杆 20kg

    // 可选用的杠铃片规格开关（默认全部可用）
    var enabledPlates by remember {
        mutableStateOf(STANDARD_PLATES.map { it.weight }.toSet())
    }

    // 计算逻辑
    val remainingForBothSides = (targetWeight - barWeight).coerceAtLeast(0.0)
    val remainingPerSide = remainingForBothSides / 2.0

    // 贪心配片（单侧）
    val platesPerSide = remember(remainingPerSide, enabledPlates) {
        val list = mutableListOf<PlateSpec>()
        var rem = remainingPerSide
        val available = STANDARD_PLATES.filter { it.weight in enabledPlates }.sortedByDescending { it.weight }
        for (plate in available) {
            while (rem >= plate.weight - 0.001) {
                list.add(plate)
                rem -= plate.weight
            }
        }
        list
    }

    val actualPlatesWeight = platesPerSide.sumOf { it.weight } * 2.0
    val totalCalculatedWeight = barWeight + actualPlatesWeight

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "杠铃片配重计算器",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 目标总重量快速调节
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("目标总重 (含杆)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${formatNum(targetWeight)} $unit",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    // 步进按钮
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-10.0, -2.5, +2.5, +10.0).forEach { delta ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        targetWeight = (targetWeight + delta).coerceAtLeast(barWeight)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (delta > 0) "+$delta" else "$delta",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 2. 空杆重量选择
                Column {
                    Text("杠铃杆重量", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(20.0 to "20kg 标准杆", 15.0 to "15kg 女子杆", 10.0 to "10kg 短杆", 0.0 to "0kg (扣除)").forEach { (w, lbl) ->
                            val selected = barWeight == w
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { barWeight = w }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = lbl,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. 杠铃横杆与挂片可视化图形展示
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E232A))
                        .padding(14.dp)
                ) {
                    Text("单侧挂片示意图（从内到外）", fontSize = 11.sp, color = Color.LightGray)
                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // 杠铃套筒 (Sleeve bar)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(Color(0xFF78909C), RoundedCornerShape(4.dp))
                        )
                        // 档环 (Bar collar)
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(60.dp)
                                .background(Color(0xFFB0BEC5), RoundedCornerShape(2.dp))
                        )

                        // 杠铃片排布
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (platesPerSide.isEmpty()) {
                                Text("仅需空杆，无需加片", color = Color(0xFFCFD8DC), fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp))
                            } else {
                                platesPerSide.forEach { p ->
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height(p.heightDp.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(p.color)
                                            .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = formatPlateLabel(p.weight),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = p.textColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. 配重文字清单明细
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    val grouped = platesPerSide.groupBy { it.weight }.mapValues { it.value.size }
                    val breakdown = if (grouped.isEmpty()) "无需杠铃片"
                    else grouped.entries.joinToString(" + ") { "${formatNum(it.key)}kg × ${it.value}片" }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("单侧挂载：", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(breakdown, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("最终总重：", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${formatNum(totalCalculatedWeight)} kg (两边共挂 ${formatNum(actualPlatesWeight)}kg + 杆重 ${formatNum(barWeight)}kg)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(totalCalculatedWeight)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("带入重量 (${formatNum(totalCalculatedWeight)}kg)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

private fun formatNum(n: Double): String {
    val rounded = (n * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private fun formatPlateLabel(w: Double): String {
    return if (w % 1.0 == 0.0) "${w.toInt()}" else "$w"
}
