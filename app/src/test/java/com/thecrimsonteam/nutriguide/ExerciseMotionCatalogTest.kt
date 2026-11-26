package com.thecrimsonteam.nutriguide

import com.thecrimsonteam.nutriguide.data.catalog.ExerciseDefinition
import com.thecrimsonteam.nutriguide.ui.ExerciseMotionCatalog
import com.thecrimsonteam.nutriguide.ui.ExerciseMotionPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseMotionCatalogTest {
    private fun exercise(name: String, category: String = "Test") = ExerciseDefinition(
        id = name.lowercase().replace(" ", "-"),
        name = name,
        category = category,
        equipment = "Test equipment",
        primaryMuscles = "Multiple",
        secondaryMuscles = "",
        difficulty = "General"
    )

    @Test fun mapsDistinctCommonMovementFamilies() {
        assertEquals(ExerciseMotionPattern.SQUAT, ExerciseMotionCatalog.resolve(exercise("Barbell back squat")))
        assertEquals(ExerciseMotionPattern.HINGE, ExerciseMotionCatalog.resolve(exercise("Romanian deadlift")))
        assertEquals(ExerciseMotionPattern.LUNGE, ExerciseMotionCatalog.resolve(exercise("Walking lunge")))
        assertEquals(ExerciseMotionPattern.PUSH, ExerciseMotionCatalog.resolve(exercise("Push-up")))
        assertEquals(ExerciseMotionPattern.ROW, ExerciseMotionCatalog.resolve(exercise("Seated cable row")))
        assertEquals(ExerciseMotionPattern.CURL, ExerciseMotionCatalog.resolve(exercise("Dumbbell curl")))
        assertEquals(ExerciseMotionPattern.RAISE, ExerciseMotionCatalog.resolve(exercise("Lateral raise")))
        assertEquals(ExerciseMotionPattern.CORE, ExerciseMotionCatalog.resolve(exercise("Plank")))
        assertEquals(ExerciseMotionPattern.CARDIO, ExerciseMotionCatalog.resolve(exercise("Jumping jack")))
        assertEquals(ExerciseMotionPattern.MOBILITY, ExerciseMotionCatalog.resolve(exercise("Hip mobility drill")))
    }

    @Test fun doesNotShowUnrelatedDemoForUnknownMovement() {
        assertNull(ExerciseMotionCatalog.resolve(exercise("Mystery movement")))
    }
}
