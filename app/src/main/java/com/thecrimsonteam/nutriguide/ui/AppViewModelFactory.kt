package com.thecrimsonteam.nutriguide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thecrimsonteam.nutriguide.data.remote.FoodAnalysisApi
import com.thecrimsonteam.nutriguide.data.repository.ExerciseRepository
import com.thecrimsonteam.nutriguide.data.repository.NutritionRepository
import com.thecrimsonteam.nutriguide.data.repository.TrainingRepository

class AppViewModelFactory(
    private val nutrition: NutritionRepository,
    private val training: TrainingRepository,
    private val exercises: ExerciseRepository,
    private val foodApi: FoodAnalysisApi?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(AppViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
        return AppViewModel(nutrition, training, exercises, foodApi) as T
    }
}
