package com.thecrimsonteam.nutriguide.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thecrimsonteam.nutriguide.data.catalog.ExerciseDefinition
import com.thecrimsonteam.nutriguide.data.catalog.RoutineTemplateSelector
import com.thecrimsonteam.nutriguide.data.local.RoutineExerciseEntity
import com.thecrimsonteam.nutriguide.data.local.ScheduledWorkoutEntity
import com.thecrimsonteam.nutriguide.data.local.WorkoutRoutineEntity
import com.thecrimsonteam.nutriguide.domain.model.RoutineExercisePlan
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RoutinePlannerScreen(
    viewModel: AppViewModel,
    allExercises: List<ExerciseDefinition>,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf("ROUTINES") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var formOpen by rememberSaveable { mutableStateOf(false) }
    var routineName by rememberSaveable { mutableStateOf("") }
    var routineDescription by rememberSaveable { mutableStateOf("") }
    var exerciseQuery by rememberSaveable { mutableStateOf("") }
    val selectedPlans = remember { mutableStateListOf<RoutineExercisePlan>() }
    val today = remember { LocalDate.now() }
    val currentLocale = LocalLocale.current.platformLocale
    var selectedDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var selectedRoutineId by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(editingId) {
        val id = editingId
        if (id != null) {
            val routine = viewModel.getRoutine(id)
            if (routine != null) {
                routineName = routine.name
                routineDescription = routine.description
                selectedPlans.clear()
                viewModel.routineExercises(id).forEach { selectedPlans.add(it.toPlan()) }
            }
        }
    }
    LaunchedEffect(viewModel.routines) {
        if (selectedRoutineId.isBlank()) selectedRoutineId = viewModel.routines.firstOrNull()?.id.orEmpty()
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("TRAINING / PLANNER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Your plan.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        Text("Build flexible sessions from your exercise library, then place them into your week.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ROUTINES", "WEEKLY PLAN").forEach { value ->
                FilterChip(selected = tab == value, onClick = { tab = value }, label = { Text(value) })
            }
        }

        if (tab == "ROUTINES") {
            if (!formOpen) {
                Button(
                    onClick = {
                        editingId = null
                        routineName = ""
                        routineDescription = ""
                        exerciseQuery = ""
                        selectedPlans.clear()
                        formOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create a routine")
                }
                Text("STARTER TEMPLATES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RoutineTemplateSelector.templateNames.forEach { template ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                editingId = null
                                routineName = template
                                routineDescription = "A flexible $template starting point. Adjust movements and targets to suit your day."
                                selectedPlans.clear()
                                RoutineTemplateSelector.select(template, allExercises).forEach { selectedPlans.add(it.toRoutinePlan()) }
                                formOpen = true
                            },
                            label = { Text(template) }
                        )
                    }
                }
                Text("SAVED ROUTINES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (viewModel.routines.isEmpty()) {
                    PlannerEmptyCard("No saved routines", "Create one above or start with a catalogue-based template.")
                } else {
                    viewModel.routines.forEach { routine ->
                        RoutineSummaryCard(
                            routine = routine,
                            onStart = { viewModel.startRoutine(routine.id); onStart() },
                            onEdit = { editingId = routine.id; formOpen = true },
                            onDelete = { viewModel.deleteRoutine(routine.id) }
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (editingId == null) "NEW ROUTINE" else "EDIT ROUTINE", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { formOpen = false; editingId = null; selectedPlans.clear() }) { Text("Cancel") }
                }
                OutlinedTextField(value = routineName, onValueChange = { routineName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Routine name") }, singleLine = true)
                OutlinedTextField(value = routineDescription, onValueChange = { routineDescription = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Description / notes") }, minLines = 2)
                Text("ADD FROM EXERCISE LIBRARY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = exerciseQuery, onValueChange = { exerciseQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search exercises, equipment or muscles") }, singleLine = true)
                val chosenIds = selectedPlans.map { it.exerciseId }.toSet()
                allExercises.asSequence()
                    .filter { it.id !in chosenIds }
                    .filter {
                        exerciseQuery.isBlank() ||
                            listOf(it.name, it.category, it.equipment, it.primaryMuscles, it.secondaryMuscles)
                                .any { value -> value.contains(exerciseQuery, ignoreCase = true) }
                    }
                    .take(10)
                    .forEach { exercise ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(exercise.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${exercise.category} · ${exercise.equipment}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { selectedPlans.add(exercise.toRoutinePlan()) }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Add")
                            }
                        }
                    }
                Text("ROUTINE SEQUENCE · ${selectedPlans.size}", style = MaterialTheme.typography.labelMedium)
                if (selectedPlans.isEmpty()) {
                    PlannerEmptyCard("Choose exercises", "Search the full catalogue above and add movements in the order you want.")
                } else {
                    selectedPlans.forEachIndexed { index, plan ->
                        RoutineExerciseEditor(
                            plan = plan,
                            index = index,
                            total = selectedPlans.size,
                            onChange = { selectedPlans[index] = it },
                            onRemove = { selectedPlans.removeAt(index) },
                            onMove = { offset ->
                                val target = index + offset
                                if (target in selectedPlans.indices) {
                                    val item = selectedPlans.removeAt(index)
                                    selectedPlans.add(target, item)
                                }
                            }
                        )
                    }
                }
                Button(
                    onClick = {
                        viewModel.saveRoutine(editingId, routineName, routineDescription, selectedPlans.toList())
                        formOpen = false
                        editingId = null
                        selectedPlans.clear()
                    },
                    enabled = routineName.isNotBlank() && selectedPlans.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save routine")
                }
            }
        } else {
            Text("WEEK AT A GLANCE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0L..6L).forEach { offset ->
                    val day = today.plusDays(offset)
                    FilterChip(selected = selectedDay == day.toEpochDay(), onClick = { selectedDay = day.toEpochDay() }, label = {
                        Text(day.format(DateTimeFormatter.ofPattern("EEE d", currentLocale)))
                    })
                }
            }
            Text("ADD TO SELECTED DAY", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                viewModel.routines.forEach { routine ->
                    FilterChip(selected = selectedRoutineId == routine.id, onClick = { selectedRoutineId = routine.id }, label = { Text(routine.name) })
                }
            }
            Button(
                onClick = { viewModel.scheduleRoutine(selectedRoutineId, selectedDay) },
                enabled = selectedRoutineId.isNotBlank() && viewModel.routines.any { it.id == selectedRoutineId },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Schedule routine")
            }
            Text("SCHEDULED SESSIONS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val sessions = viewModel.scheduledWorkouts.filter { it.scheduledDay == selectedDay }
            if (sessions.isEmpty()) {
                PlannerEmptyCard("Nothing scheduled", "Choose one of your routines above to add a session to this day.")
            } else {
                sessions.forEach { scheduled ->
                    val name = viewModel.routines.firstOrNull { it.id == scheduled.routineId }?.name ?: "Saved routine"
                    ScheduledWorkoutCard(
                        scheduled = scheduled,
                        name = name,
                        onStart = { viewModel.startScheduledWorkout(scheduled.id); onStart() },
                        onMove = { viewModel.moveScheduledWorkout(scheduled.id, it) },
                        onComplete = { viewModel.setScheduledStatus(scheduled.id, "COMPLETED") },
                        onRemove = { viewModel.removeScheduledWorkout(scheduled.id) }
                    )
                }
            }
            Text("Move sessions between days, mark them completed, or start one directly. Plans are stored on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RoutineSummaryCard(routine: WorkoutRoutineEntity, onStart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (routine.description.isNotBlank()) Text(routine.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Start")
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text("Edit")
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete routine") }
            }
        }
    }
}

@Composable
private fun RoutineExerciseEditor(
    plan: RoutineExercisePlan,
    index: Int,
    total: Int,
    onChange: (RoutineExercisePlan) -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${index + 1}. ${plan.exerciseName}", style = MaterialTheme.typography.titleSmall)
                    Text(plan.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onMove(-1) }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up") }
                IconButton(onClick = { onMove(1) }, enabled = index < total - 1) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down") }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove exercise") }
            }
            if (plan.trackingType == "CARDIO" || plan.trackingType == "DURATION") {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RoutineNumberField(plan.targetSets.toString(), { onChange(plan.copy(targetSets = it.toIntOrNull()?.coerceIn(1, 20) ?: plan.targetSets)) }, "Sets", Modifier.weight(1f))
                    RoutineNumberField(((plan.targetDurationSeconds ?: 600) / 60).toString(), { onChange(plan.copy(targetDurationSeconds = (it.toIntOrNull()?.coerceIn(1, 360) ?: 10) * 60)) }, "Minutes", Modifier.weight(1f))
                    if (plan.trackingType == "CARDIO") RoutineNumberField(((plan.targetDistanceMeters ?: 0.0) / 1000.0).toString(), { onChange(plan.copy(targetDistanceMeters = it.toDoubleOrNull()?.coerceAtLeast(0.0)?.times(1000.0))) }, "Distance km", Modifier.weight(1f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RoutineNumberField(plan.targetSets.toString(), { onChange(plan.copy(targetSets = it.toIntOrNull()?.coerceIn(1, 20) ?: plan.targetSets)) }, "Sets", Modifier.weight(1f))
                    RoutineNumberField(plan.targetReps.toString(), { onChange(plan.copy(targetReps = it.toIntOrNull()?.coerceIn(1, 100) ?: plan.targetReps)) }, "Reps", Modifier.weight(1f))
                    RoutineNumberField(plan.targetWeightKg?.toString().orEmpty(), { onChange(plan.copy(targetWeightKg = it.toDoubleOrNull()?.coerceAtLeast(0.0))) }, "Weight kg", Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                RoutineNumberField(plan.restSeconds.toString(), { onChange(plan.copy(restSeconds = it.toIntOrNull()?.coerceIn(0, 600) ?: plan.restSeconds)) }, "Rest sec", Modifier.weight(1f))
                OutlinedTextField(value = plan.notes, onValueChange = { onChange(plan.copy(notes = it)) }, label = { Text("Exercise notes") }, modifier = Modifier.weight(2f), singleLine = true)
            }
        }
    }
}

@Composable
private fun ScheduledWorkoutCard(
    scheduled: ScheduledWorkoutEntity,
    name: String,
    onStart: () -> Unit,
    onMove: (Long) -> Unit,
    onComplete: () -> Unit,
    onRemove: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(scheduled.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton(onClick = { onMove(-1L) }) { Text("− 1 day") }
                OutlinedButton(onClick = { onMove(1L) }) { Text("+ 1 day") }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, enabled = scheduled.status != "COMPLETED" && scheduled.status != "STARTED", modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Start")
                }
                OutlinedButton(onClick = onComplete, enabled = scheduled.status != "COMPLETED", modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text("Mark done")
                }
            }
        }
    }
}

@Composable
private fun PlannerEmptyCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RoutineNumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { char -> char.isDigit() || char == '.' }.take(6)) },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

private fun ExerciseDefinition.toRoutinePlan() = RoutineExercisePlan(
    exerciseId = id,
    exerciseName = name,
    category = category,
    trackingType = trackingType,
    targetSets = if (trackingType == "CARDIO" || trackingType == "DURATION") 1 else 3,
    targetReps = 8,
    restSeconds = 60,
    targetDurationSeconds = if (trackingType == "CARDIO" || trackingType == "DURATION") 600 else null
)

private fun RoutineExerciseEntity.toPlan() = RoutineExercisePlan(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    category = category,
    trackingType = trackingType,
    targetSets = targetSets,
    targetReps = targetReps,
    targetWeightKg = targetWeightKg,
    restSeconds = restSeconds,
    targetDurationSeconds = targetDurationSeconds,
    targetDistanceMeters = targetDistanceMeters,
    notes = notes
)
