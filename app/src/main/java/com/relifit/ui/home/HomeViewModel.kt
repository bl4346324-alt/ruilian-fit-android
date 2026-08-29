package com.relifit.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.BodyMetric
import com.relifit.data.local.entity.DayWithEntries
import com.relifit.data.local.entity.WorkoutPlan
import com.relifit.data.repository.BodyRepository
import com.relifit.data.repository.PlanRepository
import com.relifit.data.repository.SettingsRepository
import com.relifit.data.repository.WorkoutRepository
import com.relifit.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

/**
 * 首页 UI 状态
 */
data class HomeUiState(
    val plan: WorkoutPlan? = null,
    val days: List<DayWithEntries> = emptyList(),
    val todayDay: DayWithEntries? = null,
    val todayEntries: Int = 0,
    val cycleWeek: Int = 1,               // 第 X 周
    val cycleWeeks: Int = 4,              // 共 Y 周
    val weekTrained: Int = 0,             // 本周已练次数
    val weekTotal: Int = 3,               // 本周目标次数
    val body: BodyMetric? = null,
    val bodyChanges: List<String> = emptyList(),  // 体重/身高/每日运动变化
    val bodyLabels: List<String> = emptyList(),
    val bodyValues: List<String> = emptyList()
)

/**
 * 首页 ViewModel：今日训练 + 进行中计划 + 本周统计 + 身体数据（Demo 首页四区块）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val planRepo: PlanRepository,
    private val workoutRepo: WorkoutRepository,
    private val bodyRepo: BodyRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = planRepo.observeAllPlans()
        .flatMapLatest { plans ->
            val plan = plans.firstOrNull()
            if (plan == null) flowOf(HomeUiState())
            else planRepo.observeDaysWithEntries(plan.id).flatMapLatest { days ->
                combine(
                    workoutRepo.observeLogsWithSets(),
                    bodyRepo.observeAll()
                ) { logs, bodies -> build(plan, days, logs.map { it.log }, bodies) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun build(
        plan: WorkoutPlan,
        days: List<DayWithEntries>,
        logs: List<com.relifit.data.local.entity.WorkoutLog>,
        bodies: List<BodyMetric>
    ): HomeUiState {
        val todayIdx = todayDayIndex()
        val todayDay = days.firstOrNull { it.day.dayIndex == todayIdx }

        // 周进度：按自然周统计训练次数
        val weekStart = TimeUtils.startOfWeek(System.currentTimeMillis())
        val weekEnd = weekStart + 7 * 24 * 3600 * 1000L
        val weekLogs = logs.filter { it.date in weekStart..weekEnd }

        // 计划周期进度：出现训练记录的不同周数（1..cycleWeeks）
        val trainedWeeks = logs.map { TimeUtils.startOfWeek(it.date) }.distinct().size
        val cycleWeek = (trainedWeeks + 1).coerceIn(1, plan.cycleWeeks)

        val weekCount = weekLogs.size

        // 身体数据：最新 + 与上一条的差值
        val latest = bodies.firstOrNull()
        val prev = bodies.getOrNull(1)
        fun diff(cur: Double?, old: Double?): String =
            if (cur != null && old != null) String.format("%+.1f", cur - old) else ""
        val changes = listOf(
            diff(latest?.weightKg, prev?.weightKg),
            diff(latest?.heightCm, prev?.heightCm),
            diff(latest?.dailyActivity, prev?.dailyActivity)
        )

        return HomeUiState(
            plan = plan,
            days = days,
            todayDay = todayDay,
            todayEntries = todayDay?.entries?.size ?: 0,
            cycleWeek = cycleWeek,
            cycleWeeks = plan.cycleWeeks,
            weekTrained = weekCount,
            weekTotal = plan.daysPerWeek,
            body = latest,
            bodyChanges = changes,
            bodyLabels = listOf("体重", "身高", "每日运动"),
            bodyValues = listOf(
                latest?.weightKg?.let { String.format("%.1f", it) } ?: "--",
                latest?.heightCm?.let { String.format("%.1f", it) } ?: "--",
                latest?.dailyActivity?.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.1f", it) } ?: "--"
            )
        )
    }

    /** 今天在周内的序号（周一=1 ... 周日=7），与 dayIndex 对齐 */
    private fun todayDayIndex(): Int {
        val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return (dow + 5) % 7 + 1
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                HomeViewModel(app.planRepository, app.workoutRepository, app.bodyRepository)
            }
        }
    }
}
