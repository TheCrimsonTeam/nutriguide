package com.thecrimsonteam.nutriguide.data.repository

import androidx.room.withTransaction
import com.thecrimsonteam.nutriguide.data.local.CustomExerciseDao
import com.thecrimsonteam.nutriguide.data.local.FoodItemEntity
import com.thecrimsonteam.nutriguide.data.local.MealDao
import com.thecrimsonteam.nutriguide.data.local.MealEntity
import com.thecrimsonteam.nutriguide.data.local.CustomExerciseEntity
import com.thecrimsonteam.nutriguide.data.local.NutriGuideDatabase
import com.thecrimsonteam.nutriguide.data.local.WorkoutDao
import com.thecrimsonteam.nutriguide.data.local.WorkoutEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutEntryEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

interface NutritionRepository {
    fun meals(): Flow<List<MealEntity>>
    suspend fun saveMeal(meal: MealEntity, items: List<FoodItemEntity>)
    suspend fun items(mealId: String): List<FoodItemEntity>
    suspend fun deleteMeal(mealId: String)
}

class LocalNutritionRepository(
    private val database: NutriGuideDatabase,
    private val dao: MealDao
) : NutritionRepository {
    override fun meals() = dao.observeMeals()
    override suspend fun saveMeal(meal: MealEntity, items: List<FoodItemEntity>) {
        database.withTransaction {
            dao.insertMeal(meal)
            if (items.isNotEmpty()) dao.insertItems(items)
        }
    }
    override suspend fun items(mealId: String) = dao.getItems(mealId)
    override suspend fun deleteMeal(mealId: String) = dao.deleteMeal(mealId)
}

interface TrainingRepository {
    fun workouts(): Flow<List<WorkoutEntity>>
    fun entries(workoutId: String): Flow<List<WorkoutEntryEntity>>
    fun sets(entryId: String): Flow<List<WorkoutSetEntity>>
    suspend fun createWorkout(workout: WorkoutEntity)
    suspend fun addExercise(entry: WorkoutEntryEntity)
    suspend fun addSet(set: WorkoutSetEntity)
    suspend fun updateEntry(entry: WorkoutEntryEntity)
    suspend fun updateSet(set: WorkoutSetEntity)
    suspend fun deleteEntry(id: String)
    suspend fun deleteSet(id: String)
    suspend fun finishWorkout(id: String)
}

class LocalTrainingRepository(private val dao: WorkoutDao) : TrainingRepository {
    override fun workouts() = dao.observeWorkouts()
    override fun entries(workoutId: String) = dao.observeEntries(workoutId)
    override fun sets(entryId: String) = dao.observeSets(entryId)
    override suspend fun createWorkout(workout: WorkoutEntity) = dao.insertWorkout(workout)
    override suspend fun addExercise(entry: WorkoutEntryEntity) = dao.insertEntry(entry)
    override suspend fun addSet(set: WorkoutSetEntity) = dao.insertSet(set)
    override suspend fun updateEntry(entry: WorkoutEntryEntity) = dao.updateEntry(entry)
    override suspend fun updateSet(set: WorkoutSetEntity) = dao.updateSet(set)
    override suspend fun deleteEntry(id: String) = dao.deleteEntry(id)
    override suspend fun deleteSet(id: String) = dao.deleteSet(id)
    override suspend fun finishWorkout(id: String) = dao.finishWorkout(id, System.currentTimeMillis())
}

interface ExerciseRepository {
    fun customExercises(): Flow<List<CustomExerciseEntity>>
    suspend fun saveCustomExercise(exercise: CustomExerciseEntity)
}

class LocalExerciseRepository(private val dao: CustomExerciseDao) : ExerciseRepository {
    override fun customExercises() = dao.observeAll()
    override suspend fun saveCustomExercise(exercise: CustomExerciseEntity) = dao.insert(exercise)
}
