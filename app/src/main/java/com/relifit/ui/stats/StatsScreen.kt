package com.relifit.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.ui.components.BarChart
import com.relifit.ui.components.ChartCard
import com.relifit.ui.components.HBarList
import com.relifit.ui.components.LineChart
import com.relifit.ui.components.ScreenTopBar
import com.relifit.ui.components.StatCard
import com.relifit.ui.components.AppChip
import com.relifit.util.TimeUtils

/**
 * 数据统计页（Demo 布局）：周/月切换 + 指标卡 + 4 张图表（重量曲线/训练频率/近7天热量/肌群覆盖）
 */
@Composable
fun StatsScreen(
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // 顶栏：标题 + 周/月分段 + 主题切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "数据统计",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // 周/月分段（Demo seg）
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                        .padding(4.dp)
                ) {
                    listOf("周", "月").forEach { p ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (period == p) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(50)
                                )
                                .clickable { viewModel.setPeriod(p) }
                                .padding(horizontal = 22.dp, vertical = 8.dp)
                        ) {
                            Text(
                                p,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (period == p) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                        .clickable(onClick = onToggleTheme),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ===== 三个指标卡 =====
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    icon = Icons.Filled.FitnessCenter,
                    iconTint = MaterialTheme.colorScheme.primary,
                    delta = state.countDelta,
                    value = "${state.count} 次",
                    label = if (period == "周") "本周训练" else "本月训练",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.AccessTime,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    delta = state.durDelta,
                    value = "${formatDur(state.avgDurationMin)}h",
                    label = "平均训练时长",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.Restore,
                    iconTint = MaterialTheme.colorScheme.primary,
                    delta = state.volDelta,
                    value = TimeUtils.thousands(state.volume),
                    label = "训练容量 kg",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))

            // ===== 重量进步折线图 =====
            ChartCard(
                title = "重量进步曲线",
                subtitle = state.weightSub,
                badge = if (state.prIndex >= 0) "新纪录" else null
            ) {
                LineChart(
                    values = state.weightValues,
                    labels = state.weightLabels,
                    prIndex = state.prIndex
                )
            }

            Spacer(Modifier.height(14.dp))

            // ===== 训练频率柱状图 =====
            ChartCard(title = "训练频率", subtitle = state.freqSub) {
                BarChart(values = state.freqValues, labels = state.freqLabels)
            }

            Spacer(Modifier.height(14.dp))

            // ===== 近 7 天热量柱状图 =====
            ChartCard(title = "近 7 天热量", subtitle = state.dietSub) {
                BarChart(
                    values = state.dietValues,
                    labels = state.dietLabels,
                    highlightIndex = 6   // 今天（周日位）高亮
                )
            }

            Spacer(Modifier.height(14.dp))

            // ===== 肌群覆盖 =====
            ChartCard(title = "肌群覆盖", subtitle = state.muscleSub) {
                if (state.muscles.isEmpty()) {
                    Text("暂无数据，完成训练后生成", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    HBarList(items = state.muscles, valueText = state.muscleTexts)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 无涟漪点击扩展（顶栏分段/按钮）——当前直接使用基础 clickable，M3 默认水波纹即可 */

private fun formatDur(min: Double): String {
    val h = min / 60.0
    return if (h >= 10) h.toInt().toString() else String.format("%.1f", h)
}
