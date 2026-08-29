package com.relifit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.relifit.data.local.dao.BodyMetricDao
import com.relifit.data.local.dao.DietDao
import com.relifit.data.local.dao.ExerciseDao
import com.relifit.data.local.dao.PlanDao
import com.relifit.data.local.dao.WorkoutDao
import com.relifit.data.local.entity.BodyMetric
import com.relifit.data.local.entity.DietGoal
import com.relifit.data.local.entity.Exercise
import com.relifit.data.local.entity.ExerciseEntry
import com.relifit.data.local.entity.FoodItem
import com.relifit.data.local.entity.Meal
import com.relifit.data.local.entity.SetRecord
import com.relifit.data.local.entity.WorkoutDay
import com.relifit.data.local.entity.WorkoutLog
import com.relifit.data.local.entity.WorkoutPlan

/**
 * Room 本地数据库（唯一数据源，全部数据仅存本机）
 */
@Database(
    entities = [
        Exercise::class,
        WorkoutPlan::class,
        WorkoutDay::class,
        ExerciseEntry::class,
        WorkoutLog::class,
        SetRecord::class,
        BodyMetric::class,
        Meal::class,
        FoodItem::class,
        DietGoal::class
    ],
    version = 8,                         // v8: workout_days.planId 外键级联 + body_metrics/meals 唯一索引
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun dietDao(): DietDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** 单例获取数据库（种子数据由 ReliFitApp 启动时写入）
         *  开发阶段使用破坏性迁移（升级会清空本地数据）；正式发布前应替换为 Migration 保留用户数据 */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "relifit.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
