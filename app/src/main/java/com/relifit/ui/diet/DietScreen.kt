package com.relifit.ui.diet

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.relifit.data.local.FoodData
import com.relifit.data.local.FoodInfo
import com.relifit.data.local.entity.MealWithItems
import com.relifit.data.local.entity.Recipe
import com.relifit.data.remote.FoodNutritionEstimate
import com.relifit.ui.components.AppChip
import com.relifit.ui.components.ScreenTopBar
import com.relifit.ui.components.SectionTitle
import com.relifit.ui.components.softCardShadow
import com.relifit.util.TimeUtils
import java.util.Calendar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 饮食记录页（Demo 布局）：今日摄入 Hero + 营养素 + 餐次食物（+/− 份量）+ 健身菜谱（控油盐蛋白精准记账）
 */
@Composable
fun DietScreen(
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    viewModel: DietViewModel = viewModel(factory = DietViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showCreateRecipeDialog by remember { mutableStateOf(false) }
    var recipeToLog by remember { mutableStateOf<Recipe?>(null) }
    var recipeDetail by remember { mutableStateOf<Recipe?>(null) }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            ScreenTopBar(
                title = "饮食记录",
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                extraActions = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                            .clickable { showGoalDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Settings, "目标设置", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )

            // ===== 日期导航条（支持前一天/后一天/回到今天，随时查看与补录历史饮食） =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.previousDay() },
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "前一天",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dateText = remember(state.selectedDate, state.isToday) {
                        if (state.selectedDate == 0L) "今天"
                        else {
                            val cal = Calendar.getInstance().apply { timeInMillis = state.selectedDate }
                            val month = cal.get(Calendar.MONTH) + 1
                            val day = cal.get(Calendar.DAY_OF_MONTH)
                            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.MONDAY -> "周一"
                                Calendar.TUESDAY -> "周二"
                                Calendar.WEDNESDAY -> "周三"
                                Calendar.THURSDAY -> "周四"
                                Calendar.FRIDAY -> "周五"
                                Calendar.SATURDAY -> "周六"
                                else -> "周日"
                            }
                            val todayStart = TimeUtils.startOfDay(System.currentTimeMillis())
                            val prefix = when (state.selectedDate) {
                                todayStart -> "今天 · "
                                todayStart - 86400000L -> "昨天 · "
                                todayStart + 86400000L -> "明天 · "
                                else -> ""
                            }
                            "$prefix${month}月${day}日 $dayOfWeek"
                        }
                    }

                    Text(
                        text = dateText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!state.isToday) {
                        Text(
                            text = "回到今天",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(50))
                                .clickable { viewModel.jumpToToday() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.nextDay() },
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "后一天",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ===== 今日摄入 Hero（Demo diet-hero） =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .softCardShadow(28)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(if (state.isToday) "今日摄入" else "当日摄入", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${state.kcal.roundToInt()}",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("/ ${state.goalKcal} kcal", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                    Text(
                        "目标 ${(state.kcal / state.goalKcal.coerceAtLeast(1) * 100).roundToInt().coerceAtMost(100)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                            .clickable { showGoalDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                // 进度条
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((state.kcal / state.goalKcal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f))
                            .height(10.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(androidx.compose.ui.graphics.Color(0xFF6E9CC4), MaterialTheme.colorScheme.primary)
                                ),
                                RoundedCornerShape(50)
                            )
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (state.remainKcal >= 0) "还差 ${state.remainKcal} kcal 达到目标"
                    else "已超出 ${-state.remainKcal} kcal",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                // 三大营养素
                NutrientRow("碳水", state.carbs, state.goalCarbs.toDouble())
                Spacer(Modifier.height(8.dp))
                NutrientRow("蛋白质", state.protein, state.goalProtein.toDouble())
                Spacer(Modifier.height(8.dp))
                NutrientRow("脂肪", state.fat, state.goalFat.toDouble())
            }

            Spacer(Modifier.height(16.dp))

            // ===== 每日水分摄入追踪 (Water Tracker) =====
            com.relifit.ui.components.WaterTrackerCard(
                amountMl = state.waterAmountMl,
                goalMl = state.waterGoalMl,
                onAddWater = { viewModel.addWater(it) },
                onResetWater = { viewModel.resetWater() }
            )

            Spacer(Modifier.height(18.dp))

            // ===== 餐次明细 =====
            SectionTitle(if (state.isToday) "今日餐次" else "当日餐次", actionText = "＋ 添加") { showAddDialog = true }
            state.meals.forEach { meal ->
                MealCard(
                    meal = meal,
                    onAdd = { viewModel.addServings(it) },
                    onRemove = { viewModel.removeServings(it) },
                    onCopyYesterday = { viewModel.copyYesterdayMeal(meal.meal.id, meal.meal.mealType) }
                )
            }

            Spacer(Modifier.height(18.dp))

            // ===== 健身控油盐菜谱（以谱定餐 · 精准控油盐与高蛋白） =====
            SectionTitle("健身控油盐菜谱", actionText = "＋ 自建菜谱") { showCreateRecipeDialog = true }
            Spacer(Modifier.height(4.dp))
            Text(
                "告别拍照识别不准与外卖隐形油盐：按标准化健身菜谱制作，精准记录油盐与蛋白质摄入",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            val recipeCategories = remember { listOf("全部", "高蛋白增肌", "极低脂减脂", "优质脂肪", "均衡轻食") }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipeCategories.forEach { cat ->
                    AppChip(
                        text = cat,
                        selected = state.selectedRecipeCategory == cat,
                        onClick = { viewModel.selectRecipeCategory(cat) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (state.recipes.isEmpty()) {
                Text(
                    "暂无相关菜谱",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                state.recipes.forEach { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onLogClick = { recipeToLog = recipe },
                        onDetailClick = { recipeDetail = recipe },
                        onDeleteClick = if (recipe.isCustom) { { viewModel.deleteRecipe(recipe.id) } } else null
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ===== 添加食物弹窗 =====
    if (showAddDialog) {
        AddFoodDialog(
            meals = state.meals,
            hasApiKey = state.isAiAvailable,
            isAiEstimating = state.isAiEstimating,
            onAiEstimate = { query, onSuccess ->
                viewModel.estimateFoodNutritionWithAi(query, onSuccess)
            },
            onConfigureKey = {
                scope.launch { snackbar.showSnackbar("可前往「我的/设置」页面配置自定义 AI Key") }
            },
            onDismiss = { showAddDialog = false },
            onConfirm = { mealId, name, qty, kcal, c, p, f ->
                viewModel.addFood(mealId, name, qty, kcal, c, p, f)
                showAddDialog = false
                scope.launch { snackbar.showSnackbar("已添加：$name") }
            }
        )
    }

    // ===== 目标设置弹窗（热量 + 三大营养素均可自定义） =====
    if (showGoalDialog) {
        var kcalInput by remember { mutableStateOf(state.goalKcal.toString()) }
        var carbsInput by remember { mutableStateOf(state.goalCarbs.toString()) }
        var proteinInput by remember { mutableStateOf(state.goalProtein.toString()) }
        var fatInput by remember { mutableStateOf(state.goalFat.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("每日营养目标") },
            text = {
                Column {
                    Text(
                        "建议：减脂 = 体重kg × 22-25，增肌 = 体重kg × 30-35；蛋白质 1.6-2g/kg",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = kcalInput,
                        onValueChange = { kcalInput = it.filter { c -> c.isDigit() } },
                        label = { Text("热量目标（kcal）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbsInput,
                            onValueChange = { carbsInput = it.filter { c -> c.isDigit() } },
                            label = { Text("碳水（g）") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = proteinInput,
                            onValueChange = { proteinInput = it.filter { c -> c.isDigit() } },
                            label = { Text("蛋白质（g）") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fatInput,
                        onValueChange = { fatInput = it.filter { c -> c.isDigit() } },
                        label = { Text("脂肪（g）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveGoal(
                        kcalInput.toIntOrNull() ?: 2200,
                        carbsInput.toIntOrNull() ?: 0,
                        proteinInput.toIntOrNull() ?: 0,
                        fatInput.toIntOrNull() ?: 0
                    )
                    showGoalDialog = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("取消") } }
        )
    }

    // ===== 记入餐次确认弹窗 =====
    recipeToLog?.let { recipe ->
        var selectedMealIdx by remember { mutableStateOf(0) }
        var servings by remember { mutableStateOf(1) }
        AlertDialog(
            onDismissRequest = { recipeToLog = null },
            title = { Text("记入今日餐次", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "将「${recipe.name}」打卡记入所选餐次：",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "单份营养：${recipe.kcal.roundToInt()} kcal · 控油 ${recipe.oilGram}g · 控盐 ${recipe.saltGram}g · 蛋白 ${recipe.proteinG.roundToInt()}g",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("选择餐次", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.meals.forEachIndexed { idx, m ->
                            AppChip(
                                text = m.meal.mealType,
                                selected = selectedMealIdx == idx,
                                onClick = { selectedMealIdx = idx }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("份数", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { if (servings > 1) servings -= 1 },
                                enabled = servings > 1
                            ) { Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                            Text("$servings 份", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { servings += 1 }) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mealId = state.meals.getOrNull(selectedMealIdx)?.meal?.id
                        if (mealId != null) {
                            viewModel.logRecipeToMeal(mealId, recipe, servings)
                            recipeToLog = null
                        }
                    }
                ) { Text("确认记入") }
            },
            dismissButton = {
                TextButton(onClick = { recipeToLog = null }) { Text("取消") }
            }
        )
    }

    // ===== 菜谱详情弹窗 =====
    recipeDetail?.let { recipe ->
        AlertDialog(
            onDismissRequest = { recipeDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(recipe.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(recipe.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        NutrientBadge("控油", "${recipe.oilGram}g", true)
                        NutrientBadge("控盐", "${recipe.saltGram}g", true)
                        NutrientBadge("蛋白质", "${recipe.proteinG.roundToInt()}g", false)
                        NutrientBadge("碳水", "${recipe.carbsG.roundToInt()}g", false)
                        NutrientBadge("脂肪", "${recipe.fatG.roundToInt()}g", false)
                        NutrientBadge("热量", "${recipe.kcal.roundToInt()}k", false)
                    }

                    Text("精确食材清单", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        recipe.ingredients,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(4.dp))
                    Text("烹饪与制作要点", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        recipe.instructions,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = recipe
                        recipeDetail = null
                        recipeToLog = r
                    }
                ) { Text("记入餐次") }
            },
            dismissButton = {
                TextButton(onClick = { recipeDetail = null }) { Text("关闭") }
            }
        )
    }

    // ===== 自建菜谱弹窗 =====
    if (showCreateRecipeDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("高蛋白增肌") }
        var prepTime by remember { mutableStateOf("15") }
        var difficulty by remember { mutableStateOf("简单") }
        var oilGram by remember { mutableStateOf("3.0") }
        var saltGram by remember { mutableStateOf("1.5") }
        var proteinG by remember { mutableStateOf("35.0") }
        var carbsG by remember { mutableStateOf("10.0") }
        var fatG by remember { mutableStateOf("5.0") }
        var kcal by remember { mutableStateOf("230") }
        var ingredients by remember { mutableStateOf("") }
        var instructions by remember { mutableStateOf("") }
        var aiPrompt by remember { mutableStateOf("") }

        val categories = remember { listOf("高蛋白增肌", "极低脂减脂", "优质脂肪", "均衡轻食") }

        AlertDialog(
            onDismissRequest = { showCreateRecipeDialog = false },
            title = { Text("自建健身控油盐菜谱", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ===== DeepSeek AI 智能生成菜谱 =====
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("DeepSeek AI 智能生成菜谱", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "输入菜名或灵感（如：少油高蛋白孜然牛肉粒），AI 按控油控盐与精准营养自动生成全套配方与做法。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            placeholder = { Text("输入菜名或想吃的健身餐灵感", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!state.isAiAvailable) {
                                Text(
                                    "可在设置中配置 AI 引擎",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Button(
                                    onClick = {
                                        if (aiPrompt.isNotBlank() && !state.isAiGeneratingRecipe) {
                                            viewModel.generateRecipeWithAi(aiPrompt) { generated ->
                                                name = generated.name
                                                category = generated.category
                                                prepTime = generated.prepTimeMin.toString()
                                                difficulty = generated.difficulty
                                                oilGram = generated.oilGram.toString()
                                                saltGram = generated.saltGram.toString()
                                                proteinG = generated.proteinG.toString()
                                                carbsG = generated.carbsG.toString()
                                                fatG = generated.fatG.toString()
                                                kcal = generated.kcal.roundToInt().toString()
                                                ingredients = generated.ingredients
                                                instructions = generated.instructions
                                            }
                                        }
                                    },
                                    enabled = aiPrompt.isNotBlank() && !state.isAiGeneratingRecipe,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    if (state.isAiGeneratingRecipe) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("正在设计菜谱...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("AI 一键生成菜谱", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("菜谱名称") },
                        placeholder = { Text("如：彩椒爆炒牛柳") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("分类", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { c ->
                            AppChip(text = c, selected = category == c, onClick = { category = c })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = oilGram,
                            onValueChange = { oilGram = it },
                            label = { Text("控油 (g)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = saltGram,
                            onValueChange = { saltGram = it },
                            label = { Text("控盐 (g)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = proteinG,
                            onValueChange = { proteinG = it },
                            label = { Text("蛋白质 (g)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kcal,
                            onValueChange = { kcal = it },
                            label = { Text("热量 (kcal)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbsG,
                            onValueChange = { carbsG = it },
                            label = { Text("碳水 (g)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fatG,
                            onValueChange = { fatG = it },
                            label = { Text("脂肪 (g)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        label = { Text("食材明细 (含克数)") },
                        placeholder = { Text("如：牛肉 180g\n彩椒 80g\n橄榄油 3g\n食用盐 1.5g") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("烹饪要点与做法") },
                        placeholder = { Text("如：热锅刷油快速滑炒牛肉，下彩椒大火翻炒出锅") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createRecipe(
                                name = name,
                                category = category,
                                prepTimeMin = prepTime.toIntOrNull() ?: 15,
                                difficulty = difficulty,
                                oilGram = oilGram.toDoubleOrNull() ?: 3.0,
                                saltGram = saltGram.toDoubleOrNull() ?: 1.5,
                                proteinG = proteinG.toDoubleOrNull() ?: 30.0,
                                carbsG = carbsG.toDoubleOrNull() ?: 10.0,
                                fatG = fatG.toDoubleOrNull() ?: 5.0,
                                kcal = kcal.toDoubleOrNull() ?: 200.0,
                                ingredients = ingredients.ifBlank { "按个人口味配料" },
                                instructions = instructions.ifBlank { "少油快炒，清淡烹饪" }
                            )
                            showCreateRecipeDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("创建菜谱") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRecipeDialog = false }) { Text("取消") }
            }
        )
    }
}

/** 营养素行（Demo nut：名称 + 当前/目标 + 进度条） */
@Composable
private fun NutrientRow(name: String, current: Double, target: Double) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(
                "${current.roundToInt()} / ${target.roundToInt()}g",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
        ) {
            Box(
                Modifier
                    .fillMaxWidth((current / target.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            )
        }
    }
}

/** 餐次卡片（Demo meal：头部 + 食物行 +/− 份量 + 复制昨天） */
@Composable
private fun MealCard(
    meal: MealWithItems,
    onAdd: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onCopyYesterday: () -> Unit
) {
    val total = meal.items.sumOf { it.kcal * it.servings }.roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                meal.meal.mealType,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "  ${meal.meal.timeLabel}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$total kcal",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onCopyYesterday)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "复制昨天",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "复制昨天",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (meal.items.isEmpty()) {
            Text(
                "暂无食物记录，可点击上方「＋ 添加」或「复制昨天」",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            meal.items.forEach { food ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${food.quantity} ×${food.servings}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(food.kcal * food.servings).roundToInt()} kcal",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                RoundStep("-", onClick = { onRemove(food.id) })
                Spacer(Modifier.width(8.dp))
                RoundStep("+", onClick = { onAdd(food.id) }, primary = true)
            }
        }
    }
}
}

@Composable
private fun RoundStep(symbol: String, onClick: () -> Unit, primary: Boolean = false) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(
                if (primary) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        )
    }
}

/** 添加食物弹窗（名称/份量/热量/三大营养素 + DeepSeek AI 智能估算） */
@Composable
private fun AddFoodDialog(
    meals: List<MealWithItems>,
    hasApiKey: Boolean,
    isAiEstimating: Boolean,
    onAiEstimate: (String, (FoodNutritionEstimate) -> Unit) -> Unit,
    onConfigureKey: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, Double, Double, Double, Double) -> Unit
) {
    var mealIdx by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1 份") }
    var kcal by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    // 用户手动改过营养值后，不再随份量自动重算（尊重手动输入）
    var manualEdit by remember { mutableStateOf(false) }

    // ===== 内置食物库自动匹配：输入名称即填充热量/碳水/蛋白质/脂肪 =====
    val matches = remember(name) { FoodData.search(name) }
    val grams = remember(qty) { parseGrams(qty) }

    fun fillFrom(info: FoodInfo) {
        val g = grams
        kcal = fmtNutrition(info.kcalPer100g * g / 100)
        carbs = fmtNutrition(info.carbsPer100g * g / 100)
        protein = fmtNutrition(info.proteinPer100g * g / 100)
        fat = fmtNutrition(info.fatPer100g * g / 100)
    }

    // 名称变化 → 自动填充（仅当用户未手动改过营养值；手动编辑后保留用户输入，不再覆盖）
    LaunchedEffect(name) {
        if (!manualEdit) {
            FoodData.search(name).firstOrNull()?.let { fillFrom(it) }
        }
    }
    LaunchedEffect(qty) {
        if (!manualEdit) FoodData.search(name).firstOrNull()?.let { fillFrom(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加食物", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("餐次", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    meals.forEachIndexed { i, m ->
                        TextButton(onClick = { mealIdx = i }) {
                            Text(
                                m.meal.mealType,
                                color = if (i == mealIdx) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("食物或菜品名称") },
                    placeholder = { Text("如：沙县蒸饺 / 拿铁 / 宫保鸡丁") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ===== DeepSeek AI 营养智能搜索与估算条 =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasApiKey) "✨ DeepSeek AI 营养估算" else "🔑 未配置 DeepSeek Key",
                        fontSize = 12.sp,
                        color = if (hasApiKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    if (hasApiKey) {
                        Button(
                            onClick = {
                                if (name.isNotBlank() && !isAiEstimating) {
                                    onAiEstimate(name) { estimate ->
                                        name = estimate.name
                                        qty = estimate.quantity
                                        kcal = fmtNutrition(estimate.kcal)
                                        carbs = fmtNutrition(estimate.carbsG)
                                        protein = fmtNutrition(estimate.proteinG)
                                        fat = fmtNutrition(estimate.fatG)
                                        manualEdit = true
                                    }
                                }
                            },
                            enabled = name.isNotBlank() && !isAiEstimating,
                            shape = RoundedCornerShape(50),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isAiEstimating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("估算中...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("AI 估算营养", fontSize = 12.sp)
                            }
                        }
                    } else {
                        TextButton(
                            onClick = onConfigureKey,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Key, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("去配置 Key", fontSize = 12.sp)
                        }
                    }
                }

                // 候选匹配列表：点击直接填入
                if (matches.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("本地匹配候选", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    matches.take(4).forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    name = m.name
                                    qty = "100g"
                                    manualEdit = false
                                    fillFrom(m)
                                }
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(m.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("100g ≈ ${m.kcalPer100g.toInt()} kcal", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("每份量（如 50g / 2 个）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = kcal, onValueChange = { kcal = it; manualEdit = true }, label = { Text("热量 kcal") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it; manualEdit = true }, label = { Text("碳水 g") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protein, onValueChange = { protein = it; manualEdit = true }, label = { Text("蛋白质 g") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = fat, onValueChange = { fat = it; manualEdit = true }, label = { Text("脂肪 g") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            // 名称为空或热量非法时禁用"添加"，避免静默无反馈
            TextButton(
                onClick = {
                    val mealId = meals.getOrNull(mealIdx)?.meal?.id ?: return@TextButton
                    if (name.isNotBlank() && kcal.toDoubleOrNull() != null) {
                        onConfirm(mealId, name, qty, kcal.toDouble(), carbs.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0)
                    }
                },
                enabled = name.isNotBlank() && kcal.toDoubleOrNull() != null
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** 从份量文案中解析克数（"50g/50克/250ml" → 数值；无单位默认按 100g 基准） */
private fun parseGrams(qty: String): Double {
    val m = Regex("""([\d.]+)\s*(?:g|克|ml|毫升)""").find(qty.trim().lowercase())
    return m?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0
}

/** 营养数值格式化：整数不带小数点 */
private fun fmtNutrition(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)

/** 健身菜谱卡片（精准控油、控盐、蛋白质与三大营养素） */
@Composable
private fun RecipeCard(
    recipe: Recipe,
    onLogClick: () -> Unit,
    onDetailClick: () -> Unit,
    onDeleteClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(24)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        recipe.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            recipe.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (recipe.isCustom) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "自建",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "耗时约 ${recipe.prepTimeMin} 分钟 · 难度: ${recipe.difficulty}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDeleteClick != null) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除自建菜谱",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 精准控制油盐与营养指标行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NutrientBadge(label = "控油", value = "${recipe.oilGram}g", highlight = true)
            NutrientBadge(label = "控盐", value = "${recipe.saltGram}g", highlight = true)
            NutrientBadge(label = "蛋白质", value = "${recipe.proteinG.roundToInt()}g", highlight = false)
            NutrientBadge(label = "碳水", value = "${recipe.carbsG.roundToInt()}g", highlight = false)
            NutrientBadge(label = "热量", value = "${recipe.kcal.roundToInt()}k", highlight = false)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDetailClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Text("食材与做法", fontSize = 13.sp)
            }
            Button(
                onClick = onLogClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1.3f).height(42.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("一键记入当餐", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 营养/油盐指标小胶囊 */
@Composable
private fun NutrientBadge(label: String, value: String, highlight: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 11.sp,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}


