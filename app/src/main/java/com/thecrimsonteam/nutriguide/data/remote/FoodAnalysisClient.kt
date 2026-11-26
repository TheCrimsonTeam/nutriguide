package com.thecrimsonteam.nutriguide.data.remote

import com.thecrimsonteam.nutriguide.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FoodAnalysisClient {
    fun createOrNull(): FoodAnalysisApi? {
        val configuredUrl = BuildConfig.FOOD_ANALYSIS_BASE_URL.trim()
        if (configuredUrl.isBlank()) return null
        val baseUrl = if (configuredUrl.endsWith("/")) configuredUrl else "$configuredUrl/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FoodAnalysisApi::class.java)
    }
}
