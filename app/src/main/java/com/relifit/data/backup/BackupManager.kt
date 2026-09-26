package com.relifit.data.backup

import com.relifit.data.local.AppDatabase
import com.relifit.data.local.SeedData
import com.relifit.data.local.entity.BodyMetric
import com.relifit.data.local.entity.DietGoal
import com.relifit.data.local.entity.ExerciseEntry
import com.relifit.data.local.entity.FoodItem
import com.relifit.data.local.entity.Meal
import com.relifit.data.local.entity.SetRecord
import com.relifit.data.local.entity.WorkoutDay
import com.relifit.data.local.entity.WorkoutLog
import com.relifit.data.local.entity.WorkoutPlan
import com.relifit.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 纯本地数据备份与恢复管理器（JSON 格式）
 * 覆盖训练计划、训练记录与组明细、身体数据、饮食餐次与目标、动作收藏及偏好设置
 */
class BackupManager(
    private val database: AppDatabase,
    private val settingsRepo: SettingsRepository
) {

    /**
     * 导出全量本地数据为格式化 JSON 字符串
     */
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "ReliFit")
        root.put("exportedAt", System.currentTimeMillis())

        // 1. 设置偏好
        val settingsObj = JSONObject().apply {
            put("themeMode", settingsRepo.themeMode.first())
            put("unit", settingsRepo.unit.first())
            put("defaultRestSec", settingsRepo.defaultRestSec.first())
            put("activePlanId", settingsRepo.activePlanId.first() ?: -1L)
        }
        root.put("settings", settingsObj)

        // 2. 动作收藏
        val allExercises = database.exerciseDao().getAll()
        val favoritesArray = JSONArray()
        allExercises.filter { it.isFavorite }.forEach { favoritesArray.put(it.id) }
        root.put("favoriteExerciseIds", favoritesArray)

        // 3. 计划结构（Plans -> Days -> Entries）
        val plans = database.planDao().getAllPlans()
        val plansArray = JSONArray()
        for (p in plans) {
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("type", p.type)
                put("isTemplate", p.isTemplate)
                put("cycleWeeks", p.cycleWeeks)
                put("targetDurationMin", p.targetDurationMin)
                put("daysPerWeek", p.daysPerWeek)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            }
            val days = database.planDao().getDays(p.id)
            val daysArray = JSONArray()
            for (d in days) {
                val dObj = JSONObject().apply {
                    put("id", d.id)
                    put("dayIndex", d.dayIndex)
                    put("name", d.name)
                    put("defaultRestSec", d.defaultRestSec)
                }
                val entries = database.planDao().getEntries(d.id)
                val entriesArray = JSONArray()
                for (e in entries) {
                    val eObj = JSONObject().apply {
                        put("exerciseId", e.exerciseId)
                        put("sortOrder", e.sortOrder)
                        put("targetSets", e.targetSets)
                        put("targetReps", e.targetReps)
                        put("restSec", e.restSec)
                        if (e.targetWeight != null) put("targetWeight", e.targetWeight)
                    }
                    entriesArray.put(eObj)
                }
                dObj.put("entries", entriesArray)
                daysArray.put(dObj)
            }
            pObj.put("days", daysArray)
            plansArray.put(pObj)
        }
        root.put("plans", plansArray)

        // 4. 训练日志与组记录
        val logs = database.workoutDao().getAllLogs()
        val logsArray = JSONArray()
        for (log in logs) {
            val logObj = JSONObject().apply {
                put("id", log.id)
                if (log.planId != null) put("planId", log.planId)
                if (log.workoutDayId != null) put("workoutDayId", log.workoutDayId)
                put("date", log.date)
                put("durationMin", log.durationMin)
                put("totalVolumeKg", log.totalVolumeKg)
                put("totalSets", log.totalSets)
                put("note", log.note)
                put("status", log.status)
            }
            val sets = database.workoutDao().getSets(log.id)
            val setsArray = JSONArray()
            for (s in sets) {
                val sObj = JSONObject().apply {
                    put("exerciseId", s.exerciseId)
                    put("setIndex", s.setIndex)
                    put("weightKg", s.weightKg)
                    put("reps", s.reps)
                    put("completed", s.completed)
                    put("restSec", s.restSec)
                }
                setsArray.put(sObj)
            }
            logObj.put("sets", setsArray)
            logsArray.put(logObj)
        }
        root.put("workoutLogs", logsArray)

        // 5. 身体数据
        val metrics = database.bodyMetricDao().getAll()
        val metricsArray = JSONArray()
        for (m in metrics) {
            val mObj = JSONObject().apply {
                put("date", m.date)
                if (m.weightKg != null) put("weightKg", m.weightKg)
                if (m.heightCm != null) put("heightCm", m.heightCm)
                if (m.dailyActivity != null) put("dailyActivity", m.dailyActivity)
            }
            metricsArray.put(mObj)
        }
        root.put("bodyMetrics", metricsArray)

        // 6. 饮食餐次与食物
        val meals = database.dietDao().getAllMeals()
        val mealsArray = JSONArray()
        for (meal in meals) {
            val mealObj = JSONObject().apply {
                put("date", meal.date)
                put("mealType", meal.mealType)
            }
            val foods = database.dietDao().getFoodsForMeal(meal.id)
            val foodsArray = JSONArray()
            for (f in foods) {
                val fObj = JSONObject().apply {
                    put("name", f.name)
                    put("quantity", f.quantity)
                    put("kcal", f.kcal)
                    put("proteinG", f.proteinG)
                    put("fatG", f.fatG)
                    put("carbsG", f.carbsG)
                    put("servings", f.servings)
                }
                foodsArray.put(fObj)
            }
            mealObj.put("foods", foodsArray)
            mealsArray.put(mealObj)
        }
        root.put("meals", mealsArray)

        // 7. 饮食目标
        val goal = database.dietDao().getGoal()
        if (goal != null) {
            val goalObj = JSONObject().apply {
                put("dailyKcal", goal.dailyKcal)
                put("carbsG", goal.carbsG)
                put("proteinG", goal.proteinG)
                put("fatG", goal.fatG)
            }
            root.put("dietGoal", goalObj)
        }

        root.toString(2)
    }

    /**
     * 校验并导入 JSON 备份数据，覆盖恢复本地数据库
     */
    suspend fun importBackupJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val appName = root.optString("appName")
            if (appName != "ReliFit") {
                return@withContext Result.failure(IllegalArgumentException("非有效的锐炼Fit备份文件"))
            }

            // 清理并重新预置基础库
            database.clearAllTables()
            SeedData.seedIfEmpty(database)

            // 1. 恢复动作收藏
            val favoritesArray = root.optJSONArray("favoriteExerciseIds")
            if (favoritesArray != null) {
                for (i in 0 until favoritesArray.length()) {
                    val exId = favoritesArray.optLong(i)
                    database.exerciseDao().updateFavorite(exId, true)
                }
            }

            // 2. 恢复设置偏好
            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                val mode = settingsObj.optString("themeMode", "system")
                val unit = settingsObj.optString("unit", "kg")
                val rest = settingsObj.optInt("defaultRestSec", 90)
                val activePlanId = settingsObj.optLong("activePlanId", -1L)

                settingsRepo.setThemeMode(mode)
                settingsRepo.setUnit(unit)
                settingsRepo.setDefaultRestSec(rest)
                if (activePlanId > 0) settingsRepo.setActivePlanId(activePlanId)
            }

            // 3. 恢复计划结构，构建旧ID到新ID的映射
            val oldPlanToNewPlan = mutableMapOf<Long, Long>()
            val oldDayToNewDay = mutableMapOf<Long, Long>()
            val plansArray = root.optJSONArray("plans")
            var importedPlanCount = 0

            if (plansArray != null) {
                // 先获取默认已 seed 的模板计划
                val seededTemplates = database.planDao().getTemplates().associateBy { it.name }

                for (i in 0 until plansArray.length()) {
                    val pObj = plansArray.getJSONObject(i)
                    val oldPlanId = pObj.getLong("id")
                    val name = pObj.getString("name")
                    val isTemplate = pObj.optBoolean("isTemplate", false)

                    val targetPlanId: Long
                    if (isTemplate && seededTemplates.containsKey(name)) {
                        // 预置模板已存在，复用现有模板ID
                        targetPlanId = seededTemplates[name]!!.id
                    } else {
                        // 自定义计划或新计划，插入新纪录
                        targetPlanId = database.planDao().insertPlan(
                            WorkoutPlan(
                                id = 0,
                                name = name,
                                type = pObj.optString("type", "力量"),
                                isTemplate = isTemplate,
                                cycleWeeks = pObj.optInt("cycleWeeks", 4),
                                targetDurationMin = pObj.optInt("targetDurationMin", 60),
                                daysPerWeek = pObj.optInt("daysPerWeek", 3),
                                createdAt = pObj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = pObj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                        importedPlanCount++

                        // 插入对应训练日及动作
                        val daysArray = pObj.optJSONArray("days")
                        if (daysArray != null) {
                            for (j in 0 until daysArray.length()) {
                                val dObj = daysArray.getJSONObject(j)
                                val oldDayId = dObj.getLong("id")
                                val newDayId = database.planDao().insertDay(
                                    WorkoutDay(
                                        id = 0,
                                        planId = targetPlanId,
                                        dayIndex = dObj.optInt("dayIndex", j + 1),
                                        name = dObj.optString("name", "训练日"),
                                        defaultRestSec = dObj.optInt("defaultRestSec", 90)
                                    )
                                )
                                oldDayToNewDay[oldDayId] = newDayId

                                val entriesArray = dObj.optJSONArray("entries")
                                if (entriesArray != null) {
                                    for (k in 0 until entriesArray.length()) {
                                        val eObj = entriesArray.getJSONObject(k)
                                        database.planDao().insertEntry(
                                            ExerciseEntry(
                                                id = 0,
                                                workoutDayId = newDayId,
                                                exerciseId = eObj.getLong("exerciseId"),
                                                sortOrder = eObj.optInt("sortOrder", k + 1),
                                                targetSets = eObj.optInt("targetSets", 4),
                                                targetReps = eObj.optInt("targetReps", 10),
                                                restSec = eObj.optInt("restSec", 90),
                                                targetWeight = if (eObj.has("targetWeight")) eObj.optDouble("targetWeight") else null
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    oldPlanToNewPlan[oldPlanId] = targetPlanId
                }
            }

            // 4. 恢复训练日志与组记录
            val logsArray = root.optJSONArray("workoutLogs")
            var importedLogCount = 0
            if (logsArray != null) {
                for (i in 0 until logsArray.length()) {
                    val logObj = logsArray.getJSONObject(i)
                    val oldPlanId = if (logObj.has("planId")) logObj.optLong("planId") else null
                    val oldDayId = if (logObj.has("workoutDayId")) logObj.optLong("workoutDayId") else null

                    val mappedPlanId = oldPlanId?.let { oldPlanToNewPlan[it] }
                    val mappedDayId = oldDayId?.let { oldDayToNewDay[it] }

                    val sets = mutableListOf<SetRecord>()
                    val setsArray = logObj.optJSONArray("sets")
                    if (setsArray != null) {
                        for (sIdx in 0 until setsArray.length()) {
                            val sObj = setsArray.getJSONObject(sIdx)
                            sets.add(
                                SetRecord(
                                    id = 0,
                                    logId = 0,
                                    exerciseId = sObj.getLong("exerciseId"),
                                    setIndex = sObj.optInt("setIndex", sIdx + 1),
                                    weightKg = sObj.optDouble("weightKg", 0.0),
                                    reps = sObj.optInt("reps", 0),
                                    completed = sObj.optBoolean("completed", true),
                                    restSec = sObj.optInt("restSec", 0)
                                )
                            )
                        }
                    }

                    database.workoutDao().saveWorkoutTx(
                        WorkoutLog(
                            id = 0,
                            planId = mappedPlanId,
                            workoutDayId = mappedDayId,
                            date = logObj.optLong("date", System.currentTimeMillis()),
                            durationMin = logObj.optInt("durationMin", 45),
                            totalVolumeKg = logObj.optDouble("totalVolumeKg", 0.0),
                            totalSets = if (logObj.has("totalSets")) logObj.optInt("totalSets") else sets.size,
                            note = logObj.optString("note", ""),
                            status = logObj.optString("status", "完成")
                        ),
                        sets
                    )
                    importedLogCount++
                }
            }

            // 5. 恢复身体数据
            val metricsArray = root.optJSONArray("bodyMetrics")
            var importedMetricCount = 0
            if (metricsArray != null) {
                for (i in 0 until metricsArray.length()) {
                    val mObj = metricsArray.getJSONObject(i)
                    database.bodyMetricDao().insert(
                        BodyMetric(
                            id = 0,
                            date = mObj.getLong("date"),
                            weightKg = if (mObj.has("weightKg")) mObj.optDouble("weightKg") else null,
                            heightCm = if (mObj.has("heightCm")) mObj.optDouble("heightCm") else null,
                            dailyActivity = if (mObj.has("dailyActivity")) mObj.optDouble("dailyActivity") else null
                        )
                    )
                    importedMetricCount++
                }
            }

            // 6. 恢复饮食数据
            val mealsArray = root.optJSONArray("meals")
            if (mealsArray != null) {
                for (i in 0 until mealsArray.length()) {
                    val mealObj = mealsArray.getJSONObject(i)
                    val mealId = database.dietDao().insertMeal(
                        Meal(
                            id = 0,
                            date = mealObj.getLong("date"),
                            mealType = mealObj.getString("mealType")
                        )
                    )
                    val foodsArray = mealObj.optJSONArray("foods")
                    if (foodsArray != null && mealId > 0) {
                        for (j in 0 until foodsArray.length()) {
                            val fObj = foodsArray.getJSONObject(j)
                            database.dietDao().insertFood(
                                FoodItem(
                                    id = 0,
                                    mealId = mealId,
                                    name = fObj.getString("name"),
                                    quantity = fObj.optString("quantity", "1份"),
                                    kcal = fObj.optDouble("kcal", 0.0),
                                    carbsG = fObj.optDouble("carbsG", 0.0),
                                    proteinG = fObj.optDouble("proteinG", 0.0),
                                    fatG = fObj.optDouble("fatG", 0.0),
                                    servings = fObj.optInt("servings", 1)
                                )
                            )
                        }
                    }
                }
            }

            // 7. 恢复饮食目标
            val goalObj = root.optJSONObject("dietGoal")
            if (goalObj != null) {
                database.dietDao().saveGoal(
                    DietGoal(
                        id = 1,
                        dailyKcal = goalObj.optInt("dailyKcal", 2200),
                        carbsG = goalObj.optInt("carbsG", 248),
                        proteinG = goalObj.optInt("proteinG", 165),
                        fatG = goalObj.optInt("fatG", 61)
                    )
                )
            }

            Result.success("备份恢复成功：已恢复 $importedPlanCount 个计划、$importedLogCount 条训练记录、$importedMetricCount 条身体数据")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
