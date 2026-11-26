package com.thecrimsonteam.nutriguide.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thecrimsonteam.nutriguide.data.catalog.ExerciseDefinition
import kotlin.math.sin

enum class ExerciseMotionPattern {
    SQUAT, HINGE, LUNGE, PUSH, PULL, ROW, CURL, RAISE, EXTENSION, CORE, CARDIO, MOBILITY, HOLD
}

/**
 * Resolves an exercise to an explicit movement family. Unknown movements deliberately
 * return null rather than showing an unrelated animation.
 */
object ExerciseMotionCatalog {
    fun resolve(exercise: ExerciseDefinition): ExerciseMotionPattern? {
        val name = exercise.name.lowercase()
        val category = exercise.category.lowercase()
        return when {
            listOf("pistol squat", "squat", "leg press", "hack squat", "wall sit").any(name::contains) ->
                ExerciseMotionPattern.SQUAT
            listOf("lunge", "split squat", "step-up", "step up", "bulgarian").any(name::contains) ->
                ExerciseMotionPattern.LUNGE
            listOf("deadlift", "romanian", "good morning", "hip thrust", "pull-through", "hip hinge").any(name::contains) ->
                ExerciseMotionPattern.HINGE
            listOf("push-up", "push up", "bench press", "chest press", "dip", "overhead press", "shoulder press", "chest fly").any(name::contains) ->
                ExerciseMotionPattern.PUSH
            listOf("pull-up", "pull up", "chin-up", "chin up", "pulldown").any(name::contains) ->
                ExerciseMotionPattern.PULL
            name.contains("row") -> ExerciseMotionPattern.ROW
            name.contains("curl") -> ExerciseMotionPattern.CURL
            listOf("lateral raise", "front raise", "rear delt", "fly", "face pull", "steering wheel").any(name::contains) ->
                ExerciseMotionPattern.RAISE
            listOf("extension", "pushdown", "skull crusher", "kickback").any(name::contains) ->
                ExerciseMotionPattern.EXTENSION
            listOf("plank", "crunch", "twist", "dead bug", "bird dog", "hollow", "l-sit", "leg raise").any(name::contains) ->
                ExerciseMotionPattern.CORE
            listOf("run", "walk", "jog", "cycling", "bike", "jump", "climber", "jack", "high knees", "rope").any(name::contains) ->
                ExerciseMotionPattern.CARDIO
            listOf("stretch", "mobility", "rotation", "warm-up", "warmup", "cat-cow", "foam roll").any(name::contains) ||
                listOf("mobility", "stretch", "warm-up").any(category::contains) ->
                ExerciseMotionPattern.MOBILITY
            listOf("hold", "carry", "shrug", "isometric").any(name::contains) ->
                ExerciseMotionPattern.HOLD
            category.contains("cardio") -> ExerciseMotionPattern.CARDIO
            else -> null
        }
    }
}

@Composable
fun ExerciseDemonstration(exercise: ExerciseDefinition) {
    val pattern = remember(exercise.id) { ExerciseMotionCatalog.resolve(exercise) }
    if (pattern == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("DEMONSTRATION NOT AVAILABLE", style = MaterialTheme.typography.labelMedium)
                Text(
                    "A verified movement visual has not been mapped for this exercise yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
    var playing by rememberSaveable(exercise.id) { mutableStateOf(!reducedMotion) }
    var replayIndex by rememberSaveable(exercise.id) { mutableStateOf(0) }
    val progress = remember(exercise.id) { Animatable(0f) }

    var lastReplayIndex by remember(exercise.id) { mutableStateOf(0) }
    LaunchedEffect(playing, replayIndex, reducedMotion) {
        if (playing && !reducedMotion) {
            if (lastReplayIndex != replayIndex) {
                progress.snapTo(0f)
                lastReplayIndex = replayIndex
            }
            while (true) {
                progress.animateTo(
                    1f,
                    animationSpec = tween(1300, easing = FastOutSlowInEasing)
                )
                progress.animateTo(
                    0f,
                    animationSpec = tween(1300, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF101010))
        ) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(224.dp)
                    .semantics {
                        contentDescription = "Monochrome animated demonstration of ${exercise.name}"
                    }
            ) {
                drawExercisePose(pattern, progress.value, exercise.equipment)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(pattern.name.lowercase().replace('_', ' '), style = MaterialTheme.typography.labelMedium)
                Text(
                    if (reducedMotion) "Motion reduced by system setting" else "2.6-second movement loop",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                TextButton(
                    onClick = { playing = !playing },
                    enabled = !reducedMotion
                ) { Text(if (playing) "Pause" else "Play") }
                TextButton(
                    onClick = { replayIndex += 1; playing = !reducedMotion },
                    enabled = !reducedMotion
                ) { Text("Replay") }
            }
        }
    }
}

private fun DrawScope.drawExercisePose(pattern: ExerciseMotionPattern, rawProgress: Float, equipment: String) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val p = rawProgress.coerceIn(0f, 1f)
    val pulse = sin(p * Math.PI).toFloat()
    val stroke = (size.minDimension * 0.026f).coerceAtLeast(4f)
    val ink = Color(0xFFF4F4F4)
    val muted = Color(0xFF777777)
    val equipmentLabel = equipment.lowercase()
    val hasBarbellLikeEquipment = listOf("barbell", "smith", "plate-loaded", "cable", "machine").any(equipmentLabel::contains)
    val hasHandWeights = listOf("dumbbell", "kettlebell", "weight plate").any(equipmentLabel::contains)

    drawLine(muted, Offset(w * 0.12f, h * 0.91f), Offset(w * 0.88f, h * 0.91f), strokeWidth = stroke * 0.45f)
    fun segment(a: Offset, b: Offset, width: Float = stroke) {
        drawLine(ink, a, b, strokeWidth = width, cap = StrokeCap.Round)
    }
    fun joint(point: Offset, radius: Float = stroke * 0.65f) {
        drawCircle(ink, radius, point)
    }

    var head = Offset(cx, h * 0.20f)
    var shoulder = Offset(cx, h * 0.35f)
    var hip = Offset(cx, h * 0.58f)
    var elbowL = Offset(cx - w * 0.15f, h * 0.46f)
    var elbowR = Offset(cx + w * 0.15f, h * 0.46f)
    var wristL = Offset(cx - w * 0.18f, h * 0.58f)
    var wristR = Offset(cx + w * 0.18f, h * 0.58f)
    var kneeL = Offset(cx - w * 0.09f, h * 0.74f)
    var kneeR = Offset(cx + w * 0.09f, h * 0.74f)
    var ankleL = Offset(cx - w * 0.11f, h * 0.89f)
    var ankleR = Offset(cx + w * 0.11f, h * 0.89f)
    var shoulderL = Offset(cx - w * 0.045f, shoulder.y)
    var shoulderR = Offset(cx + w * 0.045f, shoulder.y)
    var hipL = Offset(cx - w * 0.04f, hip.y)
    var hipR = Offset(cx + w * 0.04f, hip.y)
    var equipmentBar: Offset? = null

    when (pattern) {
        ExerciseMotionPattern.SQUAT -> {
            hip = Offset(cx, h * (0.58f + 0.09f * pulse))
            shoulder = Offset(cx, hip.y - h * 0.24f)
            head = Offset(cx, shoulder.y - h * 0.14f)
            kneeL = Offset(cx - w * 0.16f, h * (0.73f + 0.02f * pulse))
            kneeR = Offset(cx + w * 0.16f, h * (0.73f + 0.02f * pulse))
            ankleL = Offset(cx - w * 0.18f, h * 0.89f)
            ankleR = Offset(cx + w * 0.18f, h * 0.89f)
            elbowL = Offset(cx - w * 0.20f, shoulder.y + h * 0.01f)
            elbowR = Offset(cx + w * 0.20f, shoulder.y + h * 0.01f)
            wristL = Offset(cx - w * 0.29f, shoulder.y - h * 0.01f)
            wristR = Offset(cx + w * 0.29f, shoulder.y - h * 0.01f)
            equipmentBar = if (hasBarbellLikeEquipment) Offset(cx, shoulder.y - h * 0.04f) else null
        }
        ExerciseMotionPattern.HINGE -> {
            hip = Offset(cx + w * 0.025f, h * 0.59f)
            shoulder = Offset(cx - w * (0.07f + 0.07f * pulse), h * (0.37f + 0.08f * pulse))
            head = Offset(shoulder.x, shoulder.y - h * 0.14f)
            elbowL = Offset(cx - w * 0.13f, h * 0.51f)
            elbowR = Offset(cx + w * 0.01f, h * 0.51f)
            wristL = Offset(cx - w * 0.14f, h * (0.72f + 0.03f * pulse))
            wristR = Offset(cx + w * 0.01f, h * (0.72f + 0.03f * pulse))
            kneeL = Offset(cx - w * 0.10f, h * 0.74f)
            kneeR = Offset(cx + w * 0.10f, h * 0.74f)
            equipmentBar = if (hasBarbellLikeEquipment) Offset(cx - w * 0.06f, h * 0.79f) else null
        }
        ExerciseMotionPattern.LUNGE -> {
            hip = Offset(cx, h * (0.57f + 0.045f * pulse))
            shoulder = Offset(cx, hip.y - h * 0.24f)
            head = Offset(cx, shoulder.y - h * 0.14f)
            kneeL = Offset(cx - w * 0.20f, h * 0.78f)
            kneeR = Offset(cx + w * 0.16f, h * 0.70f)
            ankleL = Offset(cx - w * 0.33f, h * 0.89f)
            ankleR = Offset(cx + w * 0.28f, h * 0.89f)
            elbowL = Offset(cx - w * 0.14f, h * 0.46f)
            elbowR = Offset(cx + w * 0.14f, h * 0.46f)
            wristL = Offset(cx - w * 0.15f, h * 0.61f)
            wristR = Offset(cx + w * 0.15f, h * 0.61f)
        }
        ExerciseMotionPattern.PUSH -> {
            head = Offset(w * 0.17f, h * 0.53f)
            shoulder = Offset(w * 0.28f, h * 0.57f)
            hip = Offset(w * 0.56f, h * 0.61f)
            kneeL = Offset(w * 0.72f, h * 0.72f)
            kneeR = Offset(w * 0.76f, h * 0.72f)
            ankleL = Offset(w * 0.89f, h * 0.76f)
            ankleR = Offset(w * 0.91f, h * 0.76f)
            elbowL = Offset(w * 0.34f, h * (0.68f - 0.07f * pulse))
            elbowR = Offset(w * 0.38f, h * (0.68f - 0.07f * pulse))
            wristL = Offset(w * 0.39f, h * 0.77f)
            wristR = Offset(w * 0.43f, h * 0.77f)
            shoulderL = Offset(w * 0.26f, shoulder.y)
            shoulderR = Offset(w * 0.30f, shoulder.y)
            hipL = Offset(w * 0.55f, hip.y)
            hipR = Offset(w * 0.58f, hip.y)
        }
        ExerciseMotionPattern.PULL, ExerciseMotionPattern.ROW -> {
            val lean = if (pattern == ExerciseMotionPattern.ROW) w * 0.08f else 0f
            shoulder = Offset(cx - lean, h * 0.34f)
            hip = Offset(cx + lean * 0.6f, h * 0.60f)
            head = Offset(shoulder.x, shoulder.y - h * 0.14f)
            elbowL = Offset(cx - w * (0.17f - 0.07f * pulse), h * (0.48f - 0.02f * pulse))
            elbowR = Offset(cx + w * (0.17f - 0.07f * pulse), h * (0.48f - 0.02f * pulse))
            wristL = Offset(cx - w * (0.20f - 0.10f * pulse), h * (0.58f - 0.10f * pulse))
            wristR = Offset(cx + w * (0.20f - 0.10f * pulse), h * (0.58f - 0.10f * pulse))
            if (pattern == ExerciseMotionPattern.ROW && hasBarbellLikeEquipment) equipmentBar = Offset(cx, h * 0.55f)
        }
        ExerciseMotionPattern.CURL -> {
            elbowL = Offset(cx - w * 0.12f, h * 0.52f)
            elbowR = Offset(cx + w * 0.12f, h * 0.52f)
            wristL = Offset(cx - w * 0.14f, h * (0.61f - 0.15f * pulse))
            wristR = Offset(cx + w * 0.14f, h * (0.61f - 0.15f * pulse))
        }
        ExerciseMotionPattern.RAISE -> {
            elbowL = Offset(cx - w * (0.13f + 0.12f * pulse), h * (0.43f - 0.13f * pulse))
            elbowR = Offset(cx + w * (0.13f + 0.12f * pulse), h * (0.43f - 0.13f * pulse))
            wristL = Offset(cx - w * (0.18f + 0.22f * pulse), h * (0.54f - 0.18f * pulse))
            wristR = Offset(cx + w * (0.18f + 0.22f * pulse), h * (0.54f - 0.18f * pulse))
        }
        ExerciseMotionPattern.EXTENSION -> {
            elbowL = Offset(cx - w * 0.12f, h * 0.34f)
            elbowR = Offset(cx + w * 0.12f, h * 0.34f)
            wristL = Offset(cx - w * 0.12f, h * (0.20f + 0.13f * pulse))
            wristR = Offset(cx + w * 0.12f, h * (0.20f + 0.13f * pulse))
        }
        ExerciseMotionPattern.CORE -> {
            shoulder = Offset(cx - w * 0.03f * pulse, h * (0.35f + 0.10f * pulse))
            head = Offset(shoulder.x, shoulder.y - h * 0.13f)
            hip = Offset(cx, h * 0.61f)
            kneeL = Offset(cx - w * 0.20f, h * 0.71f)
            kneeR = Offset(cx + w * 0.20f, h * 0.71f)
            elbowL = Offset(cx - w * 0.18f, h * (0.42f + 0.07f * pulse))
            elbowR = Offset(cx + w * 0.18f, h * (0.42f + 0.07f * pulse))
            wristL = Offset(cx - w * 0.25f, h * 0.50f)
            wristR = Offset(cx + w * 0.25f, h * 0.50f)
        }
        ExerciseMotionPattern.CARDIO -> {
            val lift = pulse * h * 0.16f
            kneeL = Offset(cx - w * 0.16f, h * 0.75f - lift)
            kneeR = Offset(cx + w * 0.16f, h * 0.75f - (h * 0.16f - lift))
            ankleL = Offset(cx - w * 0.24f, h * 0.89f - lift * 0.75f)
            ankleR = Offset(cx + w * 0.24f, h * 0.89f - (h * 0.12f - lift * 0.75f))
            elbowL = Offset(cx - w * 0.15f, h * 0.42f)
            elbowR = Offset(cx + w * 0.15f, h * 0.42f)
            wristL = Offset(cx - w * 0.20f, h * (0.32f + 0.10f * pulse))
            wristR = Offset(cx + w * 0.20f, h * (0.42f - 0.10f * pulse))
        }
        ExerciseMotionPattern.MOBILITY -> {
            elbowL = Offset(cx - w * 0.18f, h * (0.28f - 0.07f * pulse))
            elbowR = Offset(cx + w * 0.18f, h * (0.28f - 0.07f * pulse))
            wristL = Offset(cx - w * 0.14f, h * (0.18f - 0.08f * pulse))
            wristR = Offset(cx + w * 0.14f, h * (0.18f - 0.08f * pulse))
            shoulder = Offset(cx, h * (0.35f + 0.02f * pulse))
            head = Offset(cx, shoulder.y - h * 0.14f)
        }
        ExerciseMotionPattern.HOLD -> {
            wristL = Offset(cx - w * 0.20f, h * 0.59f)
            wristR = Offset(cx + w * 0.20f, h * 0.59f)
        }
    }

    shoulderL = Offset(shoulder.x - w * 0.045f, shoulder.y)
    shoulderR = Offset(shoulder.x + w * 0.045f, shoulder.y)
    hipL = Offset(hip.x - w * 0.04f, hip.y)
    hipR = Offset(hip.x + w * 0.04f, hip.y)

    // Torso and head.
    segment(shoulder, hip, stroke * 1.15f)
    drawCircle(ink, size.minDimension * 0.043f, head)
    // Arms.
    segment(shoulderL, elbowL)
    segment(elbowL, wristL)
    segment(shoulderR, elbowR)
    segment(elbowR, wristR)
    // Legs.
    segment(hipL, kneeL)
    segment(kneeL, ankleL)
    segment(hipR, kneeR)
    segment(kneeR, ankleR)
    // Joints.
    listOf(shoulderL, shoulderR, elbowL, elbowR, hipL, hipR, kneeL, kneeR).forEach(::joint)

    // A minimal bar/weight cue where the selected equipment implies one.
    equipmentBar?.let { bar ->
        segment(Offset(bar.x - w * 0.24f, bar.y), Offset(bar.x + w * 0.24f, bar.y), stroke * 0.72f)
        drawCircle(ink, stroke * 1.15f, Offset(bar.x - w * 0.25f, bar.y))
        drawCircle(ink, stroke * 1.15f, Offset(bar.x + w * 0.25f, bar.y))
    }
    if (hasHandWeights) {
        listOf(wristL, wristR).forEach { hand ->
            drawCircle(ink, stroke * 1.4f, hand)
            drawLine(
                muted,
                Offset(hand.x - stroke * 1.8f, hand.y),
                Offset(hand.x + stroke * 1.8f, hand.y),
                strokeWidth = stroke * 0.65f,
                cap = StrokeCap.Round
            )
        }
    }
}
