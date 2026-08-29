package com.relifit.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.util.TimeUtils
import com.relifit.util.UnitConverter
import com.relifit.ui.components.softCardShadow
import kotlinx.coroutines.flow.collectLatest

/**
 * 训练进行页（Demo 布局，核心 P0）
 * 总计时器 / 组记录 / 重量次数步进 / 组间休息弹窗倒计时（+30s、跳过、震动）/ 保存
 */
@Composable
fun WorkoutScreen(
    planId: Long?,
    dayId: Long?,
    initialExerciseId: Long?,
    onExit: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 每秒跳动的时钟/倒计时独立订阅，避免整页重组（性能优化）
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val restLeft by viewModel.restLeft.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showAddPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbar.showSnackbar(it) }
    }
    // 保存完成后返回上一页
    LaunchedEffect(Unit) {
        viewModel.onSaved.collectLatest { onExit() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // ===== 顶栏：关闭 + 动作进度 + 总计时 =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (state.doneCount > 0) showExitConfirm = true else { viewModel.discard(); onExit() }
                    }) {
                        Icon(Icons.Filled.Close, "退出", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.current?.name ?: "自由训练",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                state.exercises.isEmpty() -> "请添加训练动作"
                                state.finished -> "全部完成 · 已记录 ${state.doneCount} 组"
                                else -> "动作 ${state.currentIndex + 1} / ${state.exercises.size} · 组 ${(state.current?.sets?.size ?: 0) + 1} / ${state.current?.targetSets ?: 0}"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 总计时 chip（Demo woClock）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            TimeUtils.mmss(clock),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // ===== 空会话提示 =====
                if (state.exercises.isEmpty()) {
                    Spacer(Modifier.height(80.dp))
                    Text(
                        "自由训练：从动作库添加训练动作",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showAddPicker = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("添加第一个动作", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // ===== 当前动作卡片（目标 + 组列表） =====
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .softCardShadow(28)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        val cur = state.current
                        if (cur == null) {
                            // 全部完成：结束态（不再访问 current，避免 NPE）
                            Text(
                                "🎉 全部动作已完成",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "已记录 ${state.doneCount} 组，点击下方「结束训练」保存本次记录",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "当前动作 · 目标 ${cur.targetSets} 组 × ${cur.targetReps} 次 · 组间休 ${cur.restSec}s",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            cur.sets.forEachIndexed { i, s ->
                                SetRow(num = i + 1, weightText = UnitConverter.weightText(s.weightKg, state.unit), reps = s.reps, done = true)
                            }
                            // 当前/待做组
                            for (i in cur.sets.size until cur.targetSets) {
                                SetRow(
                                    num = i + 1,
                                    weightText = UnitConverter.weightText(state.inputWeightKg, state.unit),
                                    reps = cur.targetReps,
                                    done = false,
                                    isCurrent = i == cur.sets.size
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ===== 输入卡：重量 / 次数步进 + 完成本组 =====
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        StepperRow(
                            label = "重量",
                            value = UnitConverter.weightText(state.inputWeightKg, state.unit),
                            onMinus = { viewModel.changeWeight(-1) },
                            onPlus = { viewModel.changeWeight(1) }
                        )
                        StepperRow(
                            label = "次数",
                            value = "${state.inputReps} 次",
                            onMinus = { viewModel.changeReps(-1) },
                            onPlus = { viewModel.changeReps(1) }
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { if (state.finished) viewModel.finishAndSave() else viewModel.completeSet() },
                            enabled = !state.restActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) {
                            Icon(Icons.Filled.Check, null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (state.finished) "全部完成 · 点击结束训练" else "完成本组",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ===== 操作行：添加动作 / 结束训练 =====
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showAddPicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("添加动作", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.finishAndSave() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("结束训练", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ===== 组间休息弹窗（Demo restOverlay） =====
            if (state.restActive) {
                Dialog(
                    onDismissRequest = { /* 休息中不可关闭 */ },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.86f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(320.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
                                .padding(28.dp)
                        ) {
                            Text("组间休息", fontSize = 15.sp, letterSpacing = 3.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$restLeft",
                                fontSize = 84.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // 进度条
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(
                                            if (state.restTotal > 0) restLeft.toFloat() / state.restTotal else 1f
                                        )
                                        .height(10.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    androidx.compose.ui.graphics.Color(0xFF6E9CC4),
                                                    MaterialTheme.colorScheme.primary
                                                )
                                            ),
                                            RoundedCornerShape(50)
                                        )
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { viewModel.restPlus30() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+30 秒", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.skipRest() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("跳过休息", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== 退出确认（有记录时） =====
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("退出训练？") },
            text = { Text("已记录 ${state.doneCount} 组，退出后将不保存本次训练。") },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; viewModel.discard(); onExit() }) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("继续训练") }
            }
        )
    }

    // ===== 添加动作选择器（自由训练/临时加动作） =====
    if (showAddPicker) {
        val all = viewModel.allExercises.collectAsStateWithLifecycle().value
        AlertDialog(
            onDismissRequest = { showAddPicker = false },
            title = { Text("添加动作") },
            text = {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(all, key = { it.id }) { ex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addExercise(ex.id)
                                    showAddPicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                ex.name,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${ex.muscleGroup} · ${ex.equipment}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddPicker = false }) { Text("取消") }
            }
        )
    }
}

/** 组行（Demo setrow：序号 + 重量 + 次数，done 显示勾选） */
@Composable
private fun SetRow(num: Int, weightText: String, reps: Int, done: Boolean, isCurrent: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                when {
                    isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                RoundedCornerShape(14.dp)
            )
            .then(
                if (isCurrent) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(14.dp)
                ) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (done) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(9.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            } else {
                Text("$num", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(weightText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("$reps 次", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 步进行（Demo stepper：标签 + 减 + 数值 + 加） */
@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        StepButton("-", onMinus)
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(110.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        StepButton("+", onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
