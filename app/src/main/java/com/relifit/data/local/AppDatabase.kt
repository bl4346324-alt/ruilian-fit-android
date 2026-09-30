package com.relifit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.relifit.data.local.dao.BodyMetricDao
import com.relifit.data.local.dao.DietDao
import com.relifit.data.local.dao.ExerciseDao
import com.relifit.data.local.dao.PlanDao
import com.relifit.data.local.dao.RecipeDao
import com.relifit.data.local.dao.WorkoutDao
import com.relifit.data.local.entity.BodyMetric
import com.relifit.data.local.entity.DietGoal
import com.relifit.data.local.entity.Exercise
import com.relifit.data.local.entity.ExerciseEntry
import com.relifit.data.local.entity.FoodItem
import com.relifit.data.local.entity.Meal
import com.relifit.data.local.entity.Recipe
import com.relifit.data.local.entity.SetRecord
import com.relifit.data.local.entity.WaterRecord
import com.relifit.data.local.entity.WorkoutDay
import com.relifit.data.local.entity.WorkoutLog
import com.relifit.data.local.entity.WorkoutPlan
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        DietGoal::class,
        Recipe::class,
        WaterRecord::class
    ],
    version = 10,                        // v10: SetRecord 新增 setType 组类型；新增 WaterRecord 每日饮水记录
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun dietDao(): DietDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_records ADD COLUMN setType TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("CREATE TABLE IF NOT EXISTS `water_records` (`date` INTEGER NOT NULL, `amountMl` INTEGER NOT NULL, `goalMl` INTEGER NOT NULL, PRIMARY KEY(`date`))")
            }
        }

        /** 单例获取数据库（种子数据由 ReliFitApp 启动时写入）
         *  平滑迁移保留历史数据 */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "relifit.db"
                )
                    .addMigrations(MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
