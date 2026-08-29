package com.relifit.ui.exerciseDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.Exercise
import com.relifit.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 动作详情 ViewModel：加载动作、收藏
 */
class ExerciseDetailViewModel(
    private val repo: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("id") ?: -1L

    /** 流驱动：动作数据变化（离线标记等）时详情页自动刷新；查无数据为 null（页面显示空态） */
    val exercise: StateFlow<Exercise?> =
        repo.observeById(exerciseId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messages = MutableSharedFlow<String>()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                ExerciseDetailViewModel(app.exerciseRepository, createSavedStateHandle())
            }
        }
    }
}
