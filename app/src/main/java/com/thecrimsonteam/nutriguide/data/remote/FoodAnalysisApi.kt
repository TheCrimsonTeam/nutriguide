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
    val name: String,
    val portionGrams: Double?,
    val confidence: Double?,
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val nutritionSource: String?
)

interface FoodAnalysisApi {
    @Multipart
    @POST("food-analysis")
    suspend fun analyze(@Part photo: MultipartBody.Part): FoodAnalysisResponse
}
