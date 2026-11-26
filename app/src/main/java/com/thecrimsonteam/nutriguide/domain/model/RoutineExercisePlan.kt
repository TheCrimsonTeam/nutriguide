package com.thecrimsonteam.nutriguide.domain.model

data class RoutineExercisePlan(
    val exerciseId: String,
    val exerciseName: String,
    val category: String,
    val trackingType: String,
    val targetSets: Int = 3,
    val targetReps: Int = 8,
    val targetWeightKg: Double? = null,
    val restSeconds: Int = 60,
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Double? = null,
    val notes: String = ""
)
