package com.relifit.ui.plan

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.data.local.entity.DayWithEntries
import com.relifit.ui.components.AppChip
import com.relifit.ui.components.RingProgress
import com.relifit.ui.components.softCardShadow
import kotlinx.coroutines.flow.collectLatest

/**
 * 计划详情页（Demo 布局）：元信息 + 周期环 + 可展开训练日 + 编辑管理
 */
@Composable
fun PlanDetailScreen(
    planId: Long,
    onBack: () -> Unit,
    onStartWorkout: (Long, Long) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editMode by remember { mutableStateOf(false) }
    var addDayDialog by remember { mutableStateOf(false) }
    var addEntryDay by remember { mutableStateOf<DayWithEntries?>(null) }
    var editEntry by remember { mutableStateOf<com.relifit.data.local.entity.ExerciseEntry?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp)
        ) {
            // 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    state.plan?.name ?: "计划详情",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // 复制（仅模板）；编辑（模板与自定义计划均支持）
                if (state.plan?.isTemplate == true) {
                    IconButton(onClick = { viewModel.copyTemplate() }) {
                        Icon(Icons.Filled.ContentCopy, "复制计划", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = { editMode = !editMode }) {
                    Icon(
                        Icons.Filled.Edit,
                        "编辑",
                        tint = if (editMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 元信息 chips
            state.plan?.let { p ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip("${p.cycleWeeks} 周周期")
                    InfoChip("每周 ${p.daysPerWeek} 练")
                    InfoChip("单次 ${p.targetDurationMin} 分钟")
                }
                // 编辑模式下可修改计划类型（力量/核心/有氧/恢复）
                if (editMode) {
                    Spacer(Modifier.height(14.dp))
                    Text("计划类型", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        planTypes.forEach { t ->
                            AppChip(text = t, selected = p.type == t, onClick = { viewModel.setPlanType(t) })
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // 周期环卡
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RingProgress(
                        progress = state.cycleWeek.toFloat() / p.cycleWeeks.coerceAtLeast(1),
                        centerTop = "${state.cycleWeek}/${p.cycleWeeks}",
                        centerBottom = "周",
                        size = 84
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (p.isTemplate) "内置模板 · 可复制为自定义计划" else "自定义计划 · 可编辑",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // 训练日列表
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.days, key = { it.day.id }) { day ->
                        DayCard(
                            day = day,
                            names = state.exerciseNames,
                            editMode = editMode,
                            onStart = { onStartWorkout(p.id, day.day.id) },
                            onDeleteDay = { viewModel.deleteDay(day.day.id) },
                            onAddEntry = { addEntryDay = day },
                            onRemoveEntry = { viewModel.removeEntry(it) },
                            onEditEntry = { editEntry = it }
                        )
                    }
                    if (editMode) {
                        item {
                            Button(
                                onClick = { addDayDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Icon(Icons.Filled.Add, null)
                                Spacer(Modifier.width(6.dp))
                                Text("添加训练日", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // ===== 添加训练日弹窗 =====
    if (addDayDialog) {
        var name by remember { mutableStateOf("") }
        var idx by remember { mutableStateOf("1") }
        var rest by remember { mutableStateOf("90") }
        AlertDialog(
            onDismissRequest = { addDayDialog = false },
            title = { Text("添加训练日") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("训练日名称（如：上肢推日）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = idx, onValueChange = { idx = it.filter { c -> c.isDigit() } }, label = { Text("周内第几天（1-7，1=周一）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = rest, onValueChange = { rest = it.filter { c -> c.isDigit() } }, label = { Text("默认组间休息（秒）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val i = idx.toIntOrNull()?.coerceIn(1, 7) ?: 1
                    val r = rest.toIntOrNull()?.coerceIn(10, 600) ?: 90
                    viewModel.addDay(i, name.ifBlank { "新训练日" }, r)
                    addDayDialog = false
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { addDayDialog = false }) { Text("取消") } }
        )
    }

    // ===== 添加动作弹窗（选动作 + 组数/次数/休息） =====
    addEntryDay?.let { day ->
        AddEntryDialog(
            day = day,
            names = state.exerciseNames,
            onDismiss = { addEntryDay = null },
            onConfirm = { exId, sets, reps, rest ->
                viewModel.addEntry(day.day.id, exId, sets, reps, rest)
                addEntryDay = null
            }
        )
    }

    // ===== 编辑动作条目弹窗 =====
    editEntry?.let { entry ->
        var sets by remember { mutableStateOf(entry.targetSets.toString()) }
        var reps by remember { mutableStateOf(entry.targetReps.toString()) }
        var rest by remember { mutableStateOf(entry.restSec.toString()) }
        AlertDialog(
            onDismissRequest = { editEntry = null },
            title = { Text("编辑动作参数") },
            text = {
                Column {
                    OutlinedTextField(value = sets, onValueChange = { sets = it.filter { c -> c.isDigit() } }, label = { Text("目标组数") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = reps, onValueChange = { reps = it.filter { c -> c.isDigit() } }, label = { Text("目标次数") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = rest, onValueChange = { rest = it.filter { c -> c.isDigit() } }, label = { Text("组间休息（秒）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateEntry(
                        entry.id,
                        sets.toIntOrNull() ?: 3,
                        reps.toIntOrNull() ?: 10,
                        rest.toIntOrNull() ?: 60,
                        entry.targetWeight
                    )
                    editEntry = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editEntry = null }) { Text("取消") } }
        )
    }
}

/** 训练日卡片（Demo pday：可展开动作条目） */
@Composable
private fun DayCard(
    day: DayWithEntries,
    names: Map<Long, String>,
    editMode: Boolean,
    onStart: () -> Unit,
    onDeleteDay: () -> Unit,
    onAddEntry: () -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onEditEntry: (com.relifit.data.local.entity.ExerciseEntry) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 周几徽章
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(dayName(day.day.dayIndex), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(day.day.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${day.entries.size} 个动作 · 组间休 ${day.day.defaultRestSec}s",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            day.entries.forEach { e ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        names[e.exerciseId] ?: "动作#${e.exerciseId}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${e.targetSets} 组 × ${e.targetReps} 次 · 组间 ${e.restSec}s",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (editMode) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Edit,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp).clickable { onEditEntry(e) }
                        )
                        Icon(
                            Icons.Filled.Delete,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp).clickable { onRemoveEntry(e.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("开始训练", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (editMode) {
                    Button(
                        onClick = onAddEntry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("添加动作", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    TextButton(onClick = onDeleteDay) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/** 添加动作弹窗 */
@Composable
private fun AddEntryDialog(
    day: DayWithEntries,
    names: Map<Long, String>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int, Int, Int) -> Unit
) {
    var exId by remember { mutableStateOf(0L) }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var rest by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加动作到「${day.day.name}」") },
        text = {
            Column {
                Text("选择动作（从动作库）", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.height(200.dp)) {
                    items(names.entries.toList(), key = { it.key }) { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exId = id }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                name,
                                fontSize = 15.sp,
                                color = if (exId == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sets, onValueChange = { sets = it.filter { c -> c.isDigit() } }, label = { Text("组数") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = reps, onValueChange = { reps = it.filter { c -> c.isDigit() } }, label = { Text("次数") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = rest, onValueChange = { rest = it.filter { c -> c.isDigit() } }, label = { Text("组间休息（秒）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (exId > 0) onConfirm(exId, sets.toIntOrNull() ?: 3, reps.toIntOrNull() ?: 10, rest.toIntOrNull() ?: 60)
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun dayName(dayIndex: Int): String = when (dayIndex) {
    1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"; 5 -> "周五"; 6 -> "周六"; else -> "周日"
}
