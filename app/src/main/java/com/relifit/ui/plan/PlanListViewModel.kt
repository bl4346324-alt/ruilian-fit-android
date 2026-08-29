package com.relifit.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.WorkoutPlan
import com.relifit.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 计划列表 UI 状态（训练记录页"训练计划"入口进入）
 */
data class PlanListUiState(
    val plans: List<WorkoutPlan> = emptyList()
)

/**
 * 计划列表 ViewModel：查看全部计划（含类型分类）、新建计划、复制模板
 */
class PlanListViewModel(private val planRepo: PlanRepository) : ViewModel() {

    val uiState: StateFlow<PlanListUiState> =
        planRepo.observeAllPlans().map { PlanListUiState(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanListUiState())

    val messages = MutableSharedFlow<String>()

    /** 新建自定义计划（名称 + 类型：力量/核心/有氧/恢复） */
    fun createPlan(name: String, type: String) {
        viewModelScope.launch {
            planRepo.createPlan(name, type)
            messages.emit("已创建计划：$name（$type）")
        }
    }

    /** 复制模板生成自定义计划 */
    fun copyTemplate(template: WorkoutPlan) {
        viewModelScope.launch {
            planRepo.copyTemplate(template)
            messages.emit("已复制为自定义计划：「${template.name}」")
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                PlanListViewModel(app.planRepository)
            }
        }
    }
}
