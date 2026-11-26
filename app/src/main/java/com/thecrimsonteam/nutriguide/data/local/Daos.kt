package com.thecrimsonteam.nutriguide.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY createdAt DESC")
    fun observeMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM food_items WHERE mealId = :mealId ORDER BY name")
    suspend fun getItems(mealId: String): List<FoodItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<FoodItemEntity>)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMeal(mealId: String)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC")
    fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkout(workoutId: String): WorkoutEntity?

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Query("SELECT * FROM workout_entries WHERE workoutId = :workoutId ORDER BY sortOrder")
    fun observeEntries(workoutId: String): Flow<List<WorkoutEntryEntity>>

    @Query("SELECT * FROM workout_sets WHERE entryId = :entryId ORDER BY setNumber")
    fun observeSets(entryId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_routines ORDER BY updatedAt DESC")
    fun observeRoutines(): Flow<List<WorkoutRoutineEntity>>

    @Query("SELECT * FROM workout_routines WHERE id = :id LIMIT 1")
    suspend fun getRoutine(id: String): WorkoutRoutineEntity?

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    suspend fun getRoutineExercises(routineId: String): List<RoutineExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: WorkoutRoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: String)

    @Query("DELETE FROM workout_routines WHERE id = :id")
    suspend fun deleteRoutine(id: String)

    @Query("SELECT * FROM scheduled_workouts ORDER BY scheduledDay ASC")
    fun observeScheduledWorkouts(): Flow<List<ScheduledWorkoutEntity>>

    @Query("SELECT * FROM scheduled_workouts WHERE id = :id LIMIT 1")
    suspend fun getScheduledWorkout(id: String): ScheduledWorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledWorkout(item: ScheduledWorkoutEntity)

    @Update
    suspend fun updateScheduledWorkout(item: ScheduledWorkoutEntity)

    @Query("DELETE FROM scheduled_workouts WHERE id = :id")
    suspend fun deleteScheduledWorkout(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: WorkoutEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity)

    @Update
    suspend fun updateEntry(entry: WorkoutEntryEntity)

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: String)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteSet(setId: String)

    @Query("UPDATE workouts SET finishedAt = :finishedAt WHERE id = :workoutId")
    suspend fun finishWorkout(workoutId: String, finishedAt: Long)
}

@Dao
interface CustomExerciseDao {
    @Query("SELECT * FROM custom_exercises ORDER BY name")
    fun observeAll(): Flow<List<CustomExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: CustomExerciseEntity)

    @Query("DELETE FROM custom_exercises WHERE id = :id")
    suspend fun delete(id: String)
}
