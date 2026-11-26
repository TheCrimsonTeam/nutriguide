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
        ).addMigrations(MIGRATION_1_2).build()

        val factory = AppViewModelFactory(
            nutrition = LocalNutritionRepository(database, database.mealDao()),
            training = LocalTrainingRepository(database.workoutDao()),
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
