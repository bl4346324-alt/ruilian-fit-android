package com.relifit.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.repository.DietRepository
import com.relifit.data.repository.ExerciseRepository
import com.relifit.data.repository.WorkoutRepository
import com.relifit.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 数据统计 UI 状态
 */
data class StatsUiState(
    val period: String = "周",
    val loading: Boolean = true,
    // 顶部三个指标 + 环比
    val count: Int = 0,
    val countDelta: String = "0",
    val avgDurationMin: Double = 0.0,
    val durDelta: String = "0%",
    val volume: Double = 0.0,
    val volDelta: String = "0%",
    // 训练频率柱状图
    val freqLabels: List<String> = emptyList(),
    val freqValues: List<Float> = emptyList(),
    val freqSub: String = "",
    // 重量进步折线图
    val weightTitle: String = "杠铃深蹲 · 最大重量",
    val weightLabels: List<String> = emptyList(),
    val weightValues: List<Float> = emptyList(),
    val prIndex: Int = -1,
    val weightSub: String = "",
    // 肌群分布
    val muscles: List<Pair<String, Float>> = emptyList(),
    val muscleTexts: List<String> = emptyList(),
    val muscleSub: String = "",
    // 近 7 天热量
    val dietLabels: List<String> = emptyList(),
    val dietValues: List<Float> = emptyList(),
    val dietSub: String = ""
)

/**
 * 数据统计 ViewModel（PRD 数据统计模块核心）
 * 周/月切换：训练频率柱状图、重量进步折线图（PR 标记）、肌群分布、近 7 天热量
 */
class StatsViewModel(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    private val dietRepo: DietRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    private val _period = MutableStateFlow("周")
    val period: StateFlow<String> = _period.asStateFlow()

    init {
        viewModelScope.launch {
            _period.collect { load(it) }
        }
    }

    fun setPeriod(p: String) { _period.value = p }

    private suspend fun load(period: String) {
        _state.value = _state.value.copy(period = period, loading = true)
        val now = System.currentTimeMillis()
        val day = 24 * 3600 * 1000L

        // ===== 时间区间：当期 + 上期 =====
        val (start, end) = if (period == "周") {
            TimeUtils.startOfWeek(now) to TimeUtils.endOfWeek(now)
        } else {
            TimeUtils.startOfMonth(now) to TimeUtils.endOfMonth(now)
        }
        val span = end - start
        val prevStart = start - span - 1
        val prevEnd = start - 1

        // ===== 指标 + 环比 =====
        val count = workoutRepo.countInRange(start, end)
        val prevCount = workoutRepo.countInRange(prevStart, prevEnd)
        val duration = workoutRepo.sumDurationInRange(start, end)
        val prevDuration = workoutRepo.sumDurationInRange(prevStart, prevEnd)
        val volume = workoutRepo.sumVolumeInRange(start, end)
        val prevVolume = workoutRepo.sumVolumeInRange(prevStart, prevEnd)
        val avgDuration = if (count > 0) duration.toDouble() / count else 0.0

        fun pct(cur: Double, prev: Double): String =
            if (prev > 0) String.format("%+.0f%%", (cur - prev) / prev * 100)
            else if (cur > 0) "+100%" else "0%"

        // ===== 训练频率柱状图 =====
        val freqRows = workoutRepo.dailyFrequency(start, end)
        val freqMap = freqRows.associate { it.date to it.cnt }
        val freqLabels: List<String>
        val freqValues: List<Float>
        if (period == "周") {
            freqLabels = listOf("一", "二", "三", "四", "五", "六", "日")
            freqValues = (0..6).map { i ->
                (freqMap[TimeUtils.startOfWeek(now) + i * day] ?: 0).toFloat()
            }
        } else {
            // 按当月实际天数动态分桶（28/29/30/31 天 → 4~5 周），避免 29-31 号数据丢失
            val monthStart = TimeUtils.startOfMonth(now)
            val daysInMonth = ((TimeUtils.endOfMonth(now) - monthStart) / day + 1).toInt()
            val weeks = (daysInMonth + 6) / 7
            freqLabels = (0 until weeks).map { "第${it + 1}周" }
            freqValues = (0 until weeks).map { w ->
                val ws = monthStart + w * 7 * day
                val we = ws + 7 * day
                freqRows.count { it.date in ws until we }.toFloat()
            }
        }

        // ===== 重量进步折线图（默认深蹲，识别 PR） =====
        val prExerciseId = findExerciseId("杠铃深蹲") ?: exerciseRepo.getAll().firstOrNull()?.id
        val weightTitle = exerciseName(prExerciseId)
        val maxRows = if (prExerciseId != null) workoutRepo.maxWeightPerLog(prExerciseId, start, end) else emptyList()
        val weightValues = maxRows.map { it.w.toFloat() }
        val weightLabels = maxRows.map { TimeUtils.formatShort(it.date) }
        val prIndex = if (weightValues.isNotEmpty()) {
            val max = weightValues.maxOrNull() ?: 0f
            weightValues.indexOfLast { it >= max }
        } else -1
        val weightSub = if (weightValues.isNotEmpty()) "${weightTitle} · 最大重量 ${formatW(weightValues.maxOrNull() ?: 0f)}kg" else "完成训练后生成曲线"

        // ===== 肌群分布 =====
        val musRows = workoutRepo.muscleDistribution(start, end)
        val maxCnt = musRows.maxOfOrNull { it.cnt } ?: 1
        val muscles = musRows.map { it.muscleGroup to (it.cnt.toFloat() / maxCnt) }
        val muscleTexts = musRows.map { "${it.cnt} 次" }

        // ===== 近 7 天热量 =====
        val todayStart = TimeUtils.startOfDay(now)
        val dietRows = dietRepo.dailyKcal(todayStart - 6 * day, todayStart + day - 1)
            .associate { it.dayStart to it.kcal }
        val dietLabels = listOf("一", "二", "三", "四", "五", "六", "日")
        val dietValues = (0..6).map { i ->
            (dietRows[TimeUtils.startOfDay(todayStart - (6 - i) * day)] ?: 0.0).toFloat()
        }

        _state.value = StatsUiState(
            period = period,
            loading = false,
            count = count,
            countDelta = if (count - prevCount >= 0) "+${count - prevCount}" else "${count - prevCount}",
            avgDurationMin = avgDuration,
            durDelta = pct(duration.toDouble(), prevDuration.toDouble()),
            volume = volume,
            volDelta = pct(volume, prevVolume),
            freqLabels = freqLabels,
            freqValues = freqValues,
            freqSub = "${if (period == "周") "本周" else "本月"} · $count 次",
            weightTitle = weightTitle,
            weightLabels = weightLabels,
            weightValues = weightValues,
            prIndex = prIndex,
            weightSub = weightSub,
            muscles = muscles,
            muscleTexts = muscleTexts,
            muscleSub = "${if (period == "周") "本周" else "本月"}训练次数分布",
            dietLabels = dietLabels,
            dietValues = dietValues,
            dietSub = "近 7 天 · 每日摄入（今天高亮）"
        )
    }

    private suspend fun findExerciseId(name: String): Long? =
        exerciseRepo.getAll().firstOrNull { it.name == name }?.id

    private suspend fun exerciseName(id: Long?): String {
        if (id == null) return "重量进步"
        return exerciseRepo.getById(id)?.name ?: "重量进步"
    }

    private fun formatW(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else v.toString()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                StatsViewModel(app.workoutRepository, app.exerciseRepository, app.dietRepository)
            }
        }
    }
}
