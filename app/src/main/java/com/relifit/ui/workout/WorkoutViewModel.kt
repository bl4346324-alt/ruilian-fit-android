package com.relifit.ui.workout

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.entity.Exercise
import com.relifit.data.local.entity.SetRecord
import com.relifit.data.local.entity.WorkoutLog
import com.relifit.data.repository.ExerciseRepository
import com.relifit.data.repository.PlanRepository
import com.relifit.data.repository.SettingsRepository
import com.relifit.data.repository.WorkoutRepository
import com.relifit.util.TimeUtils
import com.relifit.util.UnitConverter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** 会话中的单个动作（含已记录组） */
data class SessionExercise(
    val exerciseId: Long,
    val name: String,
    val targetSets: Int,
    val targetReps: Int,
    val restSec: Int,
    val weightKg: Double,                       // 预填重量（目标重量或上次记录）
    val sets: List<RecordedSet> = emptyList()
)

/** 已记录的一组 */
data class RecordedSet(val weightKg: Double, val reps: Int)

/** 训练进行页 UI 状态 */
data class WorkoutUiState(
    val exercises: List<SessionExercise> = emptyList(),
    val currentIndex: Int = 0,
    val restActive: Boolean = false,
    val restTotal: Int = 0,
    val finished: Boolean = false,
    val loading: Boolean = true,
    val unit: String = "kg",
    val inputWeightKg: Double = 0.0,    // 当前输入重量
    val inputReps: Int = 10             // 当前输入次数
) {
    val current: SessionExercise? get() = exercises.getOrNull(currentIndex)
    val doneCount: Int get() = exercises.sumOf { it.sets.size }
}

/**
 * 训练进行页 ViewModel【核心 P0】
 * 职责：会话状态机（动作/组推进）、总计时器、组间休息倒计时（+30s/跳过/震动）、
 *       重量次数步进、智能预填、保存 WorkoutLog + SetRecord 到 Room
 */
class WorkoutViewModel(
    application: Application,
    private val planRepo: PlanRepository,
    private val exerciseRepo: ExerciseRepository,
    private val workoutRepo: WorkoutRepository,
    private val settingsRepo: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(WorkoutUiState())
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()

    /**
     * 高频跳动状态与页面状态分离（性能优化）：
     * 总计时与休息倒计时每秒变化，若放进 UiState 会导致整页每秒重组。
     * 拆成独立 StateFlow，仅数字组件订阅，避免训练页整体卡顿。
     */
    private val _clock = MutableStateFlow(0)
    val clock: StateFlow<Int> = _clock.asStateFlow()

    private val _restLeft = MutableStateFlow(0)
    val restLeft: StateFlow<Int> = _restLeft.asStateFlow()

    /** Snackbar 提示 */
    val messages = MutableSharedFlow<String>()
    /** 训练保存完成事件（通知页面返回） */
    val onSaved = MutableSharedFlow<Unit>()

    /** 动作库全量（训练中添加动作选择器） */
    val allExercises: StateFlow<List<Exercise>> =
        exerciseRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var totalTimer: Job? = null
    private var restTimer: Job? = null

    /** 来源计划/训练日（自由训练为 null，保存日志时关联） */
    private val sessionPlanId: Long? = savedStateHandle.get<Long>("planId")?.takeIf { it > 0 }
    private val sessionDayId: Long? = savedStateHandle.get<Long>("dayId")?.takeIf { it > 0 }

    init {
        // 单位流
        viewModelScope.launch {
            settingsRepo.unit.collect { unit -> _state.value = _state.value.copy(unit = unit) }
        }
        // 加载会话：按计划训练日 / 单动作自由训练 / 空自由训练
        val exerciseId = savedStateHandle.get<Long>("exerciseId")?.takeIf { it > 0 }
        viewModelScope.launch {
            val session = if (sessionDayId != null) loadFromDay(sessionDayId)
            else if (exerciseId != null) listOfNotNull(buildSingle(exerciseId))
            else emptyList()
            _state.value = _state.value.copy(exercises = session, loading = false)
            resetInputForCurrent()
            startTotalTimer()
        }
    }

    // ==================== 会话加载 ====================

    /** 按计划训练日加载动作（目标组数/次数/休息 + 智能预填上次重量） */
    private suspend fun loadFromDay(dayId: Long): List<SessionExercise> {
        val entries = planRepo.observeEntriesWithExercise(dayId).first()
        val list = mutableListOf<SessionExercise>()
        entries.forEach { e ->
            val ex = e.exercise ?: return@forEach
            val lastWeight = lastRecordedWeight(ex.id)
            list.add(
                SessionExercise(
                    exerciseId = ex.id, name = ex.name,
                    targetSets = e.entry.targetSets, targetReps = e.entry.targetReps,
                    restSec = e.entry.restSec,
                    weightKg = lastWeight ?: e.entry.targetWeight ?: 0.0
                )
            )
        }
        return list
    }

    private suspend fun buildSingle(exerciseId: Long): SessionExercise? {
        val ex = exerciseRepo.getById(exerciseId) ?: return null
        return SessionExercise(
            exerciseId = ex.id, name = ex.name,
            targetSets = 3, targetReps = 10, restSec = 90,
            weightKg = lastRecordedWeight(ex.id) ?: 0.0
        )
    }

    /** 智能预填：该动作最近一次记录重量 */
    private suspend fun lastRecordedWeight(exerciseId: Long): Double? {
        return workoutRepo.observeSetsOfExercise(exerciseId).first().lastOrNull()?.weightKg
    }

    // ==================== 计时器 ====================

    /** 训练总计时（Demo 顶部时钟 chip）；独立 StateFlow，不触发整页重组 */
    private fun startTotalTimer() {
        totalTimer = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _clock.value = _clock.value + 1
            }
        }
    }

    private fun stopTotalTimer() { totalTimer?.cancel(); totalTimer = null }

    /** 组间休息倒计时：结束震动 + 提示；倒计时数字走独立 StateFlow
     *  剩余秒数以 _restLeft 为唯一数据源（restPlus30 修改它即可生效） */
    private fun startRest(sec: Int) {
        restTimer?.cancel()
        _restLeft.value = sec
        _state.value = _state.value.copy(restActive = true, restTotal = sec)
        restTimer = viewModelScope.launch {
            while (_restLeft.value > 0 && isActive) {
                delay(1000)
                _restLeft.value = _restLeft.value - 1
            }
            if (_restLeft.value <= 0) finishRest()
        }
    }

    /** 跳过休息 / 倒计时结束 */
    fun skipRest() { finishRest() }

    private fun finishRest() {
        restTimer?.cancel()
        if (_state.value.restActive) {
            _state.value = _state.value.copy(restActive = false)
            vibrate()
            viewModelScope.launch { messages.emit("休息结束，开始下一组") }
        }
    }

    /** +30 秒（直接修改唯一数据源 _restLeft，倒计时协程自动延续） */
    fun restPlus30() {
        if (!_state.value.restActive) return
        val left = (_restLeft.value + 30).coerceAtMost(600)
        _restLeft.value = left
        _state.value = _state.value.copy(restTotal = left)
    }

    private fun vibrate() {
        val app = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    // ==================== 输入与组记录 ====================

    private fun resetInputForCurrent() {
        val cur = _state.value.current
        _state.value = _state.value.copy(
            inputWeightKg = cur?.weightKg ?: 0.0,
            inputReps = cur?.targetReps ?: 10
        )
    }

    /** 重量步进（kg 步进 2.5 / lb 步进 5 磅，换算回 kg 累加，PRD 0.5kg 精度） */
    fun changeWeight(deltaSign: Int) {
        val stepKg = UnitConverter.stepKgByUnit(_state.value.unit)
        val new = (_state.value.inputWeightKg + deltaSign * stepKg).coerceAtLeast(0.0)
        _state.value = _state.value.copy(inputWeightKg = Math.round(new * 10) / 10.0)
    }

    fun changeReps(deltaSign: Int) {
        _state.value = _state.value.copy(inputReps = (_state.value.inputReps + deltaSign).coerceAtLeast(1))
    }

    /** 完成本组：记录并推进；全部完成进入结束状态 */
    fun completeSet() {
        val s = _state.value
        val cur = s.current ?: return
        if (s.restActive) return   // 休息中不可记录

        val newSets = cur.sets + RecordedSet(s.inputWeightKg, s.inputReps)
        val updatedExercises = s.exercises.toMutableList().apply {
            this[s.currentIndex] = cur.copy(sets = newSets)
        }
        var nextIndex = s.currentIndex
        var finished = false
        if (newSets.size >= cur.targetSets) {
            nextIndex = s.currentIndex + 1
            if (nextIndex >= updatedExercises.size) finished = true
        }
        _state.value = s.copy(exercises = updatedExercises, currentIndex = nextIndex, finished = finished)

        if (finished) {
            stopTotalTimer()
            viewModelScope.launch { messages.emit("全部动作完成，点击结束训练保存记录") }
        } else {
            // 进入组间休息（Demo：完成一组自动弹窗倒计时）
            startRest(cur.restSec)
            resetInputForCurrent()
        }
    }

    /** 训练中临时添加动作（PRD：可添加多个训练动作） */
    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            val ex = exerciseRepo.getById(exerciseId) ?: return@launch
            val lastWeight = lastRecordedWeight(ex.id)
            val sessionEx = SessionExercise(
                exerciseId = ex.id, name = ex.name,
                targetSets = 3, targetReps = 10, restSec = 90,
                weightKg = lastWeight ?: 0.0
            )
            _state.value = _state.value.copy(exercises = _state.value.exercises + sessionEx)
            viewModelScope.launch { messages.emit("已添加动作：「${ex.name}」") }
        }
    }

    // ==================== 结束与保存 ====================

    /** 保存训练到 Room：WorkoutLog + 全部 SetRecord（PRD 核心） */
    fun finishAndSave() {
        val s = _state.value
        if (s.doneCount == 0) {
            viewModelScope.launch { messages.emit("还没有记录任何一组") }
            return
        }
        viewModelScope.launch {
            val durationMin = ceil(_clock.value / 60.0).toInt().coerceAtLeast(1)
            val sets = mutableListOf<SetRecord>()
            var volume = 0.0
            s.exercises.forEach { ex ->
                ex.sets.forEachIndexed { idx, rs ->
                    volume += rs.weightKg * rs.reps
                    sets.add(
                        SetRecord(
                            exerciseId = ex.exerciseId, setIndex = idx + 1,
                            weightKg = rs.weightKg, reps = rs.reps, completed = true
                        )
                    )
                }
            }
            workoutRepo.saveWorkout(
                log = WorkoutLog(
                    // 日期归一化到当日零点：与统计页按天分组的 key 对齐（修复频率图恒为 0）
                    date = TimeUtils.startOfDay(System.currentTimeMillis()),
                    planId = sessionPlanId,
                    workoutDayId = sessionDayId,
                    durationMin = durationMin,
                    totalVolumeKg = Math.round(volume * 10) / 10.0,
                    totalSets = sets.size,
                    status = "完成"
                ),
                sets = sets
            )
            stopTotalTimer()
            val unit = s.unit
            val volText = if (unit == "lb") {
                "${TimeUtils.thousands(volume * 2.20462)} lb"
            } else {
                "${TimeUtils.thousands(volume)} kg"
            }
            messages.emit("训练完成！已生成训练日志（${durationMin} 分钟 · $volText）")
            onSaved.emit(Unit)
        }
    }

    /** 中途退出（不保存） */
    fun discard() {
        stopTotalTimer(); restTimer?.cancel()
        viewModelScope.launch { messages.emit("已退出训练，本次记录未保存") }
    }

    override fun onCleared() {
        stopTotalTimer(); restTimer?.cancel()
        super.onCleared()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                WorkoutViewModel(
                    application = app,
                    planRepo = app.planRepository,
                    exerciseRepo = app.exerciseRepository,
                    workoutRepo = app.workoutRepository,
                    settingsRepo = app.settingsRepository,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
