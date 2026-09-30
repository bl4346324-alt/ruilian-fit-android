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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.ui.components.AppCard
import com.relifit.ui.components.AppChip
import com.relifit.ui.components.BarChart
import com.relifit.ui.components.BrandTopBar
import com.relifit.ui.components.ChartCard
import com.relifit.ui.components.HBarList
import com.relifit.ui.components.LineChart
import com.relifit.ui.components.RingProgress
import com.relifit.ui.components.SectionTitle
import com.relifit.ui.components.StatCard
import com.relifit.ui.components.softCardShadow
import com.relifit.ui.home.HomeViewModel
import com.relifit.ui.theme.LocalExtraColors
import com.relifit.util.TimeUtils
import kotlin.math.roundToInt

/**
 * 统计与主屏概览（整合原首页与数据统计）
 * 包含：今日训练 Hero + 计划执行卡片 + 身体数据卡片 + 周月深度分析指标卡 + 4 大数据统计图表
 */
@Composable
fun StatsOverviewScreen(
    onOpenPlan: (Long) -> Unit,
    onOpenPlans: () -> Unit,
    onStartWorkout: (Long?, Long?) -> Unit,
    onOpenBody: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    statsViewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
) {
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val statsState by statsViewModel.state.collectAsStateWithLifecycle()
    val period by statsViewModel.period.collectAsStateWithLifecycle()
    val extra = LocalExtraColors.current

    var showExercisePicker by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // 顶栏：品牌 logo + 快捷自由训练 + 主题切换 + 设置
            BrandTopBar(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onTimer = { onStartWorkout(null, null) },
                onSettings = onOpenSettings
            )

            Spacer(Modifier.height(8.dp))

            // ===== 第一部分：今日训练 Hero（原首页核心） =====
            val today = homeState.todayDay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .softCardShadow(28)
                    .background(
                        Brush.linearGradient(extra.heroGradient),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(extra.success)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "今日安排 · 第 ${homeState.cycleWeek} 周 / 共 ${homeState.cycleWeeks} 周",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = today?.day?.name ?: "今日休息",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (extra.heroGradient.first().luminance() > 0.5f) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else Color(0xFFEAF2F9)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${homeState.todayEntries} 个动作",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Text(
                            "${today?.day?.defaultRestSec ?: 60}s 组间休",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    // 本周进度条
                    Row(Modifier.fillMaxWidth()) {
                        Text("本周完成进度", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${homeState.weekTrained} / ${homeState.weekTotal} 次",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(
                                    (homeState.weekTrained.toFloat() / homeState.weekTotal.coerceAtLeast(1)).coerceIn(0f, 1f)
                                )
                                .height(8.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF7FA9CF), MaterialTheme.colorScheme.primary)
                                    ),
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onStartWorkout(homeState.plan?.id, today?.day?.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (today != null) "开始训练" else "自由训练", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(14.dp))
                        TextButton(onClick = { homeState.plan?.let { onOpenPlan(it.id) } }) {
                            Text("查看计划", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ===== 双卡片：当前计划环 + 身体数据概览 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 计划卡片
                homeState.plan?.let { plan ->
                    AppCard(
                        onClick = { onOpenPlan(plan.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("执行中计划", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(plan.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RingProgress(
                                    progress = homeState.cycleWeek.toFloat() / plan.cycleWeeks.coerceAtLeast(1),
                                    centerTop = "${homeState.cycleWeek}/${plan.cycleWeeks}",
                                    centerBottom = "周",
                                    size = 56
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("周期进度", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("共 ${plan.cycleWeeks} 周", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                } ?: run {
                    AppCard(
                        onClick = onOpenPlans,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("暂无计划", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Text("点击选取训练计划", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // 身体数据卡片
                AppCard(
                    onClick = onOpenBody,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("身体数据", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.Scale, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        val body = homeState.body
                        if (body != null) {
                            Text(body.weightKg?.let { "${it} kg" } ?: "-- kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text("身高 ${body.heightCm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.1f", it) } ?: "--"} cm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("尚未记录", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text("点此记录身体数据", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ===== 第二部分：数据统计与图表分析（原数据统计核心） =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "训练数据分析",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 周/月切换
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                        .padding(3.dp)
                ) {
                    listOf("周", "月").forEach { p ->
                        val selected = period == p
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    RoundedCornerShape(50)
                                )
                                .clickable { statsViewModel.setPeriod(p) }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                p,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 3 个核心统计指标卡
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    icon = Icons.Filled.FitnessCenter,
                    iconTint = MaterialTheme.colorScheme.primary,
                    delta = statsState.countDelta,
                    value = "${statsState.count} 次",
                    label = if (period == "周") "本周训练" else "本月训练",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.AccessTime,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    delta = statsState.durDelta,
                    value = "${formatDur(statsState.avgDurationMin)}h",
                    label = "平均时长",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.Restore,
                    iconTint = MaterialTheme.colorScheme.primary,
                    delta = statsState.volDelta,
                    value = TimeUtils.thousands(statsState.volume),
                    label = "总容量 kg",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // 训练打卡足迹 (GitHub 风格 Activity Heatmap)
            com.relifit.ui.components.ActivityHeatmap(
                activityMap = statsState.heatmapActivityMap,
                currentStreak = statsState.currentStreak,
                longestStreak = statsState.longestStreak,
                totalActiveDays = statsState.totalActiveDays
            )

            Spacer(Modifier.height(16.dp))

            // 图表 1：重量进步折线图
            ChartCard(
                title = "动作重量进阶",
                subtitle = statsState.weightSub,
                badge = if (statsState.prIndex >= 0) "新纪录" else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "切换动作 ▶",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .clickable { showExercisePicker = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                LineChart(
                    values = statsState.weightValues,
                    labels = statsState.weightLabels,
                    prIndex = statsState.prIndex
                )
            }

            Spacer(Modifier.height(16.dp))

            // 图表 2：训练频次柱状图
            ChartCard(
                title = "训练频次分布",
                subtitle = if (period == "周") "周一至周日分布（次）" else "本月各周分布（次）"
            ) {
                BarChart(
                    values = statsState.freqValues,
                    labels = statsState.freqLabels
                )
            }

            Spacer(Modifier.height(16.dp))

            // 图表 3：近 7 天饮食热量摄入趋势
            ChartCard(
                title = "近 7 天热量摄入趋势",
                subtitle = statsState.dietSub
            ) {
                BarChart(
                    values = statsState.dietValues,
                    labels = statsState.dietLabels,
                    highlightIndex = 6
                )
            }

            Spacer(Modifier.height(16.dp))

            // 图表 4：肌群负荷分布
            ChartCard(
                title = "肌群训练负荷占比",
                subtitle = statsState.muscleSub
            ) {
                if (statsState.muscles.isEmpty()) {
                    Text("暂无数据，完成训练后生成", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    HBarList(items = statsState.muscles, valueText = statsState.muscleTexts)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // 动作切换弹窗
    if (showExercisePicker) {
        AlertDialog(
            onDismissRequest = { showExercisePicker = false },
            title = { Text("选择统计动作", fontWeight = FontWeight.Bold) },
            text = {
                if (statsState.availableExercises.isEmpty()) {
                    Text("暂无可统计动作", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(statsState.availableExercises) { (id, name) ->
                            val isSelected = id == statsState.selectedExerciseId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        statsViewModel.selectExercise(id)
                                        showExercisePicker = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExercisePicker = false }) { Text("关闭") }
            }
        )
    }
}

private fun formatDur(min: Double): String {
    val h = min / 60.0
    return if (h >= 10) h.toInt().toString() else String.format("%.1f", h)
}
