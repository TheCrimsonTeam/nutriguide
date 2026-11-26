package com.thecrimsonteam.nutriguide.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecrimsonteam.nutriguide.data.local.CustomExerciseEntity
import com.thecrimsonteam.nutriguide.data.local.FoodItemEntity
import com.thecrimsonteam.nutriguide.data.local.MealEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutEntryEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutSetEntity
import com.thecrimsonteam.nutriguide.data.remote.FoodAnalysisApi
import com.thecrimsonteam.nutriguide.data.repository.ExerciseRepository
import com.thecrimsonteam.nutriguide.data.repository.NutritionRepository
import com.thecrimsonteam.nutriguide.data.repository.TrainingRepository
import com.thecrimsonteam.nutriguide.data.catalog.ExerciseDefinition
import com.thecrimsonteam.nutriguide.domain.model.FoodDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class AppViewModel(
    private val nutrition: NutritionRepository,
    private val training: TrainingRepository,
    private val exercises: ExerciseRepository,
    private val foodApi: FoodAnalysisApi?
) : ViewModel() {
    var meals: List<MealEntity> = emptyList()
        private set
    var workouts: List<WorkoutEntity> = emptyList()
        private set
    var customExercises: List<CustomExerciseEntity> = emptyList()
        private set
    var foodDrafts: List<FoodDraft> = emptyList()
        private set
    var activeEntries: List<WorkoutEntryEntity> = emptyList()
        private set
    var setsByEntry: Map<String, List<WorkoutSetEntity>> = emptyMap()
        private set
    var activeWorkoutId: String? = null
        private set
    var isAnalyzing: Boolean = false
        private set
    var analysisMessage: String = ""
        private set
    var errorMessage: String? = null
        private set

    init {
        viewModelScope.launch { nutrition.meals().collectLatest { meals = it; refresh() } }
        viewModelScope.launch { training.workouts().collectLatest { workouts = it; refresh() } }
        viewModelScope.launch { exercises.customExercises().collectLatest { customExercises = it; refresh() } }
    }

    private val observers = mutableSetOf<() -> Unit>()
    private var activeEntriesJob: Job? = null
    fun observe(observer: () -> Unit) { observers += observer }
    fun removeObserver(observer: () -> Unit) { observers -= observer }
    private fun refresh() { observers.toList().forEach { it() } }

    fun setDrafts(items: List<FoodDraft>) { foodDrafts = items; refresh() }
    fun addDraft(item: FoodDraft) { foodDrafts = foodDrafts + item; refresh() }
    fun removeDraft(id: String) { foodDrafts = foodDrafts.filterNot { it.id == id }; refresh() }
    fun updateDraft(item: FoodDraft) {
        foodDrafts = foodDrafts.map { if (it.id == item.id) item else it }
        refresh()
    }

    fun analyzePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            isAnalyzing = true
            analysisMessage = ""
            refresh()
            val api = foodApi
            if (api == null) {
                analysisMessage = "Photo analysis is not connected yet. You can still add the foods and confirm their nutrition manually."
                deleteTemporaryCameraPhoto(context, uri)
                isAnalyzing = false
                refresh()
                return@launch
            }
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("The selected photo could not be read.")
                require(bytes.isNotEmpty()) { "The selected photo is empty." }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val body = bytes.toRequestBody(mime.toMediaType())
                val photo = MultipartBody.Part.createFormData("photo", "meal-photo", body)
                val response = api.analyze(photo)
                foodDrafts = response.items.map { item ->
                    FoodDraft(
                        id = UUID.randomUUID().toString(),
                        name = item.name,
                        portionGrams = item.portionGrams ?: 0.0,
                        caloriesKcal = item.caloriesKcal,
                        proteinG = item.proteinG,
                        carbsG = item.carbsG,
                        fatG = item.fatG,
                        nutritionSource = item.nutritionSource,
                        confidence = item.confidence
                    )
                }
                analysisMessage = response.message ?: if (foodDrafts.isEmpty()) {
                    "No food components were confidently identified. Add the foods manually."
                } else {
                    "Review each food and serving. Uncertain values are not treated as confirmed."
                }
            } catch (e: Exception) {
                analysisMessage = "We couldn't analyse this photo: ${e.message ?: "service unavailable"}. You can enter the meal manually."
            } finally {
                deleteTemporaryCameraPhoto(context, uri)
                isAnalyzing = false
                refresh()
            }
        }
    }

    private fun deleteTemporaryCameraPhoto(context: Context, uri: Uri) {
        if (uri.scheme != "file") return
        val path = uri.path ?: return
        runCatching {
            val file = java.io.File(path).canonicalFile
            val cacheRoot = context.cacheDir.canonicalFile
            if (file.path.startsWith(cacheRoot.path + java.io.File.separator)) {
                file.delete()
            }
        }
    }

    fun saveMeal(mealType: String, imageUri: String?) {
        viewModelScope.launch {
            val cleaned = foodDrafts.filter { it.name.isNotBlank() && it.portionGrams > 0.0 }
            if (cleaned.isEmpty()) {
                errorMessage = "Add at least one food and a serving amount before saving."
                refresh()
                return@launch
            }
            // Raw meal photos are not retained in the diary by default.
            val meal = MealEntity(mealType = mealType, imageUri = null)
            nutrition.saveMeal(meal, cleaned.map { it.toEntity(meal.id) })
            foodDrafts = emptyList()
            analysisMessage = "Meal saved on this device."
            errorMessage = null
            refresh()
        }
    }

    suspend fun mealItems(mealId: String): List<FoodItemEntity> = nutrition.items(mealId)
    fun deleteMeal(mealId: String) { viewModelScope.launch { nutrition.deleteMeal(mealId) } }

    fun startWorkout(name: String = "Workout") {
        activeEntriesJob?.cancel()
        val workout = WorkoutEntity(name = name.ifBlank { "Workout" })
        activeWorkoutId = workout.id
        activeEntries = emptyList()
        setsByEntry = emptyMap()
        refresh()
        activeEntriesJob = viewModelScope.launch {
            training.createWorkout(workout)
            training.entries(workout.id).collectLatest { activeEntries = it; refresh() }
        }
    }

    fun addExerciseToWorkout(exercise: ExerciseDefinition) {
        val workoutId = activeWorkoutId ?: return
        viewModelScope.launch {
            training.addExercise(
                WorkoutEntryEntity(
                    workoutId = workoutId,
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    category = exercise.category,
                    trackingType = exercise.trackingType,
                    sortOrder = (activeEntries.maxOfOrNull { it.sortOrder } ?: -1) + 1
                )
            )
        }
    }

    fun moveEntry(entryId: String, offset: Int) {
        val from = activeEntries.indexOfFirst { it.id == entryId }
        val to = from + offset
        if (from < 0 || to !in activeEntries.indices) return
        val reordered = activeEntries.toMutableList()
        val moved = reordered.removeAt(from)
        reordered.add(to, moved)
        viewModelScope.launch {
            reordered.forEachIndexed { index, entry ->
                training.updateEntry(entry.copy(sortOrder = index))
            }
        }
    }

    fun removeEntry(entryId: String) {
        viewModelScope.launch {
            training.deleteEntry(entryId)
            setsByEntry = setsByEntry - entryId
            refresh()
        }
    }

    fun observeSets(entryId: String) {
        if (setsByEntry.containsKey(entryId)) return
        viewModelScope.launch {
            training.sets(entryId).collectLatest { sets ->
                setsByEntry = setsByEntry + (entryId to sets)
                refresh()
            }
        }
    }

    fun addSet(entry: WorkoutEntryEntity, reps: Int?, weightKg: Double?, durationSeconds: Int?, distanceMeters: Double?) {
        viewModelScope.launch {
            val nextNumber = (setsByEntry[entry.id]?.maxOfOrNull { it.setNumber } ?: 0) + 1
            training.addSet(
                WorkoutSetEntity(
                    entryId = entry.id,
                    setNumber = nextNumber,
                    repetitions = reps,
                    weightKg = weightKg,
                    durationSeconds = durationSeconds,
                    distanceMeters = distanceMeters
                )
            )
        }
    }

    fun updateSet(set: WorkoutSetEntity) {
        viewModelScope.launch { training.updateSet(set) }
    }

    fun removeSet(setId: String) {
        viewModelScope.launch { training.deleteSet(setId) }
    }

    fun finishWorkout() {
        val id = activeWorkoutId ?: return
        activeEntriesJob?.cancel()
        activeEntriesJob = null
        viewModelScope.launch {
            training.finishWorkout(id)
            activeWorkoutId = null
            activeEntries = emptyList()
            setsByEntry = emptyMap()
            refresh()
        }
    }

    fun saveCustomExercise(
        name: String,
        category: String,
        equipment: String,
        primaryMuscles: String,
        secondaryMuscles: String,
        instructions: String,
        trackingType: String
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                errorMessage = "Enter an exercise name."
                refresh()
                return@launch
            }
            exercises.saveCustomExercise(
                CustomExerciseEntity(
                    name = name.trim(),
                    category = category,
                    equipment = equipment,
                    primaryMuscles = primaryMuscles,
                    secondaryMuscles = secondaryMuscles,
                    difficulty = "User-created",
                    instructions = instructions.ifBlank { "Use a comfortable, controlled range and appropriate equipment setup." },
                    trackingType = trackingType
                )
            )
            errorMessage = null
            refresh()
        }
    }
}
