package com.thecrimsonteam.nutriguide.domain.model

import com.thecrimsonteam.nutriguide.data.local.FoodItemEntity

data class FoodNutritionCandidate(
    val fdcId: Long,
    val description: String,
    val dataType: String,
    val matchScore: Double,
    val caloriesKcalPer100g: Double?,
    val proteinGPer100g: Double?,
    val carbsGPer100g: Double?,
    val fatGPer100g: Double?,
    val fiberGPer100g: Double?,
    val sugarGPer100g: Double?,
    val source: String = "USDA FoodData Central"
)

data class FoodDraft(
    val id: String,
    val name: String,
    val portionGrams: Double,
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val nutritionSource: String?,
    val confidence: Double?,
    val fiberG: Double? = null,
    val sugarG: Double? = null,
    val alternativeNames: List<String> = emptyList(),
    val servingDescription: String? = null,
    val pieceCount: Double? = null,
    val identificationConfidence: String? = null,
    val portionUncertainty: String? = null,
    val visualObservations: List<String> = emptyList(),
    val nutritionCandidates: List<FoodNutritionCandidate> = emptyList(),
    val selectedNutritionFdcId: Long? = null
) {
    fun selectNutrition(candidate: FoodNutritionCandidate): FoodDraft {
        val factor = portionGrams.coerceAtLeast(0.0) / 100.0
        return copy(
            selectedNutritionFdcId = candidate.fdcId,
            caloriesKcal = candidate.caloriesKcalPer100g?.times(factor),
            proteinG = candidate.proteinGPer100g?.times(factor),
            carbsG = candidate.carbsGPer100g?.times(factor),
            fatG = candidate.fatGPer100g?.times(factor),
            fiberG = candidate.fiberGPer100g?.times(factor),
            sugarG = candidate.sugarGPer100g?.times(factor),
            nutritionSource = "${candidate.source} · FDC ${candidate.fdcId}"
        )
    }

    fun withPortionGrams(grams: Double): FoodDraft {
        val changed = copy(portionGrams = grams.coerceAtLeast(0.0))
        val selected = changed.nutritionCandidates.firstOrNull { it.fdcId == changed.selectedNutritionFdcId }
        return selected?.let(changed::selectNutrition) ?: changed
    }

    fun toEntity(mealId: String) = FoodItemEntity(
        id = id,
        mealId = mealId,
        name = name.trim(),
        portionGrams = portionGrams,
        caloriesKcal = caloriesKcal,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        fiberG = fiberG,
        sugarG = sugarG,
        nutritionSource = nutritionSource,
        recognitionConfidence = confidence
    )
}
