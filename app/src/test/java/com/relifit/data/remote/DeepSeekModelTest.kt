package com.relifit.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekModelTest {

    @Test
    fun testFoodNutritionEstimateModel() {
        val estimate = FoodNutritionEstimate(
            name = "番茄鸡蛋面",
            quantity = "1 碗 (约400g)",
            kcal = 450.0,
            carbsG = 65.0,
            proteinG = 18.0,
            fatG = 12.0,
            note = "适量少油少盐"
        )

        assertEquals("番茄鸡蛋面", estimate.name)
        assertEquals("1 碗 (约400g)", estimate.quantity)
        assertEquals(450.0, estimate.kcal, 0.01)
        assertEquals(65.0, estimate.carbsG, 0.01)
        assertEquals(18.0, estimate.proteinG, 0.01)
        assertEquals(12.0, estimate.fatG, 0.01)
        assertEquals("适量少油少盐", estimate.note)
    }

    @Test
    fun testGeneratedRecipeModel() {
        val recipe = GeneratedRecipe(
            name = "黑椒芦笋炒牛肉粒",
            category = "高蛋白增肌",
            prepTimeMin = 15,
            difficulty = "简单",
            oilGram = 3.0,
            saltGram = 1.5,
            proteinG = 38.0,
            carbsG = 8.0,
            fatG = 5.0,
            kcal = 240.0,
            ingredients = "牛里脊 200g\n芦笋 100g\n橄榄油 3g\n食用盐 1.5g",
            instructions = "1. 热锅少油滑炒牛肉\n2. 加芦笋快炒"
        )

        assertEquals("黑椒芦笋炒牛肉粒", recipe.name)
        assertEquals("高蛋白增肌", recipe.category)
        assertEquals(15, recipe.prepTimeMin)
        assertTrue(recipe.oilGram <= 5.0)
        assertTrue(recipe.saltGram <= 2.0)
        assertTrue(recipe.proteinG >= 30.0)
    }
}
