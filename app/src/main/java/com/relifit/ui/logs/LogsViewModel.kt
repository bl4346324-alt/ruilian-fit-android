package com.relifit.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.Exercise
import com.relifit.data.local.entity.LogWithSets
import com.relifit.data.repository.ExerciseRepository
import com.relifit.data.repository.WorkoutRepository
import com.relifit.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 训练记录 UI 状态
 */
data class LogsUiState(
    val filter: String = "全部",                 // 全部 / 本月 / 上月
    val selectedDate: Long? = null,              // 按日期筛选（当日零点毫秒；null=未筛选）
    val logs: List<LogWithSets> = emptyList(),
    val exerciseNames: Map<Long, String> = emptyMap()  // exerciseId -> 名称
)

/**
 * 训练记录 ViewModel：按日期筛选（日历）+ 按月份筛选 + 历史日志（可折叠）+ 删除
 */
class LogsViewModel(
    private val workoutRepo: WorkoutRepository,
    exerciseRepo: ExerciseRepository
) : ViewModel() {

    private val filter = MutableStateFlow("全部")
    private val selectedDate = MutableStateFlow<Long?>(null)

    private val allLogs = workoutRepo.observeLogsWithSets()
    private val exNames: StateFlow<Map<Long, String>> =
        exerciseRepo.observeAll().map { list -> list.associate { it.id to it.name } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uiState: StateFlow<LogsUiState> = combine(allLogs, exNames, filter, selectedDate) { logs, names, f, date ->
        val day = 24 * 3600 * 1000L
        val filtered = if (date != null) {
            // 日历筛选：只看该日期当天的记录（优先级最高）
            logs.filter { it.log.date in date until (date + day) }
        } else {
            when (f) {
                "本月" -> logs.filter { it.log.date >= TimeUtils.startOfMonth(System.currentTimeMillis()) }
                "上月" -> {
                    val now = System.currentTimeMillis()
                    val thisMonthStart = TimeUtils.startOfMonth(now)
                    logs.filter { it.log.date >= TimeUtils.startOfMonth(thisMonthStart - 1) && it.log.date < thisMonthStart }
                }
                else -> logs
            }
        }
        LogsUiState(filter = f, selectedDate = date, logs = filtered, exerciseNames = names)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogsUiState())

    fun setFilter(f: String) { filter.value = f }

    /** 日历选择日期（当日零点）；传 null 清除日期筛选 */
    fun setSelectedDate(dayStart: Long?) { selectedDate.value = dayStart }

    fun deleteLog(id: Long) {
        viewModelScope.launch { workoutRepo.deleteLog(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                LogsViewModel(app.workoutRepository, app.exerciseRepository)
            }
        }
    }
}
