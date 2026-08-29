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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 动作详情 ViewModel：加载动作、离线下载切换、收藏
 */
class ExerciseDetailViewModel(
    private val repo: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("id") ?: -1L
    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise.asStateFlow()

    val messages = MutableSharedFlow<String>()

    init {
        viewModelScope.launch { _exercise.value = repo.getById(exerciseId) }
    }

    /** 离线下载/取消（Demo 底部按钮） */
    fun toggleOffline() {
        viewModelScope.launch {
            val ex = _exercise.value ?: return@launch
            repo.setOffline(ex.id, !ex.offlineAvailable)
            _exercise.value = ex.copy(offlineAvailable = !ex.offlineAvailable)
            messages.emit(if (_exercise.value!!.offlineAvailable) "已离线下载，无网可用" else "已取消离线下载")
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                ExerciseDetailViewModel(app.exerciseRepository, createSavedStateHandle())
            }
        }
    }
}
