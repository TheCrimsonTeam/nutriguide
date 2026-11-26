package com.thecrimsonteam.nutriguide.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val mealType: String,
    val imageUri: String? = null
)

@Entity(
    tableName = "food_items",
    foreignKeys = [ForeignKey(
        entity = MealEntity::class,
        parentColumns = ["id"],
        childColumns = ["mealId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("mealId")]
)
data class FoodItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val mealId: String,
    val name: String,
    val portionGrams: Double,
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val fiberG: Double? = null,
    val sugarG: Double? = null,
    val nutritionSource: String? = null,
    val recognitionConfidence: Double? = null
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)

@Entity(
    tableName = "workout_entries",
    foreignKeys = [ForeignKey(
        entity = WorkoutEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class WorkoutEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val exerciseId: String,
    val exerciseName: String,
    val category: String,
    val trackingType: String,
    val sortOrder: Int
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(
        entity = WorkoutEntryEntity::class,
        parentColumns = ["id"],
        childColumns = ["entryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("entryId")]
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entryId: String,
    val setNumber: Int,
    val repetitions: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null
)

@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val equipment: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val difficulty: String,
    val instructions: String,
    val trackingType: String = "STRENGTH",
    val aliases: String = ""
)
