package com.relifit.ui.logs

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.data.local.entity.LogWithSets
import com.relifit.ui.components.AppChip
import com.relifit.ui.components.ScreenTopBar
import com.relifit.ui.components.softCardShadow
import com.relifit.util.TimeUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 训练记录页（Demo 布局）：月历打卡视图 + 列表筛选 + 可折叠日志卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    onOpenPlans: () -> Unit = {},
    viewModel: LogsViewModel = viewModel(factory = LogsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var viewMode by rememberSaveable { mutableStateOf("calendar") } // "calendar" or "list"
    val expandedIds = rememberSaveable { mutableStateOf(setOf<Long>()) }

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
            ScreenTopBar(
                title = "训练记录",
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                extraActions = {
                    // 视图切换按钮：月历打卡 / 简洁列表
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                            .clickable {
                                viewMode = if (viewMode == "calendar") "list" else "calendar"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (viewMode == "calendar") Icons.AutoMirrored.Filled.ViewList else Icons.Filled.CalendarMonth,
                            contentDescription = if (viewMode == "calendar") "切换列表视图" else "切换打卡月历",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // 训练计划管理入口（力量 / 核心 / 有氧 / 恢复）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenPlans)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("训练计划", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("力量 · 核心 · 有氧 · 恢复", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("管理", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))

            if (viewMode == "calendar") {
                // ===== 月历打卡视图 =====
                WorkoutMonthCalendar(
                    loggedDates = state.loggedDates,
                    selectedDate = state.selectedDate,
                    onSelectDate = { viewModel.setSelectedDate(it) }
                )
                Spacer(Modifier.height(12.dp))
                val selDate = state.selectedDate
                if (selDate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${TimeUtils.formatShort(selDate)} 训练记录",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "查看全部记录",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { viewModel.setSelectedDate(null) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            } else {
                // ===== 列表筛选模式 =====
                val selDate = state.selectedDate
                if (selDate != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppChip(text = "${TimeUtils.formatShort(selDate)} 的记录", selected = true, onClick = { showDatePicker = true })
                        AppChip(text = "清除", selected = false, onClick = { viewModel.setSelectedDate(null) })
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("全部", "本月", "上月").forEach { f ->
                            AppChip(text = f, selected = state.filter == f, onClick = { viewModel.setFilter(f) })
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(14.dp))

            if (state.logs.isEmpty()) {
                // ===== 空态：无记录 / 筛选无结果 =====
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("暂无训练记录", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.selectedDate != null) "这一天还没有训练，换个日期看看"
                        else "完成一次训练后会自动生成记录",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.logs, key = { it.log.id }) { logWithSets ->
                        val expanded = logWithSets.log.id in expandedIds.value
                        LogCard(
                            log = logWithSets,
                            names = state.exerciseNames,
                            expanded = expanded,
                            onToggle = {
                                expandedIds.value = if (expanded) expandedIds.value - logWithSets.log.id
                                else expandedIds.value + logWithSets.log.id
                            },
                            onDelete = { deleteTarget = logWithSets.log.id }
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }

    // ===== 日期选择弹窗（筛选对应日期的训练记录） =====
    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate?.let { TimeUtils.toUtcDateMillis(it) }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { utc ->
                        viewModel.setSelectedDate(TimeUtils.fromUtcDateMillis(utc))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    // 删除确认
    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这条训练记录？") },
            text = { Text("删除后不可恢复，历史统计数据会同步更新。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLog(id)
                    deleteTarget = null
                    scope.launch { snackbar.showSnackbar("已删除训练记录") }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

/** 日志卡片（Demo log-card：可折叠展开组数据） */
@Composable
private fun LogCard(
    log: LogWithSets,
    names: Map<Long, String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val l = log.log
    // 按动作分组展示组记录
    val grouped = log.sets.groupBy { it.exerciseId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(28)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .clickable(onClick = onToggle)
            .padding(18.dp)
    ) {
        // 头部：日期 + 计划
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(TimeUtils.formatDate(l.date), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(
                "删除",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onDelete)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(l.note.ifBlank { "训练" }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        // 汇总 chips：时长 / 组数 / 容量
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryChip("${l.durationMin} 分钟")
            SummaryChip("${l.totalSets} 组")
            SummaryChip("${TimeUtils.thousands(l.totalVolumeKg)} kg")
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
        }

        // 展开的组数据
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            grouped.forEach { (exId, sets) ->
                Column {
                    Text(
                        names[exId] ?: "动作#$exId",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sets.sortedBy { it.setIndex }.forEach { s ->
                            val (tagBg, tagColor, tagText) = when (s.setType) {
                                "WARMUP" -> Triple(Color(0xFFFFF3E0), Color(0xFFF57C00), "W")
                                "DROP" -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), "D")
                                "FAILURE" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), "F")
                                else -> Triple(Color.Transparent, Color.Transparent, null)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                if (tagText != null) {
                                    Text(
                                        text = tagText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tagColor,
                                        modifier = Modifier
                                            .background(tagBg, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                } else {
                                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    if (s.weightKg > 0) "${formatW(s.weightKg)}×${s.reps}" else "自重×${s.reps}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

private fun formatW(w: Double): String {
    val r = (w * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
}

/** 月度打卡日历视图组件 */
@Composable
private fun WorkoutMonthCalendar(
    loggedDates: Set<Long>,
    selectedDate: Long?,
    onSelectDate: (Long?) -> Unit
) {
    var calMonthOffset by remember { mutableStateOf(0) }
    val now = remember { java.util.Calendar.getInstance() }
    val displayCal = remember(calMonthOffset) {
        (now.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.MONTH, calMonthOffset)
        }
    }
    val year = displayCal.get(java.util.Calendar.YEAR)
    val month = displayCal.get(java.util.Calendar.MONTH)

    val daysInMonth = remember(year, month) {
        getDaysInMonth(year, month)
    }

    val monthLoggedCount = remember(daysInMonth, loggedDates) {
        daysInMonth.filterNotNull().count { it in loggedDates }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        // 头部：年月切换 + 本月打卡统计
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { calMonthOffset -= 1 },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.ChevronLeft, "上一月", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${year}年 ${month + 1}月",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "本月已打卡 $monthLoggedCount 天",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(
                onClick = { calMonthOffset += 1 },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.ChevronRight, "下一月", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(10.dp))

        // 星期标头
        val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            weekdays.forEach { w ->
                Text(
                    w,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 日历网格（以 7 天为一行）
        val chunks = daysInMonth.chunked(7)
        chunks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                week.forEach { dayMillis ->
                    if (dayMillis == null) {
                        Spacer(Modifier.size(36.dp))
                    } else {
                        val isLogged = dayMillis in loggedDates
                        val isSelected = selectedDate == dayMillis
                        val dayNum = java.util.Calendar.getInstance().apply { timeInMillis = dayMillis }
                            .get(java.util.Calendar.DAY_OF_MONTH)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isLogged -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    if (isSelected) onSelectDate(null)
                                    else onSelectDate(dayMillis)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$dayNum",
                                    fontSize = 13.sp,
                                    fontWeight = if (isLogged || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isLogged -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (isLogged && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }
                // 补齐末尾空白
                repeat(7 - week.size) {
                    Spacer(Modifier.size(36.dp))
                }
            }
        }
    }
}

private fun getDaysInMonth(year: Int, month: Int): List<Long?> {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.YEAR, year)
    cal.set(java.util.Calendar.MONTH, month)
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)

    val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7

    val list = mutableListOf<Long?>()
    for (i in 0 until firstDayOfWeek) {
        list.add(null)
    }
    for (day in 1..maxDays) {
        cal.set(java.util.Calendar.DAY_OF_MONTH, day)
        list.add(cal.timeInMillis)
    }
    return list
}

