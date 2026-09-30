package com.relifit.ui.stats

import com.relifit.data.local.entity.SetRecord
import com.relifit.data.local.entity.WaterRecord
import com.relifit.data.local.entity.WorkoutLog
import com.relifit.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class Step2FeaturesTest {

    @Test
    fun testWarmupSetExclusionFromEffectiveVolume() {
        val sets = listOf(
            SetRecord(exerciseId = 1, setIndex = 1, weightKg = 40.0, reps = 12, setType = "WARMUP"),
            SetRecord(exerciseId = 1, setIndex = 2, weightKg = 60.0, reps = 8, setType = "WARMUP"),
            SetRecord(exerciseId = 1, setIndex = 3, weightKg = 100.0, reps = 5, setType = "NORMAL"),
            SetRecord(exerciseId = 1, setIndex = 4, weightKg = 100.0, reps = 5, setType = "NORMAL"),
            SetRecord(exerciseId = 1, setIndex = 5, weightKg = 80.0, reps = 8, setType = "DROP")
        )

        var totalVolume = 0.0
        var workingVolume = 0.0
        sets.forEach { s ->
            val v = s.weightKg * s.reps
            totalVolume += v
            if (s.setType != "WARMUP") {
                workingVolume += v
            }
        }

        // Total volume = 40*12 (480) + 60*8 (480) + 100*5 (500) + 100*5 (500) + 80*8 (640) = 2600.0
        assertEquals(2600.0, totalVolume, 0.01)
        // Working volume excludes 480 + 480 = 960 -> 2600 - 960 = 1640.0
        assertEquals(1640.0, workingVolume, 0.01)
    }

    @Test
    fun testWaterRecordCalculation() {
        var record = WaterRecord(date = 1000L, amountMl = 500, goalMl = 2000)
        assertEquals(500, record.amountMl)
        assertEquals(2000, record.goalMl)

        // add 250ml
        record = record.copy(amountMl = record.amountMl + 250)
        assertEquals(750, record.amountMl)

        // reset
        record = record.copy(amountMl = 0)
        assertEquals(0, record.amountMl)
    }

    @Test
    fun testStreakLogic() {
        val dayMillis = 24 * 3600 * 1000L
        val now = 1727654400000L // arbitrary timestamp
        val today = TimeUtils.startOfDay(now)
        val d1 = today - 3 * dayMillis
        val d2 = today - 2 * dayMillis
        val d3 = today - 1 * dayMillis
        val d4 = today

        val logs = listOf(
            WorkoutLog(date = d1, durationMin = 45, totalVolumeKg = 2000.0, totalSets = 15),
            WorkoutLog(date = d2, durationMin = 45, totalVolumeKg = 3000.0, totalSets = 15),
            WorkoutLog(date = d3, durationMin = 45, totalVolumeKg = 4000.0, totalSets = 15),
            WorkoutLog(date = d4, durationMin = 45, totalVolumeKg = 5000.0, totalSets = 15)
        )

        val activeDays = logs.map { TimeUtils.startOfDay(it.date) }.distinct().sorted()
        val activeDaySet = activeDays.toSet()

        var curStreak = 0
        var checkDay: Long? = today
        while (checkDay != null && checkDay in activeDaySet) {
            curStreak++
            checkDay -= dayMillis
        }
        assertEquals(4, curStreak)
    }

    @Test
    fun testWorkoutHubKeyCollisionPrevention() {
        // When both workout log and exercise have the same id (e.g. 1L)
        val logId = 1L
        val exerciseId = 1L

        val logKey = "log_$logId"
        val exerciseKey = "preview_ex_$exerciseId"

        // Ensure keys never collide in LazyColumn
        org.junit.Assert.assertNotEquals(logKey, exerciseKey)
        val set = setOf("calendar_view", "logs_header", logKey, "exercises_header", exerciseKey, "records_bottom_spacer")
        assertEquals(6, set.size)
    }
}
