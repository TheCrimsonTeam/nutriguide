package com.thecrimsonteam.nutriguide.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun WorkoutRestTimer(startTrigger: Int, requestedSeconds: Int = 60) {
    var configuredSeconds by rememberSaveable { mutableIntStateOf(60) }
    var customSeconds by rememberSaveable { mutableStateOf("") }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(60) }
    var remainingAtPause by rememberSaveable { mutableIntStateOf(60) }
    var deadlineElapsedRealtime by rememberSaveable { mutableLongStateOf(0L) }
    var running by rememberSaveable { mutableStateOf(false) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }

    fun start(seconds: Int) {
        configuredSeconds = seconds.coerceIn(5, 900)
        remainingSeconds = configuredSeconds
        remainingAtPause = configuredSeconds
        deadlineElapsedRealtime = SystemClock.elapsedRealtime() + configuredSeconds * 1000L
        running = true
        paused = false
        completed = false
    }

    LaunchedEffect(startTrigger, requestedSeconds) {
        if (startTrigger > 0) start(requestedSeconds)
    }
    LaunchedEffect(running, paused, deadlineElapsedRealtime) {
        while (running && !paused) {
            val millisLeft = deadlineElapsedRealtime - SystemClock.elapsedRealtime()
            val secondsLeft = ((millisLeft + 999L) / 1000L).toInt().coerceAtLeast(0)
            remainingSeconds = secondsLeft
            remainingAtPause = secondsLeft
            if (secondsLeft == 0) {
                running = false
                paused = false
                completed = true
                break
            }
            delay(250)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("REST TIMER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(30, 45, 60, 90, 120).forEach { seconds ->
                    FilterChip(
                        selected = configuredSeconds == seconds,
                        onClick = {
                            configuredSeconds = seconds
                            remainingSeconds = seconds
                            remainingAtPause = seconds
                            running = false
                            paused = false
                            completed = false
                        },
                        label = { Text(seconds.toString()) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customSeconds,
                    onValueChange = { customSeconds = it.filter(Char::isDigit).take(3) },
                    label = { Text("Custom sec") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedButton(
                    onClick = {
                        val value = customSeconds.toIntOrNull()
                        if (value != null && value in 5..900) {
                            configuredSeconds = value
                            remainingSeconds = value
                            remainingAtPause = value
                            running = false
                            paused = false
                            completed = false
                        }
                    }
                ) { Text("Apply") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        when {
                            !running -> start(remainingSeconds.takeIf { it > 0 } ?: configuredSeconds)
                            paused -> {
                                deadlineElapsedRealtime = SystemClock.elapsedRealtime() + remainingAtPause * 1000L
                                paused = false
                            }
                            else -> {
                                remainingAtPause = remainingSeconds
                                paused = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(when { !running -> "Start"; paused -> "Resume"; else -> "Pause" })
                }
                OutlinedButton(onClick = {
                    if (running && !paused) {
                        deadlineElapsedRealtime += 15_000L
                    } else {
                        remainingAtPause += 15
                        remainingSeconds = remainingAtPause
                    }
                    completed = false
                }) { Text("+15s") }
                OutlinedButton(onClick = {
                    running = false
                    paused = false
                    remainingAtPause = configuredSeconds
                    remainingSeconds = configuredSeconds
                    completed = false
                }) { Text("Reset") }
            }
            if (completed) {
                Text("Rest complete", style = MaterialTheme.typography.labelLarge)
            } else if (running && !paused) {
                Text("Running · switch to your next set when ready", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (paused) {
                Text("Paused", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
