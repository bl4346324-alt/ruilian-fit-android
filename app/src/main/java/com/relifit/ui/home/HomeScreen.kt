package com.relifit.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.relifit.ui.components.BrandTopBar
import com.relifit.ui.components.RingProgress
import com.relifit.ui.components.SectionTitle
import com.relifit.ui.components.softCardShadow
import com.relifit.ui.theme.LocalExtraColors

/**
 * 首页（Demo 首页布局）：Hero 今日训练 / 计划环卡 / 身体数据
 * （训练统计数据已移至"数据统计"页，避免与首页重复）
 */
@Composable
fun HomeScreen(
    onOpenPlan: (Long) -> Unit,
    onStartWorkout: (Long?, Long?) -> Unit,
    onOpenBody: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val extra = LocalExtraColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 顶栏
        item {
            BrandTopBar(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onTimer = { onStartWorkout(null, null) },
                onSettings = onOpenSettings
            )
        }

        // ===== Hero 今日训练 =====
        item {
            val today = state.todayDay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .softCardShadow(28)
                    .background(
                        Brush.linearGradient(extra.heroGradient),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(22.dp)
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
                            text = "今日训练 · 第 ${state.cycleWeek} 周 / 共 ${state.cycleWeeks} 周",
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
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = today?.day?.name ?: "今日休息",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (extra.heroGradient.first().luminance() > 0.5f) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else Color(0xFFEAF2F9)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroChip("${state.todayEntries} 个动作", Icons.Filled.FitnessCenter)
                        HeroChip(
                            "${today?.day?.defaultRestSec ?: 60}s 组间休",
                            Icons.Filled.Timer
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    // 本周进度
                    Row(Modifier.fillMaxWidth()) {
                        Text("本周进度", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${state.weekTrained} / ${state.weekTotal} 次",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
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
                                    (state.weekTrained.toFloat() / state.weekTotal.coerceAtLeast(1)).coerceIn(0f, 1f)
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
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onStartWorkout(state.plan?.id, today?.day?.id) },
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
                        Spacer(Modifier.width(16.dp))
                        TextButton(onClick = { state.plan?.let { onOpenPlan(it.id) } }) {
                            Text("查看计划", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        // ===== 训练计划 =====
        item {
            SectionTitle("训练计划", actionText = "全部") { state.plan?.let { onOpenPlan(it.id) } }
            state.plan?.let { plan ->
                AppCard(onClick = { onOpenPlan(plan.id) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RingProgress(
                            progress = state.cycleWeek.toFloat() / plan.cycleWeeks,
                            centerTop = "${state.cycleWeek}/${plan.cycleWeeks}",
                            centerBottom = "周",
                            size = 92
                        )
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    plan.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "进行中",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.days.forEach { d ->
                                    val isToday = state.todayDay?.day?.id == d.day.id
                                    Text(
                                        d.day.name,
                                        fontSize = 12.sp,
                                        color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .background(
                                                if (isToday) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== 身体数据 =====
        item {
            SectionTitle("身体数据", actionText = "详情", onAction = onOpenBody)
            AppCard(onClick = onOpenBody) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    (0..2).forEach { i ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(state.bodyValues.getOrElse(i) { "--" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(state.bodyLabels.getOrElse(i) { "" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                state.bodyChanges.getOrElse(i) { "" },
                                fontSize = 11.sp,
                                color = extra.success,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HeroChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
