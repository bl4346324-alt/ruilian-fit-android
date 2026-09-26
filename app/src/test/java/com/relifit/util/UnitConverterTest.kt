package com.relifit.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun testWeightTextKg() {
        assertEquals("自重", UnitConverter.weightText(0.0, "kg"))
        assertEquals("自重", UnitConverter.weightText(-5.0, "kg"))
        assertEquals("60", UnitConverter.weightText(60.0, "kg"))
        assertEquals("62.5", UnitConverter.weightText(62.5, "kg"))
    }

    @Test
    fun testWeightTextLb() {
        assertEquals("自重", UnitConverter.weightText(0.0, "lb"))
        // 100 kg * 2.20462 = 220.462 -> rounded to 220.5
        assertEquals("220.5", UnitConverter.weightText(100.0, "lb"))
    }

    @Test
    fun testStepByUnit() {
        assertEquals(2.5, UnitConverter.stepByUnit("kg"), 0.001)
        assertEquals(5.0, UnitConverter.stepByUnit("lb"), 0.001)
    }

    @Test
    fun testFormat() {
        assertEquals("10", UnitConverter.format(10.0))
        assertEquals("10.5", UnitConverter.format(10.5))
        assertEquals("10.6", UnitConverter.format(10.56))
    }
}
