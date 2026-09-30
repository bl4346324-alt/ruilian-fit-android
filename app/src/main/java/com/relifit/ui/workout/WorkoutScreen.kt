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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.util.TimeUtils
import com.relifit.util.UnitConverter
import com.relifit.ui.components.AppChip
import com.relifit.ui.components.PlateCalculatorDialog
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
    val haptic = LocalHapticFeedback.current
    var showExitConfirm by remember { mutableStateOf(false) }
    var showAddPicker by remember { mutableStateOf(false) }
    var showPlateCalculator by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbar.showSnackbar(it) }
    }
    // 保存完成后返回上一页
    LaunchedEffect(Unit) {
        viewModel.onSaved.collectLatest { onExit() }
    }

    // 页面不可见（退后台/切页）时暂停总计时，避免后台时长计入训练时长
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pauseTotalTimer()
                Lifecycle.Event.ON_RESUME -> viewModel.resumeTotalTimer()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                                SetRow(
                                    num = i + 1,
                                    weightText = UnitConverter.weightText(s.weightKg, state.unit),
                                    reps = s.reps,
                                    done = true,
                                    setType = s.setType
                                )
                            }
                            // 当前/待做组
                            for (i in cur.sets.size until cur.targetSets) {
                                SetRow(
                                    num = i + 1,
                                    weightText = UnitConverter.weightText(state.inputWeightKg, state.unit),
                                    reps = cur.targetReps,
                                    done = false,
                                    setType = if (i == cur.sets.size) state.inputSetType else "NORMAL",
                                    isCurrent = i == cur.sets.size
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ===== 输入卡：组类型 + 重量 / 次数步进 + 完成本组 =====
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        // 上次表现参照 (Ghost Data)
                        val ghost = state.previousPerformanceForCurrentSet
                        if (ghost != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 上次该组: ${UnitConverter.weightText(ghost.weightKg, state.unit)} × ${ghost.reps}次",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "一键带入",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { viewModel.applyPreviousPerformance() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // 组类型选择器（正式 N / 热身 W / 递减 D / 力竭 F）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SetTypeChip(
                                label = "正式组",
                                tag = "N",
                                selected = state.inputSetType == "NORMAL",
                                activeColor = MaterialTheme.colorScheme.primary,
                                onClick = { viewModel.changeSetType("NORMAL") },
                                modifier = Modifier.weight(1f)
                            )
                            SetTypeChip(
                                label = "热身组",
                                tag = "W",
                                selected = state.inputSetType == "WARMUP",
                                activeColor = Color(0xFFF57C00),
                                onClick = { viewModel.changeSetType("WARMUP") },
                                modifier = Modifier.weight(1f)
                            )
                            SetTypeChip(
                                label = "递减组",
                                tag = "D",
                                selected = state.inputSetType == "DROP",
                                activeColor = Color(0xFF7B1FA2),
                                onClick = { viewModel.changeSetType("DROP") },
                                modifier = Modifier.weight(1f)
                            )
                            SetTypeChip(
                                label = "力竭组",
                                tag = "F",
                                selected = state.inputSetType == "FAILURE",
                                activeColor = Color(0xFFD32F2F),
                                onClick = { viewModel.changeSetType("FAILURE") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (state.inputSetType == "WARMUP") {
                            Text(
                                text = "ℹ️ 热身组不计入正式有效总容量，不触发新 PR 纪录",
                                fontSize = 11.sp,
                                color = Color(0xFFF57C00),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        StepperRow(
                            label = "重量",
                            value = UnitConverter.weightText(state.inputWeightKg, state.unit),
                            onMinus = { viewModel.changeWeight(-1) },
                            onPlus = { viewModel.changeWeight(1) },
                            extraAction = {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .clickable { showPlateCalculator = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.FitnessCenter,
                                        contentDescription = "杠铃片配重计算",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        "配重计算",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                        StepperRow(
                            label = "次数",
                            value = "${state.inputReps} 次",
                            onMinus = { viewModel.changeReps(-1) },
                            onPlus = { viewModel.changeReps(1) }
                        )

                        // 1RM 预估 & PR 激励
                        if (state.inputWeightKg > 0.0 && state.inputReps > 0) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "估算 1RM: ${UnitConverter.weightText(state.estimated1RM, state.unit)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (state.isNewPR) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "🏆 突破个人记录(PR)!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (state.finished) viewModel.finishAndSave() else viewModel.completeSet()
                            },
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

    // ===== 添加动作选择器（自由训练/临时加动作，支持搜索与肌群筛选） =====
    if (showAddPicker) {
        val all = viewModel.allExercises.collectAsStateWithLifecycle().value
        var pickerQuery by remember { mutableStateOf("") }
        var pickerGroup by remember { mutableStateOf("全部") }
        val muscleGroups = remember { listOf("全部", "胸", "背", "肩", "腿", "手臂", "核心", "有氧") }

        val filtered = remember(all, pickerQuery, pickerGroup) {
            all.filter { ex ->
                val matchGroup = if (pickerGroup == "全部") true else ex.muscleGroup.contains(pickerGroup)
                val matchQuery = if (pickerQuery.isBlank()) true else ex.name.contains(pickerQuery.trim(), ignoreCase = true)
                matchGroup && matchQuery
            }
        }

        AlertDialog(
            onDismissRequest = { showAddPicker = false },
            title = { Text("添加动作", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pickerQuery,
                        onValueChange = { pickerQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索动作名称...") },
                        leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (pickerQuery.isNotEmpty()) {
                                IconButton(onClick = { pickerQuery = "" }) {
                                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(muscleGroups) { g ->
                            AppChip(
                                text = g,
                                selected = pickerGroup == g,
                                onClick = { pickerGroup = g }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("未找到相关动作", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(filtered, key = { it.id }) { ex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addExercise(ex.id)
                                            showAddPicker = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            ex.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${ex.muscleGroup} · ${ex.equipment}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "添加",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddPicker = false }) { Text("关闭") }
            }
        )
    }

    if (showPlateCalculator) {
        PlateCalculatorDialog(
            initialWeight = state.inputWeightKg,
            unit = state.unit,
            onApply = { newWeight ->
                viewModel.setWeight(newWeight)
                showPlateCalculator = false
            },
            onDismiss = { showPlateCalculator = false }
        )
    }
}

/** 组行（序号 + 重量 + 次数，支持正式/热身/递减/力竭组类型标识与勾选） */
@Composable
private fun SetRow(
    num: Int,
    weightText: String,
    reps: Int,
    done: Boolean,
    setType: String = "NORMAL",
    isCurrent: Boolean = false
) {
    val (typeBg, typeColor, typeText) = when (setType) {
        "WARMUP" -> Triple(Color(0xFFFFF3E0), Color(0xFFF57C00), "W")
        "DROP" -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), "D")
        "FAILURE" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), "F")
        else -> Triple(
            if (done) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            "$num"
        )
    }

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
                .background(typeBg, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (setType == "NORMAL" && done) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = typeText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(weightText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (setType != "NORMAL") {
            val typeLabel = when (setType) {
                "WARMUP" -> "热身"
                "DROP" -> "递减"
                "FAILURE" -> "力竭"
                else -> ""
            }
            Text(
                text = typeLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = typeColor,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text("$reps 次", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SetTypeChip(
    label: String,
    tag: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) activeColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (selected) Modifier.border(1.5.dp, activeColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tag,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 步进行（Demo stepper：标签 + 减 + 数值 + 加） */
@Composable
private fun StepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    extraAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (extraAction != null) {
            Spacer(Modifier.width(8.dp))
            extraAction()
        }
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
