package com.thecrimsonteam.nutriguide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MealEntity::class,
        FoodItemEntity::class,
        WorkoutEntity::class,
        WorkoutEntryEntity::class,
        WorkoutSetEntity::class,
        CustomExerciseEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class NutriGuideDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun customExerciseDao(): CustomExerciseDao
}
