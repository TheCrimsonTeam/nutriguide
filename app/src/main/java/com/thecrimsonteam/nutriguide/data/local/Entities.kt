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
    val finishedAt: Long? = null,
    val elapsedMillis: Long = 0L,
    val runningSinceElapsedRealtime: Long? = null,
    val isPaused: Boolean = false
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
    val sortOrder: Int,
    val plannedSets: Int? = null,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null,
    val restSeconds: Int? = null,
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Double? = null,
    val notes: String? = null
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


@Entity(tableName = "workout_routines")
data class WorkoutRoutineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [ForeignKey(
        entity = WorkoutRoutineEntity::class,
        parentColumns = ["id"],
        childColumns = ["routineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routineId")]
)
data class RoutineExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val exerciseId: String,
    val exerciseName: String,
    val category: String,
    val trackingType: String,
    val sortOrder: Int,
    val targetSets: Int = 3,
    val targetReps: Int = 8,
    val targetWeightKg: Double? = null,
    val restSeconds: Int = 60,
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Double? = null,
    val notes: String = ""
)

@Entity(
    tableName = "scheduled_workouts",
    foreignKeys = [ForeignKey(
        entity = WorkoutRoutineEntity::class,
        parentColumns = ["id"],
        childColumns = ["routineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routineId"), Index("scheduledDay")]
)
data class ScheduledWorkoutEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    /** LocalDate.toEpochDay(), stored as a stable date-only value. */
    val scheduledDay: Long,
    val status: String = "PLANNED",
    val completedWorkoutId: String? = null,
    val notes: String = ""
)
