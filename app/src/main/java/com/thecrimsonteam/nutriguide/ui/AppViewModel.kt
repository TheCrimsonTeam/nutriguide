package com.thecrimsonteam.nutriguide.ui

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecrimsonteam.nutriguide.data.local.CustomExerciseEntity
import com.thecrimsonteam.nutriguide.data.local.FoodItemEntity
import com.thecrimsonteam.nutriguide.data.local.MealEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutEntryEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutSetEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutRoutineEntity
import com.thecrimsonteam.nutriguide.data.local.RoutineExerciseEntity
import com.thecrimsonteam.nutriguide.data.local.ScheduledWorkoutEntity
import com.thecrimsonteam.nutriguide.data.remote.FoodAnalysisApi
import com.thecrimsonteam.nutriguide.data.repository.ExerciseRepository
import com.thecrimsonteam.nutriguide.data.repository.NutritionRepository
import com.thecrimsonteam.nutriguide.data.repository.TrainingRepository
import com.thecrimsonteam.nutriguide.data.catalog.ExerciseDefinition
import com.thecrimsonteam.nutriguide.domain.model.FoodDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.time.LocalDate

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
    var routines: List<WorkoutRoutineEntity> = emptyList()
        private set
    var scheduledWorkouts: List<ScheduledWorkoutEntity> = emptyList()
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
    var activeWorkout: WorkoutEntity? = null
        private set
    var isAnalyzing: Boolean = false
        private set
    var analysisMessage: String = ""
        private set
    var errorMessage: String? = null
        private set

    init {
        viewModelScope.launch { nutrition.meals().collectLatest { meals = it; refresh() } }
        viewModelScope.launch {
            training.workouts().collectLatest { latest ->
                workouts = latest
                activeWorkoutId?.let { id ->
                    activeWorkout = latest.firstOrNull { it.id == id } ?: activeWorkout
                }
                if (!recoveredSession) {
                    recoveredSession = true
                    latest.firstOrNull { it.finishedAt == null }?.let(::recoverWorkout)
                }
                refresh()
            }
        }
        viewModelScope.launch { exercises.customExercises().collectLatest { customExercises = it; refresh() } }
        viewModelScope.launch { training.routines().collectLatest { routines = it; refresh() } }
        viewModelScope.launch { training.scheduledWorkouts().collectLatest { scheduledWorkouts = it; refresh() } }
    }

    private val observers = mutableSetOf<() -> Unit>()
    private var activeEntriesJob: Job? = null
    private var sessionCheckpointJob: Job? = null
    private var recoveredSession = false
    private var activeScheduledWorkoutId: String? = null
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
                        confidence = item.confidence,
                        fiberG = item.fiberG,
                        sugarG = item.sugarG,
                        alternativeNames = item.alternatives,
                        servingDescription = item.servingDescription,
                        pieceCount = item.pieceCount,
                        identificationConfidence = item.confidenceLabel,
                        portionUncertainty = item.portionUncertainty,
                        visualObservations = item.visualObservations,
                        nutritionCandidates = item.nutritionCandidates.map { candidate ->
                            com.thecrimsonteam.nutriguide.domain.model.FoodNutritionCandidate(
                                fdcId = candidate.fdcId,
                                description = candidate.description,
                                dataType = candidate.dataType,
                                matchScore = candidate.matchScore,
                                caloriesKcalPer100g = candidate.caloriesKcalPer100g,
                                proteinGPer100g = candidate.proteinGPer100g,
                                carbsGPer100g = candidate.carbsGPer100g,
                                fatGPer100g = candidate.fatGPer100g,
                                fiberGPer100g = candidate.fiberGPer100g,
                                sugarGPer100g = candidate.sugarGPer100g,
                                source = candidate.source
                            )
                        }
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
        val workout = WorkoutEntity(
            name = name.ifBlank { "Workout" },
            runningSinceElapsedRealtime = SystemClock.elapsedRealtime()
        )
        activateWorkout(workout, null)
        viewModelScope.launch { training.createWorkout(workout) }
    }

    private fun activateWorkout(workout: WorkoutEntity, scheduledId: String?) {
        activeEntriesJob?.cancel()
        sessionCheckpointJob?.cancel()
        recoveredSession = true
        activeScheduledWorkoutId = scheduledId
        activeWorkout = workout
        activeWorkoutId = workout.id
        activeEntries = emptyList()
        setsByEntry = emptyMap()
        refresh()
        observeWorkoutEntries(workout.id)
        startSessionCheckpoints()
    }

    fun saveRoutine(
        id: String?,
        name: String,
        description: String,
        selectedExercises: List<ExerciseDefinition>
    ) {
        viewModelScope.launch {
            if (name.isBlank() || selectedExercises.isEmpty()) {
                errorMessage = "Add a routine name and at least one exercise."
                refresh()
                return@launch
            }
            val previous = id?.let { training.getRoutine(it) }
            val routineId = previous?.id ?: UUID.randomUUID().toString()
            val previousItems = previous?.let { training.routineExercises(it.id) }.orEmpty()
            val previousByExercise = previousItems.associateBy { it.exerciseId }
            val now = System.currentTimeMillis()
            val routine = WorkoutRoutineEntity(
                id = routineId,
                name = name.trim(),
                description = description.trim(),
                createdAt = previous?.createdAt ?: now,
                updatedAt = now
            )
            val entries = selectedExercises.distinctBy { it.id }.mapIndexed { index, exercise ->
                val old = previousByExercise[exercise.id]
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    category = exercise.category,
                    trackingType = exercise.trackingType,
                    sortOrder = index,
                    targetSets = old?.targetSets ?: if (exercise.trackingType == "CARDIO" || exercise.trackingType == "DURATION") 1 else 3,
                    targetReps = old?.targetReps ?: 8,
                    targetWeightKg = old?.targetWeightKg,
                    restSeconds = old?.restSeconds ?: 60,
                    targetDurationSeconds = old?.targetDurationSeconds,
                    targetDistanceMeters = old?.targetDistanceMeters,
                    notes = old?.notes.orEmpty()
                )
            }
            training.saveRoutine(routine, entries)
            errorMessage = null
            refresh()
        }
    }

    fun deleteRoutine(id: String) {
        viewModelScope.launch { training.deleteRoutine(id) }
    }

    suspend fun routineExercises(id: String): List<RoutineExerciseEntity> = training.routineExercises(id)

    fun scheduleRoutine(routineId: String, day: Long) {
        viewModelScope.launch {
            training.scheduleWorkout(ScheduledWorkoutEntity(routineId = routineId, scheduledDay = day))
        }
    }

    fun moveScheduledWorkout(id: String, dayOffset: Long) {
        viewModelScope.launch {
            val current = training.getScheduledWorkout(id) ?: return@launch
            training.updateScheduledWorkout(current.copy(scheduledDay = current.scheduledDay + dayOffset))
        }
    }

    fun setScheduledStatus(id: String, status: String) {
        viewModelScope.launch {
            val current = training.getScheduledWorkout(id) ?: return@launch
            training.updateScheduledWorkout(current.copy(status = status))
        }
    }

    fun removeScheduledWorkout(id: String) {
        viewModelScope.launch { training.deleteScheduledWorkout(id) }
    }

    fun startScheduledWorkout(id: String) {
        viewModelScope.launch {
            val scheduled = training.getScheduledWorkout(id) ?: return@launch
            if (scheduled.status == "COMPLETED") return@launch
            val routine = training.getRoutine(scheduled.routineId) ?: return@launch
            val planItems = training.routineExercises(routine.id)
            val workout = WorkoutEntity(
                name = routine.name,
                runningSinceElapsedRealtime = SystemClock.elapsedRealtime()
            )
            activateWorkout(workout, scheduled.id)
            training.createWorkout(workout)
            planItems.forEach { item ->
                training.addExercise(
                    WorkoutEntryEntity(
                        workoutId = workout.id,
                        exerciseId = item.exerciseId,
                        exerciseName = item.exerciseName,
                        category = item.category,
                        trackingType = item.trackingType,
                        sortOrder = item.sortOrder,
                        plannedSets = item.targetSets,
                        targetReps = item.targetReps,
                        targetWeightKg = item.targetWeightKg,
                        restSeconds = item.restSeconds,
                        targetDurationSeconds = item.targetDurationSeconds,
                        targetDistanceMeters = item.targetDistanceMeters,
                        notes = item.notes
                    )
                )
            }
            training.updateScheduledWorkout(scheduled.copy(status = "STARTED", completedWorkoutId = workout.id))
        }
    }

    private fun recoverWorkout(saved: WorkoutEntity) {
        val now = SystemClock.elapsedRealtime()
        // elapsedRealtime resets on device reboot. Preserve the last checkpoint and pause safely.
        val recovered = if (saved.runningSinceElapsedRealtime != null && saved.runningSinceElapsedRealtime > now) {
            saved.copy(isPaused = true, runningSinceElapsedRealtime = null)
        } else saved
        activeWorkout = recovered
        activeWorkoutId = recovered.id
        if (recovered !== saved) viewModelScope.launch { training.updateWorkout(recovered) }
        observeWorkoutEntries(recovered.id)
        startSessionCheckpoints()
    }

    private fun observeWorkoutEntries(workoutId: String) {
        activeEntriesJob?.cancel()
        activeEntriesJob = viewModelScope.launch {
            training.entries(workoutId).collectLatest { activeEntries = it; refresh() }
        }
    }

    fun currentElapsedMillis(): Long {
        val workout = activeWorkout ?: return 0L
        val since = workout.runningSinceElapsedRealtime ?: return workout.elapsedMillis
        val now = SystemClock.elapsedRealtime()
        return workout.elapsedMillis + (now - since).coerceAtLeast(0L)
    }

    fun pauseWorkout() {
        val workout = activeWorkout ?: return
        if (workout.isPaused || workout.runningSinceElapsedRealtime == null) return
        val paused = workout.copy(
            elapsedMillis = currentElapsedMillis(),
            runningSinceElapsedRealtime = null,
            isPaused = true
        )
        activeWorkout = paused
        refresh()
        viewModelScope.launch { training.updateWorkout(paused) }
    }

    fun resumeWorkout() {
        val workout = activeWorkout ?: return
        if (!workout.isPaused) return
        val resumed = workout.copy(
            runningSinceElapsedRealtime = SystemClock.elapsedRealtime(),
            isPaused = false
        )
        activeWorkout = resumed
        refresh()
        viewModelScope.launch { training.updateWorkout(resumed) }
    }

    private fun startSessionCheckpoints() {
        sessionCheckpointJob?.cancel()
        sessionCheckpointJob = viewModelScope.launch {
            while (activeWorkoutId != null) {
                delay(30_000)
                val current = activeWorkout ?: continue
                val since = current.runningSinceElapsedRealtime ?: continue
                val now = SystemClock.elapsedRealtime()
                if (now >= since) {
                    val checkpoint = current.copy(
                        elapsedMillis = current.elapsedMillis + (now - since),
                        runningSinceElapsedRealtime = now
                    )
                    activeWorkout = checkpoint
                    training.updateWorkout(checkpoint)
                    refresh()
                }
            }
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
        val current = activeWorkout ?: return
        val scheduledId = activeScheduledWorkoutId
        sessionCheckpointJob?.cancel()
        sessionCheckpointJob = null
        activeEntriesJob?.cancel()
        activeEntriesJob = null
        val finished = current.copy(
            finishedAt = System.currentTimeMillis(),
            elapsedMillis = currentElapsedMillis(),
            runningSinceElapsedRealtime = null,
            isPaused = true
        )
        activeWorkout = null
        activeWorkoutId = null
        activeScheduledWorkoutId = null
        activeEntries = emptyList()
        setsByEntry = emptyMap()
        refresh()
        viewModelScope.launch {
            training.updateWorkout(finished)
            scheduledId?.let { id ->
                training.getScheduledWorkout(id)?.let { plan ->
                    training.updateScheduledWorkout(plan.copy(status = "COMPLETED", completedWorkoutId = finished.id))
                }
            }
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
