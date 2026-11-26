package com.thecrimsonteam.nutriguide

import com.thecrimsonteam.nutriguide.data.catalog.ExerciseDefinition
import com.thecrimsonteam.nutriguide.data.catalog.RoutineTemplateSelector
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineTemplateSelectorTest {
    private fun exercise(name: String, muscles: String, equipment: String = "Bodyweight", category: String = "Strength") =
        ExerciseDefinition(
            id = name.lowercase().replace(" ", "-"),
            name = name,
            category = category,
            equipment = equipment,
            primaryMuscles = muscles,
            secondaryMuscles = "",
            difficulty = "General"
        )

    @Test fun starterTemplatesUseTheProvidedFullCatalogueAndReturnOnlyRelevantEntries() {
        val catalogue = listOf(
            exercise("Push-up", "Chest triceps"),
            exercise("Row", "Back biceps", "Dumbbell"),
            exercise("Squat", "Quadriceps glutes", "Barbell"),
            exercise("Hamstring stretch", "Hamstrings", category = "Mobility"),
            exercise("Jog", "Cardio", category = "Cardio")
        )
        assertTrue(RoutineTemplateSelector.select("Push", catalogue).any { it.name == "Push-up" })
        assertTrue(RoutineTemplateSelector.select("Pull", catalogue).any { it.name == "Row" })
        assertTrue(RoutineTemplateSelector.select("Legs", catalogue).any { it.name == "Squat" })
        assertTrue(RoutineTemplateSelector.select("Mobility", catalogue).any { it.name == "Hamstring stretch" })
        assertTrue(RoutineTemplateSelector.select("General Fitness", catalogue).any { it.name == "Jog" })
    }

    @Test fun unknownTemplateDoesNotPretendToMatchExercises() {
        assertTrue(RoutineTemplateSelector.select("Unlisted", listOf(exercise("Push-up", "Chest"))).isEmpty())
    }
}
