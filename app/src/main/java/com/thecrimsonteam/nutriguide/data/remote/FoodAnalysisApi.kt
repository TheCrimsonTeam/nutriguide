package com.thecrimsonteam.nutriguide.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class FoodAnalysisResponse(
    val items: List<FoodAnalysisItemDto> = emptyList(),
    val message: String? = null,
    val requiresUserConfirmation: Boolean = true
)

data class FoodAnalysisItemDto(
    val name: String = "",
    val portionGrams: Double? = null,
    val confidence: Double? = null,
    val caloriesKcal: Double? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val fiberG: Double? = null,
    val sugarG: Double? = null,
    val nutritionSource: String? = null,
    val alternatives: List<String> = emptyList(),
    val servingDescription: String? = null,
    val pieceCount: Double? = null,
    val confidenceLabel: String? = null,
    val portionUncertainty: String? = null,
    val visualObservations: List<String> = emptyList(),
    val nutritionCandidates: List<NutritionCandidateDto> = emptyList()
)

data class NutritionCandidateDto(
    val fdcId: Long = 0L,
    val description: String = "",
    val dataType: String = "",
    val matchScore: Double = 0.0,
    val caloriesKcalPer100g: Double? = null,
    val proteinGPer100g: Double? = null,
    val carbsGPer100g: Double? = null,
    val fatGPer100g: Double? = null,
    val fiberGPer100g: Double? = null,
    val sugarGPer100g: Double? = null,
    val source: String = "USDA FoodData Central"
)

interface FoodAnalysisApi {
    @Multipart
    @POST("food-analysis")
    suspend fun analyze(@Part photo: MultipartBody.Part): FoodAnalysisResponse
}
