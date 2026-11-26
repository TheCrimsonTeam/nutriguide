package com.thecrimsonteam.nutriguide

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionTotalsTest {
    @Test
    fun calculatesTotalsAcrossFoodEntries() {
        val entries = listOf(
            FoodEntry(1, "Oats", 150, 5.0, 27.0, 3.0),
            FoodEntry(2, "Milk", 90, 6.0, 9.0, 3.5)
        )
        val totals = calculateTotals(entries)
        assertEquals(240, totals.calories)
        assertEquals(11.0, totals.protein, 0.001)
        assertEquals(36.0, totals.carbs, 0.001)
        assertEquals(6.5, totals.fat, 0.001)
    }

    @Test
    fun emptyDiaryHasZeroTotals() {
        val totals = calculateTotals(emptyList())
        assertEquals(0, totals.calories)
        assertEquals(0.0, totals.protein, 0.001)
        assertEquals(0.0, totals.carbs, 0.001)
        assertEquals(0.0, totals.fat, 0.001)
    }
}
