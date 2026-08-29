package com.relifit.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.Exercise
import com.relifit.data.repository.ExerciseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 动作库 UI 状态
 */
data class LibraryUiState(
    val group: String = "胸",          // 当前肌群（Demo 默认选中"胸"）
    val query: String = "",
    val exercises: List<Exercise> = emptyList(),
    val offlineCount: Int = 0
)

/**
 * 动作库 ViewModel：肌群筛选 + 搜索 + 离线标记（Demo 云朵图标交互）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(private val repo: ExerciseRepository) : ViewModel() {

    private val group = MutableStateFlow("胸")
    private val query = MutableStateFlow("")
    private val offlineCount = repo.observeAll()
        .map { list -> list.count { it.offlineAvailable } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 单次消息事件（Snackbar 提示） */
    val messages = MutableSharedFlow<String>()

    val uiState: StateFlow<LibraryUiState> =
        combine(
            combine(group, query) { g, q -> g to q }
                .flatMapLatest { (g, q) ->
                    if (q.isBlank()) repo.observeByGroup(g) else repo.search(q)
                },
            offlineCount,
            group
        ) { list, off, g ->
            LibraryUiState(group = g, query = query.value, exercises = list, offlineCount = off)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun selectGroup(g: String) { group.value = g }

    fun onQueryChange(q: String) { query.value = q }

    /** 离线包状态提示（Demo 顶栏云朵图标） */
    fun showOfflineInfo() {
        viewModelScope.launch { messages.emit("离线包 · 已下载 ${offlineCount.value} 个动作") }
    }

    /** 离线下载/取消（纯本地标记，无网络） */
    fun toggleOffline(id: Long) {
        viewModelScope.launch {
            val ex = repo.getById(id) ?: return@launch
            val target = !ex.offlineAvailable
            repo.setOffline(id, target)
            messages.emit(if (target) "已离线下载：「${ex.name}」" else "已取消离线：「${ex.name}」")
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                LibraryViewModel(app.exerciseRepository)
            }
        }
    }
}
