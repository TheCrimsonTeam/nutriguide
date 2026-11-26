package com.thecrimsonteam.nutriguide.domain.model

import com.thecrimsonteam.nutriguide.data.local.FoodItemEntity

data class FoodDraft(
    val id: String,
    val name: String,
    val portionGrams: Double,
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val nutritionSource: String?,
    val confidence: Double?
) {
    fun toEntity(mealId: String) = FoodItemEntity(
        id = id,
        mealId = mealId,
        name = name.trim(),
        portionGrams = portionGrams,
        caloriesKcal = caloriesKcal,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        nutritionSource = nutritionSource,
        recognitionConfidence = confidence
    )
}
