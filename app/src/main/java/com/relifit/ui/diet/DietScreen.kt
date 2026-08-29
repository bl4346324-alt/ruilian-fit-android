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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import com.relifit.ui.components.ScreenTopBar
import com.relifit.ui.components.SectionTitle
import com.relifit.ui.components.softCardShadow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 饮食记录页（Demo 布局）：今日摄入 Hero + 营养素 + 餐次食物（+/− 份量）+ 近 7 天热量
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
                        Text("今日摄入", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            Spacer(Modifier.height(18.dp))

            // ===== 今日餐次 =====
            SectionTitle("今日餐次", actionText = "＋ 添加") { showAddDialog = true }
            state.meals.forEach { meal ->
                MealCard(
                    meal = meal,
                    onAdd = { viewModel.addServings(it) },
                    onRemove = { viewModel.removeServings(it) }
                )
            }

            Spacer(Modifier.height(10.dp))

            Spacer(Modifier.height(24.dp))
        }
    }

    // ===== 添加食物弹窗 =====
    if (showAddDialog) {
        AddFoodDialog(
            meals = state.meals,
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

/** 餐次卡片（Demo meal：头部 + 食物行 +/− 份量） */
@Composable
private fun MealCard(meal: MealWithItems, onAdd: (Long) -> Unit, onRemove: (Long) -> Unit) {
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
        }
        Spacer(Modifier.height(10.dp))
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

/** 添加食物弹窗（名称/份量/热量/三大营养素） */
@Composable
private fun AddFoodDialog(
    meals: List<MealWithItems>,
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
        title = { Text("添加食物") },
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
                    label = { Text("食物名称（自动匹配营养数据）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 候选匹配列表：点击直接填入
                if (matches.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    matches.take(5).forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    name = m.name
                                    qty = "100g"
                                    manualEdit = false
                                    fillFrom(m)
                                }
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(m.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("100g ≈ ${m.kcalPer100g.toInt()} kcal", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
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
