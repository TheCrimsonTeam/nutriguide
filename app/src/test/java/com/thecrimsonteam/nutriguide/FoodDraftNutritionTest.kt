package com.thecrimsonteam.nutriguide

import com.thecrimsonteam.nutriguide.domain.model.FoodDraft
import com.thecrimsonteam.nutriguide.domain.model.FoodNutritionCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodDraftNutritionTest {
    private fun candidate(
        calories: Double? = 130.0,
        protein: Double? = 2.7,
        fiber: Double? = null
    ) = FoodNutritionCandidate(
        fdcId = 12345L,
        description = "Cooked white rice",
        dataType = "SR Legacy",
        matchScore = 0.98,
        caloriesKcalPer100g = calories,
        proteinGPer100g = protein,
        carbsGPer100g = 28.0,
        fatGPer100g = 0.3,
        fiberGPer100g = fiber,
        sugarGPer100g = null
    )

    private fun draft(food: FoodNutritionCandidate = candidate()) = FoodDraft(
        id = "draft-1",
        name = "Rice",
        portionGrams = 150.0,
        caloriesKcal = null,
        proteinG = null,
        carbsG = null,
        fatG = null,
        nutritionSource = null,
        confidence = null,
        nutritionCandidates = listOf(food)
    )

    @Test
    fun selectingCandidateScalesPerHundredGramValuesToPortion() {
        val selected = draft().selectNutrition(candidate())

        assertEquals(195.0, selected.caloriesKcal!!, 0.001)
        assertEquals(4.05, selected.proteinG!!, 0.001)
        assertEquals(42.0, selected.carbsG!!, 0.001)
        assertEquals(12345L, selected.selectedNutritionFdcId)
        assertEquals(true, selected.nutritionSource!!.contains("USDA FoodData Central"))
    }

    @Test
    fun changingPortionRecalculatesSelectedDatabaseValues() {
        val revised = draft().selectNutrition(candidate()).withPortionGrams(225.0)

        assertEquals(292.5, revised.caloriesKcal!!, 0.001)
        assertEquals(6.075, revised.proteinG!!, 0.001)
    }

    @Test
    fun nutrientsMissingFromDatabaseRemainUnknown() {
        val revised = draft(candidate(calories = null, protein = 4.0, fiber = null))
            .selectNutrition(candidate(calories = null, protein = 4.0, fiber = null))

        assertNull(revised.caloriesKcal)
        assertNull(revised.fiberG)
        assertNull(revised.sugarG)
    }
}
