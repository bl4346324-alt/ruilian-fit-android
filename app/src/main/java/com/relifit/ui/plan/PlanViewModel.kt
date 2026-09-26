package com.relifit.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.DayWithEntries
import com.relifit.data.local.entity.EntryWithExercise
import com.relifit.data.local.entity.WorkoutPlan
import com.relifit.data.repository.PlanRepository
import com.relifit.data.repository.WorkoutRepository
import com.relifit.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 计划详情 UI 状态
 */
data class PlanUiState(
    val plan: WorkoutPlan? = null,
    val days: List<DayWithEntries> = emptyList(),
    val exerciseNames: Map<Long, String> = emptyMap(),
    val cycleWeek: Int = 0,
    val isActive: Boolean = false
)

/**
 * 计划详情 ViewModel（PRD 训练计划模块 P0）
 * 模板复制、训练日管理、动作条目增删改、开始训练、设为当前计划、删除计划
 */
class PlanViewModel(
    private val planRepo: PlanRepository,
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: com.relifit.data.repository.ExerciseRepository,
    private val settingsRepo: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: Long = savedStateHandle.get<Long>("planId") ?: -1L
    val messages = MutableSharedFlow<String>()

    val uiState: StateFlow<PlanUiState> = combine(
        planRepo.observePlan(planId),
        planRepo.observeDaysWithEntries(planId),
        exerciseRepo.observeAll(),
        workoutRepo.observeLogsWithSets(),
        settingsRepo.activePlanId
    ) { plan, days, exercises, logs, activeId ->
        val planLogs = logs.filter { it.log.planId == planId }
        val cycleWeek = if (planLogs.isEmpty()) 0
        else {
            val firstWeek = com.relifit.util.TimeUtils.startOfWeek(planLogs.minOf { it.log.date })
            val nowWeek = com.relifit.util.TimeUtils.startOfWeek(System.currentTimeMillis())
            val weekNo = ((nowWeek - firstWeek) / (7 * 24 * 3600 * 1000L)).toInt() + 1
            ((weekNo - 1) % (plan?.cycleWeeks ?: 4).coerceAtLeast(1)) + 1
        }
        val isActive = if (activeId != null) activeId == planId else false
        PlanUiState(
            plan = plan,
            days = days,
            exerciseNames = exercises.associate { it.id to it.name },
            cycleWeek = cycleWeek,
            isActive = isActive
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanUiState())

    /** 设为当前执行计划（首页展示该计划） */
    fun setActivePlan() {
        viewModelScope.launch {
            settingsRepo.setActivePlanId(planId)
            messages.emit("已设为当前计划，首页已同步更新")
        }
    }

    /** 删除自定义计划 */
    fun deletePlan(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val p = planRepo.getPlan(planId) ?: return@launch
            if (!p.isTemplate) {
                planRepo.deletePlan(planId)
                messages.emit("已删除计划「${p.name}」")
                onSuccess()
            }
        }
    }

    /** 复制模板生成自定义计划 */
    fun copyTemplate() {
        viewModelScope.launch {
            val plan = planRepo.getPlan(planId) ?: return@launch
            if (plan.isTemplate) {
                planRepo.copyTemplate(plan)
                messages.emit("已复制为自定义计划：「${plan.name}（副本）」")
            }
        }
    }

    /** 修改计划类型（力量/核心/有氧/恢复） */
    fun setPlanType(type: String) {
        viewModelScope.launch { planRepo.updatePlanType(planId, type) }
    }

    /** 新增训练日 */
    fun addDay(dayIndex: Int, name: String, restSec: Int) {
        viewModelScope.launch {
            planRepo.addDay(planId, dayIndex, name, restSec)
            messages.emit("已添加训练日：$name")
        }
    }

    fun deleteDay(dayId: Long) {
        viewModelScope.launch {
            planRepo.deleteDay(dayId)
            messages.emit("已删除训练日")
        }
    }

    /** 新增动作条目 */
    fun addEntry(dayId: Long, exerciseId: Long, sets: Int, reps: Int, restSec: Int) {
        viewModelScope.launch {
            planRepo.addEntry(dayId, exerciseId, sets, reps, restSec)
            messages.emit("已添加动作")
        }
    }

    fun removeEntry(entryId: Long) {
        viewModelScope.launch { planRepo.removeEntry(entryId) }
    }

    /** 修改动作条目参数（目标组数/次数/组间休息） */
    fun updateEntry(id: Long, sets: Int, reps: Int, restSec: Int, weight: Double?) {
        viewModelScope.launch { planRepo.updateEntry(id, sets, reps, restSec, weight) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                PlanViewModel(app.planRepository, app.workoutRepository, app.exerciseRepository, app.settingsRepository, createSavedStateHandle())
            }
        }
    }
}
