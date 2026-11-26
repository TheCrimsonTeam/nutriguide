package com.thecrimsonteam.nutriguide.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.thecrimsonteam.nutriguide.data.catalog.ExerciseCatalog
import com.thecrimsonteam.nutriguide.data.catalog.ExerciseDefinition
import com.thecrimsonteam.nutriguide.data.local.FoodItemEntity
import com.thecrimsonteam.nutriguide.data.local.MealEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutEntryEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutSetEntity
import com.thecrimsonteam.nutriguide.domain.model.FoodDraft
import com.thecrimsonteam.nutriguide.ui.theme.NutriGuideTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")

@Composable
fun NutriGuideApp(viewModel: AppViewModel) {
    val context = LocalContext.current
    val revision = remember { mutableIntStateOf(0) }
    DisposableEffect(viewModel) {
        val observer = { revision.intValue++ }
        viewModel.observe(observer)
        onDispose { viewModel.removeObserver(observer) }
    }
    revision.intValue

    val preferences = remember {
        context.getSharedPreferences("nutriguide_preferences", Context.MODE_PRIVATE)
    }
    var themeMode by rememberSaveable {
        mutableStateOf(preferences.getString("theme", "System") ?: "System")
    }
    LaunchedEffect(themeMode) {
        preferences.edit().putString("theme", themeMode).apply()
    }

    var route by rememberSaveable { mutableStateOf("nutrition") }
    var selectedImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMealId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedExerciseId by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri.toString()
            viewModel.setDrafts(emptyList())
            viewModel.analyzePhoto(context, uri)
            route = "review"
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) route = "camera"
    }

    NutriGuideTheme(mode = themeMode) {
        Scaffold(
            topBar = {
                if (route != "camera") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("nutri", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("guide", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        if (route == "settings") {
                            IconButton(onClick = { route = "nutrition" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = { route = "settings" }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (route == "nutrition" || route == "workouts") {
                    NavigationBar {
                        NavigationBarItem(
                            selected = route == "nutrition",
                            onClick = { route = "nutrition" },
                            icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                            label = { Text("Nutrition") }
                        )
                        NavigationBarItem(
                            selected = route == "workouts",
                            onClick = { route = "workouts" },
                            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                            label = { Text("Workouts") }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (route) {
                    "nutrition" -> NutritionHome(
                        meals = viewModel.meals,
                        onScan = { route = "scan" },
                        onManual = { viewModel.setDrafts(emptyList()); selectedImageUri = null; route = "review" },
                        onDiary = { route = "diary" },
                        onMeal = { id -> selectedMealId = id; route = "meal-detail" }
                    )
                    "scan" -> ScanScreen(
                        onCamera = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (granted) route = "camera" else cameraPermission.launch(Manifest.permission.CAMERA)
                        },
                        onGallery = { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onBack = { route = "nutrition" }
                    )
                    "camera" -> CameraCaptureScreen(
                        onPhotoCaptured = { uri ->
                            selectedImageUri = uri.toString()
                            viewModel.setDrafts(emptyList())
                            viewModel.analyzePhoto(context, uri)
                            route = "review"
                        },
                        onCancel = { route = "scan" }
                    )
                    "review" -> FoodReviewScreen(
                        viewModel = viewModel,
                        imageUri = selectedImageUri,
                        onSaved = { route = "nutrition" },
                        onBack = { route = "nutrition" }
                    )
                    "diary" -> MealDiaryScreen(
                        meals = viewModel.meals,
                        onMeal = { id -> selectedMealId = id; route = "meal-detail" },
                        onDelete = viewModel::deleteMeal,
                        onBack = { route = "nutrition" }
                    )
                    "meal-detail" -> MealDetailScreen(
                        mealId = selectedMealId,
                        meals = viewModel.meals,
                        viewModel = viewModel,
                        onBack = { route = "diary" }
                    )
                    "workouts" -> WorkoutHome(
                        workouts = viewModel.workouts,
                        onStart = { viewModel.startWorkout(); route = "library" },
                        onLibrary = { route = "library" },
                        onHistory = { route = "history" }
                    )
                    "library" -> ExerciseLibraryScreen(
                        viewModel = viewModel,
                        selectedExerciseId = selectedExerciseId,
                        onSelect = { id -> selectedExerciseId = id; route = "exercise-detail" },
                        onBack = { route = if (viewModel.activeWorkoutId == null) "workouts" else "active-workout" },
                        onAddToWorkout = { exercise ->
                            if (viewModel.activeWorkoutId == null) viewModel.startWorkout()
                            viewModel.addExerciseToWorkout(exercise)
                            route = "active-workout"
                        }
                    )
                    "exercise-detail" -> {
                        val selected = (ExerciseCatalog.all + viewModel.customExercises.map { it.toDefinition() })
                            .firstOrNull { it.id == selectedExerciseId }
                        ExerciseDetailScreen(
                            exercise = selected,
                            onAdd = {
                                selected?.let {
                                    if (viewModel.activeWorkoutId == null) viewModel.startWorkout()
                                    viewModel.addExerciseToWorkout(it)
                                }
                                route = "active-workout"
                            },
                            onBack = { route = "library" }
                        )
                    }
                    "active-workout" -> ActiveWorkoutScreen(
                        viewModel = viewModel,
                        onAddExercise = { route = "library" },
                        onFinish = { viewModel.finishWorkout(); route = "workouts" }
                    )
                    "history" -> WorkoutHistoryScreen(
                        workouts = viewModel.workouts,
                        onBack = { route = "workouts" }
                    )
                    "settings" -> SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = { themeMode = it },
                        onBack = { route = "nutrition" }
                    )
                    else -> NutritionHome(viewModel.meals, { route = "scan" }, { route = "review" }, { route = "diary" }, { id -> selectedMealId = id; route = "meal-detail" })
                }
            }
        }
    }
}

@Composable
private fun NutritionHome(
    meals: List<MealEntity>,
    onScan: () -> Unit,
    onManual: () -> Unit,
    onDiary: () -> Unit,
    onMeal: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Nutrition", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text("Keep a simple record of the meals you enjoy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your food diary", style = MaterialTheme.typography.titleMedium)
                Text("Log meals with a photo or add the foods yourself. Nutrition values are estimates and can be reviewed before saving.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan a meal")
        }
        OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add meal manually")
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Recent meals", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onDiary) { Text("View diary") }
        }
        if (meals.isEmpty()) {
            EmptyCard("No meals logged yet", "Your saved meals will appear here. You can add foods manually or try a photo scan.")
        } else {
            meals.take(8).forEach { meal ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onMeal(meal.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(meal.mealType, style = MaterialTheme.typography.titleMedium)
                            Text(formatDate(meal.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Details  ›", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Text("Nutrition is personal. NutriGuide records information without daily targets or judgement.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScanScreen(onCamera: () -> Unit, onGallery: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackHeading("Scan a meal", onBack)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                Text("Photograph the whole plate", style = MaterialTheme.typography.titleMedium)
                Text("Try even lighting and include every visible food. Mixed meals may need you to confirm components, ingredients and serving sizes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Take a photo")
        }
        OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose from gallery")
        }
        Text("A photo cannot reliably reveal hidden ingredients, exact weights or cooking oil. You will review every estimate before it is saved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FoodReviewScreen(
    viewModel: AppViewModel,
    imageUri: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    var mealType by rememberSaveable { mutableStateOf("Lunch") }
    var newName by rememberSaveable { mutableStateOf("") }
    var newGrams by rememberSaveable { mutableStateOf("100") }
    var newCalories by rememberSaveable { mutableStateOf("") }
    var newProtein by rememberSaveable { mutableStateOf("") }
    var newCarbs by rememberSaveable { mutableStateOf("") }
    var newFat by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackHeading("Review meal", onBack)
        Text("Meal type", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            mealTypes.forEach { type ->
                FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(type) })
            }
        }
        if (viewModel.isAnalyzing) {
            Card {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Reviewing the photo…")
                }
            }
        }
        if (viewModel.analysisMessage.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(viewModel.analysisMessage, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        if (imageUri != null) {
            Text("Photo attached for this entry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Food components", style = MaterialTheme.typography.titleLarge)
        if (viewModel.foodDrafts.isEmpty()) {
            EmptyCard("No confirmed foods yet", "Add each visible food below. Leave a nutrient field blank when you do not know its value.")
        }
        viewModel.foodDrafts.forEach { draft ->
            FoodDraftCard(draft = draft, onChange = viewModel::updateDraft, onRemove = { viewModel.removeDraft(draft.id) })
        }
        Divider()
        Text("Add or correct a food", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(newName, { newName = it }, label = { Text("Food name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(newGrams, { newGrams = it }, label = { Text("Serving amount (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(newCalories, { newCalories = it }, "kcal", Modifier.weight(1f))
            NumberField(newProtein, { newProtein = it }, "Protein g", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(newCarbs, { newCarbs = it }, "Carbs g", Modifier.weight(1f))
            NumberField(newFat, { newFat = it }, "Fat g", Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = {
                val grams = newGrams.toDoubleOrNull() ?: 0.0
                if (newName.isNotBlank() && grams > 0) {
                    viewModel.addDraft(
                        FoodDraft(
                            id = UUID.randomUUID().toString(),
                            name = newName.trim(),
                            portionGrams = grams,
                            caloriesKcal = newCalories.toDoubleOrNull(),
                            proteinG = newProtein.toDoubleOrNull(),
                            carbsG = newCarbs.toDoubleOrNull(),
                            fatG = newFat.toDoubleOrNull(),
                            nutritionSource = if (newCalories.isBlank() && newProtein.isBlank() && newCarbs.isBlank() && newFat.isBlank()) null else "User-entered",
                            confidence = null
                        )
                    )
                    newName = ""; newGrams = "100"; newCalories = ""; newProtein = ""; newCarbs = ""; newFat = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add food component")
        }
        Button(
            onClick = { viewModel.saveMeal(mealType, imageUri); onSaved() },
            enabled = viewModel.foodDrafts.any { it.name.isNotBlank() && it.portionGrams > 0 } && !viewModel.isAnalyzing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save meal")
        }
        viewModel.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Blank nutrition fields remain unknown; NutriGuide does not fill missing values with guesses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FoodDraftCard(draft: FoodDraft, onChange: (FoodDraft) -> Unit, onRemove: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Food item", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove food")
                    Text("Remove")
                }
            }
            OutlinedTextField(draft.name, { onChange(draft.copy(name = it)) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                draft.portionGrams.toString().removeSuffix(".0"),
                { value -> onChange(draft.copy(portionGrams = value.toDoubleOrNull() ?: 0.0)) },
                label = { Text("Serving (g)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Text(
                "Energy: ${draft.caloriesKcal?.let { "%.0f kcal".format(it) } ?: "not available"} · Protein: ${draft.proteinG?.let { "%.1f g".format(it) } ?: "not available"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Carbohydrate: ${draft.carbsG?.let { "%.1f g".format(it) } ?: "not available"} · Fat: ${draft.fatG?.let { "%.1f g".format(it) } ?: "not available"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (draft.nutritionSource != null) Text("Source: ${draft.nutritionSource}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            draft.confidence?.let { Text("Recognition confidence: ${(it * 100).toInt()}% — review this item.", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun MealDiaryScreen(
    meals: List<MealEntity>,
    onMeal: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        BackHeading("Food diary", onBack)
        Spacer(Modifier.height(10.dp))
        if (meals.isEmpty()) {
            EmptyCard("No meals yet", "Meals you save will be organised here by date and meal type.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(meals, key = { it.id }) { meal ->
                    Card {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { onMeal(meal.id) }) {
                                Text(meal.mealType, style = MaterialTheme.typography.titleMedium)
                                Text(formatDate(meal.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onMeal(meal.id) }) { Text("View") }
                            IconButton(onClick = { onDelete(meal.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete meal")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealDetailScreen(
    mealId: String?,
    meals: List<MealEntity>,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var items by remember { mutableStateOf<List<FoodItemEntity>>(emptyList()) }
    val meal = meals.firstOrNull { it.id == mealId }
    LaunchedEffect(mealId) {
        if (mealId != null) items = viewModel.mealItems(mealId)
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackHeading("Meal details", onBack)
        if (meal != null) {
            Text("${meal.mealType} · ${formatDate(meal.createdAt)}", style = MaterialTheme.typography.titleMedium)
        }
        if (items.isEmpty()) {
            EmptyCard("No food items", "This meal has no saved food components.")
        } else {
            items.forEach { item ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("${item.portionGrams} g serving", style = MaterialTheme.typography.bodySmall)
                        Text("Energy: ${item.caloriesKcal?.let { "%.0f kcal".format(it) } ?: "unknown"}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Protein ${item.proteinG?.let { "%.1f g".format(it) } ?: "—"} · Carbs ${item.carbsG?.let { "%.1f g".format(it) } ?: "—"} · Fat ${item.fatG?.let { "%.1f g".format(it) } ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        item.nutritionSource?.let { Text("Source: $it", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHome(
    workouts: List<com.thecrimsonteam.nutriguide.data.local.WorkoutEntity>,
    onStart: () -> Unit,
    onLibrary: () -> Unit,
    onHistory: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Workouts", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text("Choose movements and record the details that matter for each one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start a workout")
        }
        OutlinedButton(onClick = onLibrary, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Browse exercise library")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent workouts", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onHistory) {
                Icon(Icons.Default.History, contentDescription = null)
                Text("History")
            }
        }
        if (workouts.isEmpty()) {
            EmptyCard("No workouts saved", "Start a workout, add exercises and log sets, time or distance.")
        } else {
            workouts.take(8).forEach { workout ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(workout.name, style = MaterialTheme.typography.titleMedium)
                        Text(formatDate(workout.startedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (workout.finishedAt == null) "In progress" else "Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryScreen(
    viewModel: AppViewModel,
    selectedExerciseId: String?,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onAddToWorkout: (ExerciseDefinition) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    var showCustomForm by rememberSaveable { mutableStateOf(false) }
    var customName by rememberSaveable { mutableStateOf("") }
    var customEquipment by rememberSaveable { mutableStateOf("Bodyweight") }
    var customCategory by rememberSaveable { mutableStateOf("Custom") }
    var customMuscles by rememberSaveable { mutableStateOf("") }

    val customs = viewModel.customExercises.map { custom ->
        ExerciseDefinition(
            id = custom.id,
            name = custom.name,
            category = custom.category,
            equipment = custom.equipment,
            primaryMuscles = custom.primaryMuscles,
            secondaryMuscles = custom.secondaryMuscles,
            difficulty = custom.difficulty,
            trackingType = custom.trackingType,
            instructions = custom.instructions,
            aliases = custom.aliases.split("|")
        )
    }
    val allExercises = ExerciseCatalog.all + customs
    val categories = listOf("All") + (ExerciseCatalog.categories + viewModel.customExercises.map { it.category }).distinct()
    val results = allExercises.filter { exercise ->
        (category == "All" || exercise.category == category) &&
            (query.isBlank() || exercise.name.contains(query, true) ||
                exercise.primaryMuscles.contains(query, true) ||
                exercise.equipment.contains(query, true) ||
                exercise.aliases.any { it.contains(query, true) })
    }.sortedBy { it.name }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        BackHeading("Exercise library", onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Exercise, muscle or equipment") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories, key = { it }) { item ->
                FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${results.size} exercises", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { showCustomForm = !showCustomForm }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Custom")
            }
        }
        if (showCustomForm) {
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create your exercise", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(customName, { customName = it }, label = { Text("Exercise name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(customCategory, { customCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(customEquipment, { customEquipment = it }, label = { Text("Equipment") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(customMuscles, { customMuscles = it }, label = { Text("Primary muscles") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(
                        onClick = {
                            viewModel.saveCustomExercise(customName, customCategory, customEquipment, customMuscles, "", "", "STRENGTH")
                            customName = ""; customMuscles = ""; showCustomForm = false
                        },
                        enabled = customName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save custom exercise") }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.id }) { exercise ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(exercise.id) }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                            Text("${exercise.category} · ${exercise.equipment}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onAddToWorkout(exercise) }) { Text("Add") }
                    }
                }
            }
        }
        if (results.isEmpty()) EmptyCard("No matching exercises", "Try a different name, category or muscle.")
    }
}

@Composable
private fun ExerciseDetailScreen(exercise: ExerciseDefinition?, onAdd: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackHeading("Exercise details", onBack)
        if (exercise == null) {
            EmptyCard("Exercise not found", "Return to the catalogue and choose another item.")
        } else {
            Text(exercise.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("${exercise.category} · ${exercise.equipment} · ${exercise.difficulty}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            InfoCard("Primary muscles", exercise.primaryMuscles)
            InfoCard("Secondary muscles", exercise.secondaryMuscles.ifBlank { "Not specified" })
            InfoCard("Tracking", when (exercise.trackingType) {
                "CARDIO" -> "Duration and distance"
                "DURATION" -> "Duration"
                else -> "Sets, repetitions and optional weight"
            })
            InfoCard("Guidance", exercise.instructions)
            if (exercise.aliases.isNotEmpty()) InfoCard("Also known as", exercise.aliases.joinToString(", "))
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add to workout") }
        }
    }
}

@Composable
private fun ActiveWorkoutScreen(viewModel: AppViewModel, onAddExercise: () -> Unit, onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("Workout in progress", style = MaterialTheme.typography.headlineMedium)
        Text("Log each exercise using the fields that apply.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        if (viewModel.activeEntries.isEmpty()) {
            EmptyCard("No exercises added", "Browse the exercise library to add movements to this workout.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 10.dp)
            ) {
                items(viewModel.activeEntries, key = { it.id }) { entry ->
                    WorkoutEntryCard(entry, viewModel)
                }
            }
        }
        OutlinedButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add exercise")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish workout") }
    }
}

@Composable
private fun WorkoutEntryCard(entry: WorkoutEntryEntity, viewModel: AppViewModel) {
    var reps by rememberSaveable(entry.id) { mutableStateOf("8") }
    var weight by rememberSaveable(entry.id) { mutableStateOf("") }
    var duration by rememberSaveable(entry.id) { mutableStateOf("20") }
    var distance by rememberSaveable(entry.id) { mutableStateOf("") }
    LaunchedEffect(entry.id) { viewModel.observeSets(entry.id) }

    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.exerciseName, style = MaterialTheme.typography.titleMedium)
            Text(entry.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (entry.trackingType) {
                "CARDIO" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(duration, { duration = it }, "Duration (min)", Modifier.weight(1f))
                        NumberField(distance, { distance = it }, "Distance (km)", Modifier.weight(1f))
                    }
                }
                "DURATION" -> NumberField(duration, { duration = it }, "Duration (min)", Modifier.fillMaxWidth())
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(reps, { reps = it }, "Reps", Modifier.weight(1f))
                        NumberField(weight, { weight = it }, "Weight (kg)", Modifier.weight(1f))
                    }
                }
            }
            Button(
                onClick = {
                    when (entry.trackingType) {
                        "CARDIO" -> viewModel.addSet(entry, null, null, duration.toIntOrNull()?.times(60), distance.toDoubleOrNull()?.times(1000.0))
                        "DURATION" -> viewModel.addSet(entry, null, null, duration.toIntOrNull()?.times(60), null)
                        else -> viewModel.addSet(entry, reps.toIntOrNull(), weight.toDoubleOrNull(), null, null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save set / activity") }
            viewModel.setsByEntry[entry.id].orEmpty().forEach { set ->
                Text(formatSet(entry, set), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WorkoutHistoryScreen(
    workouts: List<com.thecrimsonteam.nutriguide.data.local.WorkoutEntity>,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        BackHeading("Workout history", onBack)
        Spacer(Modifier.height(8.dp))
        if (workouts.isEmpty()) {
            EmptyCard("No sessions yet", "Completed workouts will appear here.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(workouts, key = { it.id }) { workout ->
                    InfoCard(
                        workout.name,
                        "${formatDate(workout.startedAt)} · ${if (workout.finishedAt == null) "In progress" else "Completed"}"
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(themeMode: String, onThemeChange: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackHeading("Settings", onBack)
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        Text("Choose how NutriGuide looks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf("System", "Light", "Dark").forEach { mode ->
            FilterChip(
                selected = themeMode == mode,
                onClick = { onThemeChange(mode) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (mode == "Dark") Icon(Icons.Default.DarkMode, contentDescription = null)
                        else if (mode == "Light") Icon(Icons.Default.LightMode, contentDescription = null)
                        Text(mode)
                    }
                }
            )
        }
        InfoCard("Privacy", "Meal entries are stored locally on this device. Photo analysis is disabled until you configure a trusted backend endpoint.")
        InfoCard("Nutrition estimates", "Food photos cannot reliably reveal exact serving weights, hidden ingredients or preparation details. Confirm unclear information before saving.")
        InfoCard("Scope", "NutriGuide currently focuses on food logging and exercise tracking.")
    }
}

@Composable
private fun FoodReviewScreenPreviewText() {}

@Composable
private fun BackHeading(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun EmptyCard(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(title: String, description: String) {
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun com.thecrimsonteam.nutriguide.data.local.CustomExerciseEntity.toDefinition() =
    ExerciseDefinition(
        id = id,
        name = name,
        category = category,
        equipment = equipment,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = secondaryMuscles,
        difficulty = difficulty,
        trackingType = trackingType,
        aliases = aliases.split("|"),
        instructions = instructions
    )

private fun formatDate(time: Long): String =
    SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(time))

private fun formatSet(entry: WorkoutEntryEntity, set: WorkoutSetEntity): String = when (entry.trackingType) {
    "CARDIO" -> "Activity ${set.setNumber}: ${set.durationSeconds?.div(60) ?: 0} min · ${set.distanceMeters?.div(1000.0)?.let { "%.2f km".format(it) } ?: "distance not set"}"
    "DURATION" -> "Set ${set.setNumber}: ${set.durationSeconds?.div(60) ?: 0} min"
    else -> "Set ${set.setNumber}: ${set.repetitions ?: "—"} reps · ${set.weightKg?.let { "$it kg" } ?: "bodyweight / not set"}"
}
