package com.relifit.ui.workoutHub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.relifit.data.local.entity.Exercise
import com.relifit.data.local.entity.LogWithSets
import com.relifit.ui.components.AppChip
import com.relifit.ui.components.ScreenTopBar
import com.relifit.ui.components.softCardShadow
import com.relifit.ui.library.LibraryViewModel
import com.relifit.ui.logs.LogsViewModel
import com.relifit.util.TimeUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val muscleGroups = listOf("胸", "背", "肩", "腿", "手臂", "核心", "有氧", "恢复", "收藏", "全部")

/**
 * 锻炼主界面（整合训练记录 + 动作库）
 * 满足 PRD 与全新架构：日历月打卡、日期打卡记录与动作库全景深度联动
 */
@Composable
fun WorkoutHubScreen(
    onStartWorkout: (Long?, Long?, Long?) -> Unit,
    onOpenExercise: (Long) -> Unit,
    onOpenPlans: () -> Unit,
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    logsViewModel: LogsViewModel = viewModel(factory = LogsViewModel.Factory),
    libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
) {
    val logsState by logsViewModel.uiState.collectAsStateWithLifecycle()
    val libState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 模式切换："records" (打卡与动作联动视图) 或 "library" (全量动作库搜索浏览)
    var activeTab by rememberSaveable { mutableStateOf("records") }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var expandedIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(Unit) {
        libraryViewModel.messages.collectLatest { snackbar.showSnackbar(it) }
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
            ScreenTopBar(
                title = "锻炼",
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                extraActions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 自由训练快捷入口
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
                                .clickable { onStartWorkout(null, null, null) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, "自由训练", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        // ＋ 新建动作
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                                .clickable { showCreateDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, "新建动作", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )

            // 训练计划管理横幅
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlans)
                    .softCardShadow(18)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "训练计划",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "力量 · 核心 · 有氧 · 恢复",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "管理",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // 训练记录 / 动作库 切换胶囊
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    .padding(4.dp)
            ) {
                val tabs = listOf("records" to "📅 训练记录", "library" to "💪 动作库")
                tabs.forEach { (key, label) ->
                    val selected = activeTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) MaterialTheme.colorScheme.surface
                                else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                            .clickable { activeTab = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 主展示内容
            if (activeTab == "records") {
                // ===== 模式 1：打卡记录 + 动作库联动（正如用户设计手稿） =====
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 月历打卡视图
                    item(key = "calendar_view") {
                        WorkoutMonthCalendar(
                            loggedDates = logsState.loggedDates,
                            selectedDate = logsState.selectedDate,
                            onSelectDate = { logsViewModel.setSelectedDate(it) }
                        )
                    }

                    // 选中日期的训练打卡记录
                    if (logsState.logs.isNotEmpty()) {
                        item(key = "logs_header") {
                            val title = logsState.selectedDate?.let { "${TimeUtils.formatDate(it)} 训练记录" } ?: "历史训练记录"
                            Text(
                                title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(logsState.logs, key = { "log_${it.log.id}" }) { logWithSets ->
                            val expanded = logWithSets.log.id in expandedIds
                            LogCardItem(
                                log = logWithSets,
                                names = logsState.exerciseNames,
                                expanded = expanded,
                                onToggle = {
                                    expandedIds = if (expanded) expandedIds - logWithSets.log.id
                                    else expandedIds + logWithSets.log.id
                                },
                                onDelete = { deleteTarget = logWithSets.log.id }
                            )
                        }
                    } else {
                        // 选定日期无记录时（红框标注位置）
                        item(key = "empty_logs_view") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .softCardShadow(18)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                                    .padding(vertical = 14.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (logsState.selectedDate != null) "该日暂无训练记录" else "今天还没有开始训练",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "挑选下方动作即可开始今日锻炼，完成自动记入打卡",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // ===== 记录下方直接展示「动作库」区域（用户截图红框要求） =====
                    item(key = "exercises_header") {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "动作库 · 点击直接锻炼",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "查看全部 >",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { activeTab = "library" }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // 肌群快速滑动筛选
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                muscleGroups.forEach { g ->
                                    AppChip(
                                        text = g,
                                        selected = libState.group == g,
                                        onClick = { libraryViewModel.selectGroup(g) }
                                    )
                                }
                            }
                        }
                    }

                    // 动作候选流（添加前缀避免与历史打卡记录 key 冲突）
                    val previewExercises = libState.exercises.take(10)
                    items(previewExercises, key = { "preview_ex_${it.id}" }) { ex ->
                        ExerciseRowItem(
                            ex = ex,
                            onClick = { onOpenExercise(ex.id) },
                            onToggleFavorite = { libraryViewModel.toggleFavorite(ex.id) },
                            onToggleOffline = { libraryViewModel.toggleOffline(ex.id) },
                            onQuickStart = { onStartWorkout(null, null, ex.id) }
                        )
                    }

                    item(key = "records_bottom_spacer") { Spacer(Modifier.height(16.dp)) }
                }
            } else {
                // ===== 模式 2：全量动作库搜索与分类视图 =====
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // 动作搜索框
                    TextField(
                        value = libState.query,
                        onValueChange = { libraryViewModel.onQueryChange(it) },
                        placeholder = { Text("搜索动作（如：卧推 / 引体向上 / 深蹲）", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (libState.query.isNotEmpty()) {
                                TextButton(onClick = { libraryViewModel.onQueryChange("") }) {
                                    Text("清除", fontSize = 12.sp)
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    // 肌群横向标签
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        muscleGroups.forEach { g ->
                            AppChip(
                                text = g,
                                selected = libState.group == g,
                                onClick = { libraryViewModel.selectGroup(g) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 动作列表
                    if (libState.exercises.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "未找到相关动作",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(libState.exercises, key = { "full_ex_${it.id}" }) { ex ->
                                ExerciseRowItem(
                                    ex = ex,
                                    onClick = { onOpenExercise(ex.id) },
                                    onToggleFavorite = { libraryViewModel.toggleFavorite(ex.id) },
                                    onToggleOffline = { libraryViewModel.toggleOffline(ex.id) },
                                    onQuickStart = { onStartWorkout(null, null, ex.id) }
                                )
                            }
                            item(key = "lib_bottom_spacer") { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    // 新建自定义动作弹窗
    if (showCreateDialog) {
        CreateExerciseDialogItem(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, group, equip, diff, points ->
                libraryViewModel.createExercise(name, group, equip, diff, points)
                showCreateDialog = false
                scope.launch { snackbar.showSnackbar("已创建自定义动作：$name") }
            }
        )
    }

    // 删除训练记录确认弹窗
    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这条训练记录？") },
            text = { Text("删除后不可恢复，历史统计数据会同步更新。") },
            confirmButton = {
                TextButton(onClick = {
                    logsViewModel.deleteLog(id)
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

        // 日历网格
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
                repeat(7 - week.size) {
                    Spacer(Modifier.size(36.dp))
                }
            }
        }
    }
}

/** 日志卡片行 */
@Composable
private fun LogCardItem(
    log: LogWithSets,
    names: Map<Long, String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val l = log.log
    val grouped = log.sets.groupBy { it.exerciseId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .clickable(onClick = onToggle)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(TimeUtils.formatDate(l.date), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("删除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable(onClick = onDelete))
        }
        Spacer(Modifier.height(4.dp))
        Text(l.note.ifBlank { "训练记录" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${l.durationMin} 分钟", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp))
            Text("${l.totalSets} 组", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp))
            Text("${TimeUtils.thousands(l.totalVolumeKg)} kg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            grouped.forEach { (exId, sets) ->
                Column {
                    Text(names[exId] ?: "动作#$exId", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sets.sortedBy { it.setIndex }.forEach { s ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    if (s.weightKg > 0) "${formatW(s.weightKg)}×${s.reps}" else "自重×${s.reps}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/** 动作卡片组件 */
@Composable
private fun ExerciseRowItem(
    ex: Exercise,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleOffline: () -> Unit,
    onQuickStart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(20)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ex.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${ex.muscleGroup} · ${ex.equipment}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    ex.difficulty,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        // 离线缓存
        IconButton(onClick = onToggleOffline, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (ex.offlineAvailable) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                contentDescription = "离线缓存",
                tint = if (ex.offlineAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        // 收藏星标
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (ex.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "收藏",
                tint = if (ex.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        // 快速单动作训练
        IconButton(onClick = onQuickStart, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.PlayArrow, "开始该动作", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

/** 新建动作弹窗 */
@Composable
private fun CreateExerciseDialogItem(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("胸") }
    var equipment by remember { mutableStateOf("杠铃") }
    var difficulty by remember { mutableStateOf("入门") }
    var keyPoints by remember { mutableStateOf("") }
    val allGroups = remember { listOf("胸", "背", "肩", "腿", "手臂", "核心", "有氧") }
    val allEquipments = remember { listOf("杠铃", "哑铃", "器械", "自重", "绳索", "弹力带", "其他") }
    val allDifficulties = remember { listOf("入门", "中级", "进阶") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建自定义动作", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("动作名称") },
                    placeholder = { Text("如：上斜哑铃卧推") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("目标肌群", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allGroups.forEach { g ->
                        AppChip(text = g, selected = group == g, onClick = { group = g })
                    }
                }

                Text("所需器械", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allEquipments.forEach { eq ->
                        AppChip(text = eq, selected = equipment == eq, onClick = { equipment = eq })
                    }
                }

                Text("训练难度", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allDifficulties.forEach { diff ->
                        AppChip(text = diff, selected = difficulty == diff, onClick = { difficulty = diff })
                    }
                }

                OutlinedTextField(
                    value = keyPoints,
                    onValueChange = { keyPoints = it },
                    label = { Text("发力要点 (选填)") },
                    placeholder = { Text("如：挺胸收腹，感受肌肉顶峰收缩") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, group, equipment, difficulty, keyPoints)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun formatW(w: Double): String {
    if (w.isNaN() || w.isInfinite() || w <= 0.0) return "0"
    val r = (w * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
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
