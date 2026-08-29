package com.relifit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore 实例（单例） */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "relifit_settings")

/**
 * 设置仓库（DataStore 持久化，PRD 系统设置模块）
 * 主题模式：system / light / dark；单位：kg / lb；默认组间休息秒数
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")          // system/light/dark
        val UNIT = stringPreferencesKey("unit")                      // kg/lb
        val DEFAULT_REST_SEC = intPreferencesKey("default_rest_sec")
    }

    /** 主题模式流 */
    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }

    /** 单位流 */
    val unit: Flow<String> = context.dataStore.data.map { it[Keys.UNIT] ?: "kg" }

    /** 默认休息秒数流 */
    val defaultRestSec: Flow<Int> = context.dataStore.data.map { it[Keys.DEFAULT_REST_SEC] ?: 90 }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setUnit(unit: String) {
        context.dataStore.edit { it[Keys.UNIT] = unit }
    }

    suspend fun setDefaultRestSec(sec: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_REST_SEC] = sec }
    }

    /** 清除全部设置（主题/单位/默认休息秒数恢复默认值） */
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
