package com.relifit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relifit.ReliFitApp
import com.relifit.data.local.SeedData
import com.relifit.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置 UI 状态
 */
data class SettingsUiState(
    val themeMode: String = "system",   // system / light / dark
    val unit: String = "kg",            // kg / lb
    val defaultRestSec: Int = 90
)

/**
 * 系统设置 ViewModel（PRD 系统设置模块）
 * 主题（DataStore 持久化）、单位切换、默认休息秒数、数据清除
 */
class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val app: ReliFitApp
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepo.themeMode,
        settingsRepo.unit,
        settingsRepo.defaultRestSec
    ) { t, u, r -> SettingsUiState(t, u, r) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

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

    /** 清除全部本地数据并重建种子 */
    fun clearAllData() {
        viewModelScope.launch {
            app.database.clearAllTables()
            SeedData.seedIfEmpty(app.database)
            messages.emit("已清除全部数据并恢复默认内容")
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
