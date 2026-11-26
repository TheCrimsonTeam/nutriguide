package com.thecrimsonteam.nutriguide.data.remote

import com.thecrimsonteam.nutriguide.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object FoodAnalysisClient {
    fun createOrNull(): FoodAnalysisApi? {
        val configuredUrl = BuildConfig.FOOD_ANALYSIS_BASE_URL.trim()
        if (configuredUrl.isBlank()) return null
        val baseUrl = if (configuredUrl.endsWith("/")) configuredUrl else "$configuredUrl/"

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(55, TimeUnit.SECONDS)
            .callTimeout(70, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    // Private-beta gate only. Provider API keys remain on the server.
                    BuildConfig.FOOD_ANALYSIS_CLIENT_TOKEN
                        .takeIf { it.isNotBlank() }
                        ?.let { header("X-NutriGuide-Client-Token", it) }
                }.build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FoodAnalysisApi::class.java)
    }
}
