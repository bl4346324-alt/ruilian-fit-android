package com.relifit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.SeedData
import com.relifit.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置 UI 状态
 */
data class SettingsUiState(
    val themeMode: String = "system",   // system / light / dark
    val unit: String = "kg",            // kg / lb
    val defaultRestSec: Int = 90,
    val isAiAvailable: Boolean = false,
    val isUsingCustomKey: Boolean = false,
    val isUsingBuiltInKey: Boolean = false,
    val deepseekModel: String = "deepseek-chat",
    val isTestingKey: Boolean = false
)

/**
 * 系统设置 ViewModel（PRD 系统设置模块）
 * 主题（DataStore 持久化）、单位切换、默认休息秒数、DeepSeek API Key、数据清除
 */
class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val app: ReliFitApp
) : ViewModel() {

    private val isTestingKeyFlow = kotlinx.coroutines.flow.MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepo.themeMode,
            settingsRepo.unit,
            settingsRepo.defaultRestSec,
            settingsRepo.isAiAvailable
        ) { t, u, r, avail -> Triple(Pair(t, u), r, avail) },
        settingsRepo.isUsingCustomKey,
        settingsRepo.isUsingBuiltInKey,
        settingsRepo.deepseekModel,
        isTestingKeyFlow
    ) { (tu, r, avail), customKey, builtInKey, model, testing ->
        SettingsUiState(
            themeMode = tu.first,
            unit = tu.second,
            defaultRestSec = r,
            isAiAvailable = avail,
            isUsingCustomKey = customKey,
            isUsingBuiltInKey = builtInKey,
            deepseekModel = model,
            isTestingKey = testing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    val messages = MutableSharedFlow<String>()

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setUnit(unit: String) {
        viewModelScope.launch {
            settingsRepo.setUnit(unit)
            messages.emit(if (unit == "lb") "已切换单位：磅（lb）" else "已切换单位：公斤（kg）")
        }
    }

    fun setDefaultRestSec(sec: Int) {
        viewModelScope.launch { settingsRepo.setDefaultRestSec(sec.coerceIn(10, 600)) }
    }

    fun setDeepSeekApiKey(key: String) {
        viewModelScope.launch {
            settingsRepo.setDeepSeekApiKey(key)
            messages.emit(if (key.isBlank()) "已清除自定义 Key" else "自定义 API Key 已保存")
        }
    }

    fun clearCustomDeepSeekApiKey() {
        viewModelScope.launch {
            settingsRepo.clearCustomDeepSeekApiKey()
            messages.emit("已清除自定义 Key，已恢复使用系统内置 AI 服务")
        }
    }

    fun setDeepSeekModel(model: String) {
        viewModelScope.launch {
            settingsRepo.setDeepSeekModel(model)
            messages.emit("已设置模型为：$model")
        }
    }

    fun testDeepSeekKey() {
        viewModelScope.launch {
            val key = settingsRepo.getDeepSeekApiKey()
            if (key.isBlank()) {
                messages.emit("未配置任何可用 API Key")
                return@launch
            }
            isTestingKeyFlow.value = true
            val model = settingsRepo.getDeepSeekModel()
            val res = app.deepSeekService.testApiKey(key, model = model)
            isTestingKeyFlow.value = false
            res.fold(
                onSuccess = { messages.emit("DeepSeek 验证成功！网络连接与 Key 正常") },
                onFailure = { messages.emit("DeepSeek 验证失败: ${it.message}") }
            )
        }
    }

    /** 清除全部本地数据（Room 表 + DataStore 设置）并重建种子 */
    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                app.database.clearAllTables()
                SeedData.seedIfEmpty(app.database)
            }
            settingsRepo.clear()
            messages.emit("已清除全部本地数据并恢复默认内容")
        }
    }

    /** 导出备份 JSON 字符串 */
    suspend fun exportBackup(): String {
        return app.backupManager.exportBackupJson()
    }

    /** 导出训练记录 CSV 字符串 */
    suspend fun exportWorkoutLogsCsv(): String {
        return app.backupManager.exportWorkoutLogsCsv()
    }

    /** 导出身体数据 CSV 字符串 */
    suspend fun exportBodyMetricsCsv(): String {
        return app.backupManager.exportBodyMetricsCsv()
    }

    /** 导入备份 JSON 字符串并覆盖恢复 */
    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            val res = app.backupManager.importBackupJson(jsonString)
            if (res.isSuccess) {
                messages.emit(res.getOrNull() ?: "备份恢复成功")
            } else {
                messages.emit("恢复失败: ${res.exceptionOrNull()?.message ?: "文件解析错误"}")
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = ReliFitApp.from(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]!!)
                SettingsViewModel(app.settingsRepository, app)
            }
        }
    }
}
