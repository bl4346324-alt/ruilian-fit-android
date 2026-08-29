package com.relifit

import android.app.Application
import android.content.Context
import com.relifit.data.local.AppDatabase
import com.relifit.data.local.SeedData
import com.relifit.data.repository.BodyRepository
import com.relifit.data.repository.DietRepository
import com.relifit.data.repository.ExerciseRepository
import com.relifit.data.repository.PlanRepository
import com.relifit.data.repository.SettingsRepository
import com.relifit.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口：持有全局容器（手动 DI，无 Hilt，遵循"最少第三方库"约束）
 * 启动时异步写入种子数据（动作库 + 7 套内置模板：力量/核心/有氧/恢复）
 */
class ReliFitApp : Application() {

    /** 全局协程作用域（种子数据、后台任务） */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 数据库单例 */
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    // ===== 仓库（各 ViewModel 通过工厂注入） =====
    val exerciseRepository: ExerciseRepository by lazy { ExerciseRepository(database.exerciseDao()) }
    val planRepository: PlanRepository by lazy { PlanRepository(database.planDao()) }
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepository(database.workoutDao()) }
    val bodyRepository: BodyRepository by lazy { BodyRepository(database.bodyMetricDao()) }
    val dietRepository: DietRepository by lazy { DietRepository(database.dietDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // 首次启动写入种子数据（幂等：已有数据则跳过；失败记录日志不崩溃）
        appScope.launch {
            try {
                SeedData.seedIfEmpty(database)
            } catch (e: Exception) {
                android.util.Log.e("ReliFitApp", "种子数据初始化失败", e)
            }
        }
    }

    companion object {
        /** 便捷取容器 */
        fun from(context: Context): ReliFitApp = context.applicationContext as ReliFitApp
    }
}
