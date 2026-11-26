package com.thecrimsonteam.nutriguide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

data class FoodEntry(
    val id: Long,
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class NutritionTotals(val calories: Int, val protein: Double, val carbs: Double, val fat: Double)

fun calculateTotals(entries: List<FoodEntry>) = NutritionTotals(
    calories = entries.sumOf { it.calories },
    protein = entries.sumOf { it.protein },
    carbs = entries.sumOf { it.carbs },
    fat = entries.sumOf { it.fat }
)

private fun loadEntries(context: Context): List<FoodEntry> = runCatching {
    val raw = context.getSharedPreferences("nutriguide", Context.MODE_PRIVATE).getString("entries", "[]") ?: "[]"
    val array = JSONArray(raw)
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(FoodEntry(o.optLong("id"), o.optString("name"), o.optInt("calories"),
                o.optDouble("protein"), o.optDouble("carbs"), o.optDouble("fat")))
        }
    }
}.getOrDefault(emptyList())

private fun saveEntries(context: Context, entries: List<FoodEntry>) {
    val array = JSONArray()
    entries.forEach { e ->
        array.put(JSONObject().apply {
            put("id", e.id); put("name", e.name); put("calories", e.calories)
            put("protein", e.protein); put("carbs", e.carbs); put("fat", e.fat)
        })
    }
    context.getSharedPreferences("nutriguide", Context.MODE_PRIVATE).edit()
        .putString("entries", array.toString()).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NutriGuideApp() }
    }
}

@Composable
private fun NutriGuideApp() {
    val context = LocalContext.current
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    var themeChoice by rememberSaveable { mutableStateOf("system") }
    val dark = when (themeChoice) { "light" -> false; "dark" -> true; else -> systemDark }
    val scheme = if (dark) darkColorScheme(
        primary = Color.White, onPrimary = Color.Black,
        background = Color(0xFF090909), surface = Color(0xFF181818),
        onSurface = Color(0xFFF5F5F5), surfaceVariant = Color(0xFF292929),
        onSurfaceVariant = Color(0xFFB8B8B8), outline = Color(0xFF454545)
    ) else lightColorScheme(
        primary = Color(0xFF111111), onPrimary = Color.White,
        background = Color(0xFFF2F2F2), surface = Color.White,
        onSurface = Color(0xFF111111), surfaceVariant = Color(0xFFE8E8E8),
        onSurfaceVariant = Color(0xFF707070), outline = Color(0xFFD7D7D7)
    )

    var tab by rememberSaveable { mutableStateOf("Home") }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var entries by remember { mutableStateOf(loadEntries(context)) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        photoUri = uri
        photoBitmap = null
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        photoBitmap = bitmap
        photoUri = null
    }

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), tonalElevation = 0.dp) {
                    val destinations = listOf(
                        Triple("Home", Icons.Rounded.Home, "Home"),
                        Triple("Diary", Icons.Rounded.ReceiptLong, "Diary"),
                        Triple("Scan", Icons.Rounded.CameraAlt, "Scan"),
                        Triple("Workouts", Icons.Rounded.FitnessCenter, "Train"),
                        Triple("Settings", Icons.Rounded.Settings, "Settings")
                    )
                    destinations.forEach { (destination, image, label) ->
                        NavigationBarItem(
                            selected = tab == destination,
                            onClick = { tab = destination },
                            icon = { Icon(image, contentDescription = label) },
                            label = { Text(label, fontSize = 9.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    "Home" -> HomeScreen(entries, { tab = "Scan" }, { showAdd = true }, { tab = "Diary" })
                    "Diary" -> DiaryScreen(entries, { showAdd = true }, { entry ->
                        entries = entries.filterNot { it.id == entry.id }
                        saveEntries(context, entries)
                    })
                    "Scan" -> ScannerScreen(
                        photoUri, photoBitmap,
                        onCamera = { camera.launch(null) },
                        onGallery = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onRemove = { photoUri = null; photoBitmap = null }
                    )
                    "Workouts" -> EmptyScreen(
                        "Workout tracking",
                        "The workout catalogue is not part of this starter build. This tab is included to preview the navigation."
                    )
                    else -> SettingsScreen(themeChoice) { themeChoice = it }
                }
            }
        }

        if (showAdd) {
            ManualEntryDialog(
                onDismiss = { showAdd = false },
                onSave = { entry ->
                    entries = listOf(entry) + entries
                    saveEntries(context, entries)
                    showAdd = false
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    entries: List<FoodEntry>,
    onScan: () -> Unit,
    onAdd: () -> Unit,
    onDiary: () -> Unit
) {
    val totals = calculateTotals(entries)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("NUTRIGUIDE  /  DAILY OVERVIEW")
                Text("Good morning.", fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("A clearer view of your day.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        item {
            GlassCard {
                Eyebrow("DAILY ENERGY")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(totals.calories.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(7.dp))
                    Text("kcal logged", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 5.dp))
                }
                Text("Add foods to build your daily total.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        item {
            Text("Macros", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroCard("PROTEIN", totals.protein, Modifier.weight(1f))
                MacroCard("CARBS", totals.carbs, Modifier.weight(1f))
                MacroCard("FAT", totals.fat, Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your diary", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onDiary) { Text("VIEW ALL") }
            }
        }
        if (entries.isEmpty()) {
            item {
                GlassCard {
                    Text("Nothing logged yet", fontWeight = FontWeight.SemiBold)
                    Text("Scan a meal or add food manually to start your diary.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else {
            items(entries.take(3), key = { it.id }) { entry -> FoodRow(entry) }
        }
        item {
            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan a meal")
            }
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add food manually")
            }
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 1.1.sp)
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}

@Composable
private fun MacroCard(label: String, value: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, letterSpacing = 0.6.sp)
            Text("${value.pretty()} g", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FoodRow(entry: FoodEntry, onDelete: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) { Text(entry.name.take(1).uppercase(), fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("${entry.calories} kcal · P ${entry.protein.pretty()}  C ${entry.carbs.pretty()}  F ${entry.fat.pretty()} g",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
            if (onDelete != null) TextButton(onClick = onDelete) { Text("Remove", fontSize = 10.sp) }
        }
    }
}

@Composable
private fun DiaryScreen(entries: List<FoodEntry>, onAdd: () -> Unit, onDelete: (FoodEntry) -> Unit) {
    val totals = calculateTotals(entries)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Eyebrow("FOOD DIARY")
            Text("Today", fontSize = 29.sp, fontWeight = FontWeight.Bold)
        }
        item {
            GlassCard {
                Text("${totals.calories} kcal", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("P ${totals.protein.pretty()} g   C ${totals.carbs.pretty()} g   F ${totals.fat.pretty()} g",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        item {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add food manually")
            }
        }
        if (entries.isEmpty()) {
            item { GlassCard { Text("Your diary is empty"); Text("Saved foods will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }
        } else {
            items(entries, key = { it.id }) { entry -> FoodRow(entry) { onDelete(entry) } }
        }
    }
}

@Composable
private fun ScannerScreen(
    selectedUri: Uri?,
    selectedBitmap: Bitmap?,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val preview by produceState<Bitmap?>(selectedBitmap, selectedUri, selectedBitmap) {
        value = selectedBitmap
        if (selectedBitmap == null && selectedUri != null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(selectedUri)?.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    val sample = max(1, max(bounds.outWidth / 1400, bounds.outHeight / 1400))
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(selectedUri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                }.getOrNull()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Eyebrow("NUTRIGUIDE  /  VISION")
            Text("Food scanner", fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Choose a photo or capture your meal.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(preview!!.asImageBitmap(), "Selected food photograph", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Your meal photo appears here", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCamera, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("Camera")
                }
                OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("Gallery")
                }
            }
        }
        if (preview != null) {
            item {
                OutlinedButton(onClick = onRemove, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Remove / replace photo")
                }
            }
        }
        item {
            GlassCard {
                Text("SCANNING SERVICE UNAVAILABLE", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                Text(
                    "This preview can capture and display photos, but AI food recognition is not connected. No results will be invented or shown as real analysis.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                )
                Text("Manual food logging is available in the Diary tab.", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EmptyScreen(title: String, detail: String) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        GlassCard {
            Text("Not included in this preview", fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SettingsScreen(themeChoice: String, onThemeChoice: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", fontSize = 29.sp, fontWeight = FontWeight.Bold)
        GlassCard {
            Eyebrow("APPEARANCE")
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onThemeChoice(key) }.padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, modifier = Modifier.weight(1f))
                    Text(if (themeChoice == key) "SELECTED" else "", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
        GlassCard {
            Text("NutriGuide preview", fontWeight = FontWeight.SemiBold)
            Text("Manual entries are stored on this device. Photo recognition requires a secure backend and provider credentials.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ManualEntryDialog(onDismiss: () -> Unit, onSave: (FoodEntry) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Food name") }, singleLine = true)
                OutlinedTextField(calories, { calories = it }, label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedTextField(protein, { protein = it }, Modifier.weight(1f), label = { Text("Protein") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(carbs, { carbs = it }, Modifier.weight(1f), label = { Text("Carbs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(fat, { fat = it }, Modifier.weight(1f), label = { Text("Fat") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && (calories.toIntOrNull() ?: -1) >= 0) {
                        onSave(FoodEntry(
                            System.currentTimeMillis(), name.trim(), calories.toInt(),
                            protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0
                        ))
                    }
                },
                enabled = name.isNotBlank() && (calories.toIntOrNull() ?: -1) >= 0
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Double.pretty(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else String.format("%.1f", this)
