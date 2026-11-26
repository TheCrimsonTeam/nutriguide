package com.thecrimsonteam.nutriguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thecrimsonteam.nutriguide.data.local.NutriGuideDatabase
import com.thecrimsonteam.nutriguide.data.remote.FoodAnalysisClient
import com.thecrimsonteam.nutriguide.data.repository.LocalExerciseRepository
import com.thecrimsonteam.nutriguide.data.repository.LocalNutritionRepository
import com.thecrimsonteam.nutriguide.data.repository.LocalTrainingRepository
import com.thecrimsonteam.nutriguide.ui.AppViewModel
import com.thecrimsonteam.nutriguide.ui.AppViewModelFactory
import com.thecrimsonteam.nutriguide.ui.NutriGuideApp

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind transparent system bars; Compose owns safe-area spacing.
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            NutriGuideDatabase::class.java,
            "nutriguide.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

        val factory = AppViewModelFactory(
            nutrition = LocalNutritionRepository(database, database.mealDao()),
            training = LocalTrainingRepository(database, database.workoutDao()),
            exercises = LocalExerciseRepository(database.customExerciseDao()),
            foodApi = FoodAnalysisClient.createOrNull()
        )
        viewModel = ViewModelProvider(this, factory)[AppViewModel::class.java]

        setContent {
            NutriGuideApp(viewModel = viewModel)
        }
    }
}


private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE food_items ADD COLUMN fiberG REAL")
        database.execSQL("ALTER TABLE food_items ADD COLUMN sugarG REAL")
    }
}


private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE workouts ADD COLUMN elapsedMillis INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE workouts ADD COLUMN runningSinceElapsedRealtime INTEGER")
        database.execSQL("ALTER TABLE workouts ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0")
    }
}


private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE workout_entries ADD COLUMN plannedSets INTEGER")
        database.execSQL("ALTER TABLE workout_entries ADD COLUMN targetReps INTEGER")
        database.execSQL("ALTER TABLE workout_entries ADD COLUMN targetWeightKg REAL")
        database.execSQL("ALTER TABLE workout_entries ADD COLUMN restSeconds INTEGER")
        database.execSQL("ALTER TABLE workout_entries ADD COLUMN targetDurationSeconds INTEGER")
        database.execSQL("ALTER TABLE workout_entries ADD COLUMN targetDistanceMeters REAL")
        database.execSQL("ALTER TABLE workout_entries ADD COLUMN notes TEXT")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS workout_routines (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
        """.trimIndent())
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS routine_exercises (
                id TEXT NOT NULL,
                routineId TEXT NOT NULL,
                exerciseId TEXT NOT NULL,
                exerciseName TEXT NOT NULL,
                category TEXT NOT NULL,
                trackingType TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                targetSets INTEGER NOT NULL,
                targetReps INTEGER NOT NULL,
                targetWeightKg REAL,
                restSeconds INTEGER NOT NULL,
                targetDurationSeconds INTEGER,
                targetDistanceMeters REAL,
                notes TEXT NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(routineId) REFERENCES workout_routines(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_routineId ON routine_exercises(routineId)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS scheduled_workouts (
                id TEXT NOT NULL,
                routineId TEXT NOT NULL,
                scheduledDay INTEGER NOT NULL,
                status TEXT NOT NULL,
                completedWorkoutId TEXT,
                notes TEXT NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(routineId) REFERENCES workout_routines(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_workouts_routineId ON scheduled_workouts(routineId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_workouts_scheduledDay ON scheduled_workouts(scheduledDay)")
    }
}
