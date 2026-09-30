package com.relifit.ui.exerciseDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.Exercise
import com.relifit.data.local.entity.SetRecord
import com.relifit.data.repository.ExerciseRepository
import com.relifit.data.repository.SettingsRepository
import com.relifit.data.repository.WorkoutRepository
import com.relifit.util.UnitConverter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 动作历史统计（最高负荷、估算极限 1RM、累计组数与近期记录）
 */
data class ExerciseStats(
    val maxWeightKg: Double = 0.0,
    val best1RM: Double = 0.0,
    val totalSets: Int = 0,
    val recentSets: List<SetRecord> = emptyList(),
    val unit: String = "kg"
)

/**
 * 动作详情 ViewModel：加载动作、收藏、历史成绩与 1RM 统计
 */
class ExerciseDetailViewModel(
    private val repo: ExerciseRepository,
    private val workoutRepo: WorkoutRepository,
    private val settingsRepo: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("id") ?: -1L

    /** 流驱动：动作数据变化（离线标记等）时详情页自动刷新；查无数据为 null */
    val exercise: StateFlow<Exercise?> =
        repo.observeById(exerciseId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stats: StateFlow<ExerciseStats> = combine(
        workoutRepo.observeSetsOfExercise(exerciseId),
        settingsRepo.unit
    ) { sets, unit ->
        val maxW = sets.maxOfOrNull { it.weightKg } ?: 0.0
        val best1RM = sets.maxOfOrNull { UnitConverter.estimate1RM(it.weightKg, it.reps) } ?: 0.0
        ExerciseStats(
            maxWeightKg = maxW,
            best1RM = best1RM,
            totalSets = sets.size,
            recentSets = sets.takeLast(8).reversed(),
            unit = unit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseStats())

    val messages = MutableSharedFlow<String>()

    fun toggleFavorite() {
        viewModelScope.launch {
            val ex = exercise.value ?: return@launch
            val newFav = !ex.isFavorite
            repo.setFavorite(ex.id, newFav)
            messages.emit(if (newFav) "已收藏「${ex.name}」" else "已取消收藏「${ex.name}」")
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                ExerciseDetailViewModel(
                    app.exerciseRepository,
                    app.workoutRepository,
                    app.settingsRepository,
                    createSavedStateHandle()
                )
            }
        }
    }
}
