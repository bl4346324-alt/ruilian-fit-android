package com.relifit.ui.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.DietGoal
import com.relifit.data.local.entity.FoodItem
import com.relifit.data.local.entity.MealWithItems
import com.relifit.data.repository.DietRepository
import com.relifit.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val fat: Double = 0.0
) {
    val goalKcal: Int get() = goal.dailyKcal
    val goalCarbs: Int get() = goal.carbsG
    val goalProtein: Int get() = goal.proteinG
    val goalFat: Int get() = goal.fatG
    val remainKcal: Int get() = goalKcal - kcal.roundToInt()
}

/**
 * 饮食记录 ViewModel（PRD 饮食模块）
 * 餐次/食物 +/− 份量联动热量与三大营养素；每日目标可分别自定义
 */
class DietViewModel(private val repo: DietRepository) : ViewModel() {

    private val dayStart: Long = TimeUtils.startOfDay(System.currentTimeMillis())

    val uiState: StateFlow<DietUiState> = combine(
        repo.observeDay(dayStart),
        repo.observeGoal()
    ) { meals, goal ->
        val g = goal ?: DietGoal()
        var kcal = 0.0; var carbs = 0.0; var protein = 0.0; var fat = 0.0
        meals.forEach { m -> m.items.forEach { f ->
            kcal += f.kcal * f.servings
            carbs += f.carbsG * f.servings
            protein += f.proteinG * f.servings
            fat += f.fatG * f.servings
        } }
        DietUiState(meals = meals, goal = g, kcal = kcal, carbs = carbs, protein = protein, fat = fat)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DietUiState())

    init {
        viewModelScope.launch {
            repo.ensureTodayMeals(dayStart)
        }
    }

    /** 加一份食物 */
    fun addServings(foodId: Long) {
        viewModelScope.launch { repo.changeServings(foodId, +1) }
    }

    /** 减一份食物（减到 0 自动删除） */
    fun removeServings(foodId: Long) {
        viewModelScope.launch { repo.changeServings(foodId, -1) }
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                DietViewModel(app.dietRepository)
            }
        }
    }
}
