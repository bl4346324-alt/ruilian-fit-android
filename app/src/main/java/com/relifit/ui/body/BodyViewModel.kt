package com.relifit.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.BodyMetric
import com.relifit.data.repository.BodyRepository
import com.relifit.util.TimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 身体数据 UI 状态（体重 / 身高 / 每日运动数量）
 */
data class BodyUiState(
    val metrics: List<BodyMetric> = emptyList(),
    val latest: BodyMetric? = null,
    val weightLabels: List<String> = emptyList(),
    val weightValues: List<Float> = emptyList()
)

/**
 * 身体数据 ViewModel：体重/身高/每日运动数量录入，体重历史趋势折线图
 */
class BodyViewModel(private val repo: BodyRepository) : ViewModel() {

    val uiState: StateFlow<BodyUiState> = repo.observeAll()
        .map { list ->
            val asc = list.sortedBy { it.date }
            BodyUiState(
                metrics = list,
                latest = list.firstOrNull(),
                weightLabels = asc.mapNotNull { m -> m.weightKg?.let { TimeUtils.formatShort(m.date) } },
                weightValues = asc.mapNotNull { m -> m.weightKg?.toFloat() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BodyUiState())

    /** 记录新数据（空字段沿用上一条；日期归一化到当日零点，同日记录自动覆盖） */
    fun addMetric(weightKg: Double?, heightCm: Double?, dailyActivity: Double?) {
        viewModelScope.launch {
            val prev = repo.getLatest()
            repo.insert(
                BodyMetric(
                    date = TimeUtils.startOfDay(System.currentTimeMillis()),
                    weightKg = weightKg ?: prev?.weightKg,
                    heightCm = heightCm ?: prev?.heightCm,
                    dailyActivity = dailyActivity ?: prev?.dailyActivity
                )
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                BodyViewModel(app.bodyRepository)
            }
        }
    }
}
