package com.relifit.ui.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.DietGoal
import com.relifit.data.local.entity.FoodItem
import com.relifit.data.local.entity.MealWithItems
import com.relifit.data.local.entity.Recipe
import com.relifit.data.local.entity.WaterRecord
import com.relifit.data.remote.DeepSeekService
import com.relifit.data.remote.FoodNutritionEstimate
import com.relifit.data.remote.GeneratedRecipe
import com.relifit.data.repository.DietRepository
import com.relifit.data.repository.RecipeRepository
import com.relifit.data.repository.SettingsRepository
import com.relifit.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 饮食记录 UI 状态
 */
data class DietUiState(
    val meals: List<MealWithItems> = emptyList(),
    val goal: DietGoal = DietGoal(),
    val kcal: Double = 0.0,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipeCategory: String = "全部",
    val selectedDate: Long = 0L,
    val isToday: Boolean = true,
    val isAiAvailable: Boolean = false,
    val isAiEstimating: Boolean = false,
    val isAiGeneratingRecipe: Boolean = false,
    val waterRecord: WaterRecord = WaterRecord(0, 0, 2000)
) {
    val goalKcal: Int get() = goal.dailyKcal
    val goalCarbs: Int get() = goal.carbsG
    val goalProtein: Int get() = goal.proteinG
    val goalFat: Int get() = goal.fatG
    val remainKcal: Int get() = goalKcal - kcal.roundToInt()
    val waterAmountMl: Int get() = waterRecord.amountMl
    val waterGoalMl: Int get() = waterRecord.goalMl
}

private data class DietCoreData(
    val meals: List<MealWithItems>,
    val goal: DietGoal,
    val recipes: List<Recipe>,
    val cat: String,
    val day: Long,
    val water: WaterRecord
)

/**
 * 饮食记录 ViewModel（PRD 饮食模块 + 健身控油盐菜谱系统 + DeepSeek AI 智能助手）
 * 餐次/食物 +/− 份量联动热量与三大营养素；健身菜谱标准化配方一键记入；DeepSeek AI 估算与菜谱生成；饮水记录
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DietViewModel(
    private val repo: DietRepository,
    private val recipeRepo: RecipeRepository,
    private val settingsRepo: SettingsRepository,
    private val deepSeekService: DeepSeekService
) : ViewModel() {

    /** 当前选中的日期（当日零点） */
    private val dayStart = MutableStateFlow(TimeUtils.startOfDay(System.currentTimeMillis()))
    private val recipeCategory = MutableStateFlow("全部")
    private val isAiEstimatingFlow = MutableStateFlow(false)
    private val isAiGeneratingRecipeFlow = MutableStateFlow(false)

    val messages = MutableSharedFlow<String>()

    val uiState: StateFlow<DietUiState> = combine(
        combine(
            dayStart.flatMapLatest { day ->
                combine(
                    repo.observeDay(day),
                    repo.observeGoal(),
                    repo.observeWater(day)
                ) { meals, goal, water ->
                    Triple(meals, goal ?: DietGoal(), water ?: WaterRecord(date = day, amountMl = 0, goalMl = 2000))
                }
            },
            recipeCategory.flatMapLatest { cat ->
                if (cat == "全部") recipeRepo.observeAll()
                else recipeRepo.observeByCategory(cat)
            },
            recipeCategory,
            dayStart
        ) { (meals, goal, water), recipes, cat, day ->
            DietCoreData(meals, goal, recipes, cat, day, water)
        },
        settingsRepo.isAiAvailable,
        isAiEstimatingFlow,
        isAiGeneratingRecipeFlow
    ) { core, isAiAvail, estimating, generating ->
        buildState(core.meals, core.goal, core.recipes, core.cat, core.day, core.water, isAiAvail, estimating, generating)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DietUiState(selectedDate = TimeUtils.startOfDay(System.currentTimeMillis())))

    init {
        viewModelScope.launch {
            repo.ensureTodayMeals(dayStart.value)
            while (true) {
                val now = System.currentTimeMillis()
                val nextMidnight = TimeUtils.startOfDay(now) + 24 * 3600 * 1000L
                delay((nextMidnight - now + 1000).coerceAtLeast(1000))
                val today = TimeUtils.startOfDay(System.currentTimeMillis())
                // 如果当前正好在看当天的饮食，自动切到次日新的一天
                if (dayStart.value == TimeUtils.startOfDay(now)) {
                    dayStart.value = today
                }
                repo.ensureTodayMeals(today)
            }
        }
    }

    private fun buildState(
        meals: List<MealWithItems>,
        goal: DietGoal,
        recipes: List<Recipe>,
        cat: String,
        day: Long,
        water: WaterRecord,
        isAiAvailable: Boolean,
        isAiEstimating: Boolean,
        isAiGeneratingRecipe: Boolean
    ): DietUiState {
        var kcal = 0.0; var carbs = 0.0; var protein = 0.0; var fat = 0.0
        meals.forEach { m -> m.items.forEach { f ->
            kcal += f.kcal * f.servings
            carbs += f.carbsG * f.servings
            protein += f.proteinG * f.servings
            fat += f.fatG * f.servings
        } }
        val today = TimeUtils.startOfDay(System.currentTimeMillis())
        return DietUiState(
            meals = meals, goal = goal,
            kcal = kcal, carbs = carbs, protein = protein, fat = fat,
            recipes = recipes, selectedRecipeCategory = cat,
            selectedDate = day,
            isToday = (day == today),
            isAiAvailable = isAiAvailable,
            isAiEstimating = isAiEstimating,
            isAiGeneratingRecipe = isAiGeneratingRecipe,
            waterRecord = water
        )
    }

    /** 切换到前一天 */
    fun previousDay() {
        dayStart.value -= 24 * 3600 * 1000L
        viewModelScope.launch { repo.ensureTodayMeals(dayStart.value) }
    }

    /** 切换到后一天 */
    fun nextDay() {
        dayStart.value += 24 * 3600 * 1000L
        viewModelScope.launch { repo.ensureTodayMeals(dayStart.value) }
    }

    /** 一键回到今天 */
    fun jumpToToday() {
        val today = TimeUtils.startOfDay(System.currentTimeMillis())
        if (dayStart.value != today) {
            dayStart.value = today
            viewModelScope.launch { repo.ensureTodayMeals(dayStart.value) }
        }
    }

    fun selectRecipeCategory(cat: String) {
        recipeCategory.value = cat
    }

    /** 加一份食物 */
    fun addServings(foodId: Long) {
        viewModelScope.launch { repo.changeServings(foodId, +1) }
    }

    /** 减一份食物（减到 0 自动删除） */
    fun removeServings(foodId: Long) {
        viewModelScope.launch { repo.changeServings(foodId, -1) }
    }

    /** 复制昨天同一餐次的全部食物到当前餐次 */
    fun copyYesterdayMeal(mealId: Long, mealType: String) {
        viewModelScope.launch {
            val count = repo.copyYesterdayMeal(mealId, mealType, dayStart.value)
            if (count > 0) {
                messages.emit("已复制昨天${mealType}的 $count 项食物")
            } else {
                messages.emit("昨天${mealType}没有记录食物")
            }
        }
    }

    /** 添加自定义食物 */
    fun addFood(mealId: Long, name: String, qty: String, kcal: Double, carbs: Double, protein: Double, fat: Double) {
        viewModelScope.launch {
            repo.addFood(
                mealId,
                FoodItem(
                    mealId = mealId,
                    name = name, quantity = qty,
                    kcal = kcal, carbsG = carbs, proteinG = protein, fatG = fat
                )
            )
        }
    }

    /** 一键按食谱记入餐次（带精准控油控盐与高蛋白标签） */
    fun logRecipeToMeal(mealId: Long, recipe: Recipe, servings: Int = 1) {
        viewModelScope.launch {
            val note = if (recipe.oilGram > 0 || recipe.saltGram > 0) {
                " (控油${recipe.oilGram}g·盐${recipe.saltGram}g)"
            } else " (无额外油盐)"
            repo.addFood(
                mealId,
                FoodItem(
                    mealId = mealId,
                    name = "${recipe.name}$note",
                    quantity = "1份 (食谱配方)",
                    kcal = recipe.kcal,
                    carbsG = recipe.carbsG,
                    proteinG = recipe.proteinG,
                    fatG = recipe.fatG,
                    servings = servings
                )
            )
            messages.emit("已按食谱记入「${recipe.name}」")
        }
    }

    /** 创建自建健身菜谱 */
    fun createRecipe(
        name: String, category: String, prepTimeMin: Int, difficulty: String,
        oilGram: Double, saltGram: Double, proteinG: Double, carbsG: Double, fatG: Double, kcal: Double,
        ingredients: String, instructions: String
    ) {
        viewModelScope.launch {
            recipeRepo.addRecipe(
                Recipe(
                    name = name.trim(),
                    category = category,
                    prepTimeMin = prepTimeMin,
                    difficulty = difficulty,
                    oilGram = oilGram,
                    saltGram = saltGram,
                    proteinG = proteinG,
                    carbsG = carbsG,
                    fatG = fatG,
                    kcal = kcal,
                    ingredients = ingredients.trim(),
                    instructions = instructions.trim(),
                    isCustom = true
                )
            )
            messages.emit("已创建自建菜谱：「$name」")
        }
    }

    fun deleteRecipe(id: Long) {
        viewModelScope.launch {
            recipeRepo.deleteRecipe(id)
            messages.emit("已删除该菜谱")
        }
    }

    /** 设置每日营养目标（热量 + 三大营养素克数，均可自定义） */
    fun saveGoal(kcal: Int, carbsG: Int, proteinG: Int, fatG: Int) {
        viewModelScope.launch {
            val cur = repo.getGoal()
            repo.saveGoal(
                cur.copy(
                    dailyKcal = kcal.coerceAtLeast(1),
                    carbsG = carbsG.coerceAtLeast(0),
                    proteinG = proteinG.coerceAtLeast(0),
                    fatG = fatG.coerceAtLeast(0)
                )
            )
        }
    }

    /** 保存 DeepSeek API Key */
    fun saveDeepSeekApiKey(key: String) {
        viewModelScope.launch {
            settingsRepo.setDeepSeekApiKey(key)
            messages.emit(if (key.isBlank()) "已清除 DeepSeek API Key" else "DeepSeek API Key 已保存")
        }
    }

    /** AI 智能估算食物营养数据（支持任意菜品或外食输入） */
    fun estimateFoodNutritionWithAi(
        query: String,
        onSuccess: (FoodNutritionEstimate) -> Unit
    ) {
        viewModelScope.launch {
            val key = settingsRepo.getDeepSeekApiKey()
            if (key.isBlank()) {
                messages.emit("请先配置 DeepSeek API Key")
                return@launch
            }
            if (query.isBlank()) {
                messages.emit("请输入食物名称或分量（如：一碗牛肉面）")
                return@launch
            }
            isAiEstimatingFlow.value = true
            val model = settingsRepo.getDeepSeekModel()
            val result = deepSeekService.estimateFoodNutrition(key, query, model = model)
            isAiEstimatingFlow.value = false
            result.fold(
                onSuccess = { estimate ->
                    messages.emit("已由 DeepSeek 智能估算「${estimate.name}」")
                    onSuccess(estimate)
                },
                onFailure = { error ->
                    messages.emit("AI 估算失败: ${error.message}")
                }
            )
        }
    }

    /** AI 智能生成健身控油盐菜谱（按控油控盐高蛋白标准设计） */
    fun generateRecipeWithAi(
        prompt: String,
        onSuccess: (GeneratedRecipe) -> Unit
    ) {
        viewModelScope.launch {
            val key = settingsRepo.getDeepSeekApiKey()
            if (key.isBlank()) {
                messages.emit("请先配置 DeepSeek API Key")
                return@launch
            }
            if (prompt.isBlank()) {
                messages.emit("请输入菜谱想法（如：少油高蛋白彩椒牛柳）")
                return@launch
            }
            isAiGeneratingRecipeFlow.value = true
            val model = settingsRepo.getDeepSeekModel()
            val result = deepSeekService.generateRecipe(key, prompt, model = model)
            isAiGeneratingRecipeFlow.value = false
            result.fold(
                onSuccess = { recipe ->
                    messages.emit("已由 AI 生成「${recipe.name}」菜谱")
                    onSuccess(recipe)
                },
                onFailure = { error ->
                    messages.emit("AI 生成菜谱失败: ${error.message}")
                }
            )
        }
    }

    // ===== 每日饮水操作 =====
    fun addWater(deltaMl: Int) {
        viewModelScope.launch {
            val total = repo.addWater(dayStart.value, deltaMl)
            val sign = if (deltaMl > 0) "+${deltaMl}ml" else "${deltaMl}ml"
            messages.emit("饮水记录 $sign（累计 ${total}ml）")
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            repo.resetWater(dayStart.value)
            messages.emit("已重置当日饮水量")
        }
    }

    fun setWaterGoal(goalMl: Int) {
        viewModelScope.launch {
            repo.setWaterGoal(dayStart.value, goalMl)
            messages.emit("已更新每日饮水目标为 ${goalMl}ml")
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                DietViewModel(app.dietRepository, app.recipeRepository, app.settingsRepository, app.deepSeekService)
            }
        }
    }
}
