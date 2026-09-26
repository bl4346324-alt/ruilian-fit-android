package com.relifit.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.WorkoutPlan
import com.relifit.data.repository.PlanRepository
import com.relifit.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 计划列表 UI 状态（训练记录页"训练计划"入口进入）
 */
data class PlanListUiState(
    val plans: List<WorkoutPlan> = emptyList(),
    val activePlanId: Long? = null
)

/**
 * 计划列表 ViewModel：查看全部计划（含类型分类）、新建计划、复制模板、设为当前计划、删除自定义计划
 */
class PlanListViewModel(
    private val planRepo: PlanRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<PlanListUiState> =
        combine(planRepo.observeAllPlans(), settingsRepo.activePlanId) { plans, activeId ->
            PlanListUiState(plans = plans, activePlanId = activeId ?: plans.firstOrNull()?.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanListUiState())

    val messages = MutableSharedFlow<String>()

    /** 新建自定义计划（名称 + 类型：力量/核心/有氧/恢复） */
    fun createPlan(name: String, type: String) {
        viewModelScope.launch {
            val newId = planRepo.createPlan(name, type)
            // 如果目前没有激活的计划，自动激活新建的计划
            settingsRepo.setActivePlanId(newId)
            messages.emit("已创建计划：$name（$type），并设为当前计划")
        }
    }

    /** 复制模板生成自定义计划 */
    fun copyTemplate(template: WorkoutPlan) {
        viewModelScope.launch {
            planRepo.copyTemplate(template)
            messages.emit("已复制为自定义计划：「${template.name}（副本）」")
        }
    }

    /** 设为当前计划 */
    fun setActivePlan(id: Long) {
        viewModelScope.launch {
            settingsRepo.setActivePlanId(id)
            messages.emit("已设为当前计划")
        }
    }

    /** 删除自定义计划 */
    fun deletePlan(id: Long) {
        viewModelScope.launch {
            val p = planRepo.getPlan(id) ?: return@launch
            if (!p.isTemplate) {
                planRepo.deletePlan(id)
                messages.emit("已删除计划「${p.name}」")
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                PlanListViewModel(app.planRepository, app.settingsRepository)
            }
        }
    }
}
