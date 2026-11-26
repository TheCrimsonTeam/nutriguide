package com.thecrimsonteam.nutriguide.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun IntervalTimerScreen(onBack: () -> Unit) {
    var warmupInput by rememberSaveable { mutableStateOf("0") }
    var workInput by rememberSaveable { mutableStateOf("30") }
    var restInput by rememberSaveable { mutableStateOf("30") }
    var roundsInput by rememberSaveable { mutableStateOf("6") }

    var plannedWarmup by rememberSaveable { mutableIntStateOf(0) }
    var plannedWork by rememberSaveable { mutableIntStateOf(30) }
    var plannedRest by rememberSaveable { mutableIntStateOf(30) }
    var plannedRounds by rememberSaveable { mutableIntStateOf(6) }
    var round by rememberSaveable { mutableIntStateOf(1) }
    var phase by rememberSaveable { mutableStateOf("READY") }
    var running by rememberSaveable { mutableStateOf(false) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var remaining by rememberSaveable { mutableIntStateOf(0) }
    var remainingAtPause by rememberSaveable { mutableIntStateOf(0) }
    var deadlineElapsedRealtime by rememberSaveable { mutableLongStateOf(0L) }

    fun beginPhase(label: String, seconds: Int) {
        phase = label
        remaining = seconds
        remainingAtPause = seconds
        deadlineElapsedRealtime = SystemClock.elapsedRealtime() + seconds * 1000L
        running = true
        paused = false
    }

    fun advancePhase() {
        when (phase) {
            "WARM UP" -> beginPhase("WORK", plannedWork)
            "WORK" -> {
                if (round >= plannedRounds) {
                    phase = "COMPLETE"
                    remaining = 0
                    remainingAtPause = 0
                    running = false
                    paused = false
                } else {
                    beginPhase("REST", plannedRest)
                }
            }
            "REST" -> {
                round += 1
                beginPhase("WORK", plannedWork)
            }
        }
    }

    fun startPlan() {
        val warmup = warmupInput.toIntOrNull()?.coerceIn(0, 600) ?: 0
        val work = workInput.toIntOrNull()?.coerceIn(5, 3600) ?: 30
        val rest = restInput.toIntOrNull()?.coerceIn(0, 3600) ?: 30
        val rounds = roundsInput.toIntOrNull()?.coerceIn(1, 99) ?: 6
        plannedWarmup = warmup
        plannedWork = work
        plannedRest = rest
        plannedRounds = rounds
        round = 1
        if (warmup > 0) beginPhase("WARM UP", warmup) else beginPhase("WORK", work)
    }

    fun resetPlan() {
        running = false
        paused = false
        phase = "READY"
        round = 1
        remaining = 0
        remainingAtPause = 0
    }

    LaunchedEffect(running, paused, phase, deadlineElapsedRealtime) {
        while (running && !paused && phase != "READY" && phase != "COMPLETE") {
            val millisLeft = deadlineElapsedRealtime - SystemClock.elapsedRealtime()
            val secondsLeft = ((millisLeft + 999L) / 1000L).toInt().coerceAtLeast(0)
            remaining = secondsLeft
            remainingAtPause = secondsLeft
            if (secondsLeft == 0) {
                advancePhase()
                break
            }
            delay(200)
        }
    }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("INTERVAL TIMER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Work. Recover. Repeat.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (phase == "READY") "READY" else if (phase == "COMPLETE") "COMPLETE" else "$phase · ROUND $round / $plannedRounds",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "%02d:%02d".format(remaining / 60, remaining % 60),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when (phase) {
                        "WARM UP" -> "Optional preparation period"
                        "WORK" -> "Work interval"
                        "REST" -> "Recovery interval"
                        "COMPLETE" -> "Interval session finished"
                        else -> "Set your intervals below"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text("TIMER SETTINGS", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumericSetting("Warm-up sec", warmupInput, { warmupInput = it }, Modifier.weight(1f))
            NumericSetting("Work sec", workInput, { workInput = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumericSetting("Rest sec", restInput, { restInput = it }, Modifier.weight(1f))
            NumericSetting("Rounds", roundsInput, { roundsInput = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    when {
                        phase == "READY" || phase == "COMPLETE" -> startPlan()
                        paused -> {
                            deadlineElapsedRealtime = SystemClock.elapsedRealtime() + remainingAtPause * 1000L
                            running = true
                            paused = false
                        }
                        running -> {
                            remainingAtPause = remaining
                            paused = true
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(when {
                    phase == "READY" || phase == "COMPLETE" -> "Start"
                    paused -> "Resume"
                    else -> "Pause"
                })
            }
            OutlinedButton(
                onClick = { advancePhase() },
                enabled = running && phase != "COMPLETE",
                modifier = Modifier.weight(1f)
            ) { Text("Skip phase") }
            OutlinedButton(onClick = { resetPlan() }) { Text("Reset") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to workouts") }
        Text(
            "Intervals are adjustable. Choose comfortable work periods and recovery times; stop or change a session whenever needed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NumericSetting(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
