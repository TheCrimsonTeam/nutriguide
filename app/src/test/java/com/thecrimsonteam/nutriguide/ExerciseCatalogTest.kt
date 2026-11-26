package com.thecrimsonteam.nutriguide

import com.thecrimsonteam.nutriguide.data.catalog.ExerciseCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {
    @Test
    fun catalogueCoversCoreEquipmentAndMovementGroups() {
        val categories = ExerciseCatalog.categories.toSet()
        assertTrue(categories.contains("Dumbbells"))
        assertTrue(categories.contains("Barbells"))
        assertTrue(categories.contains("Calisthenics"))
        assertTrue(categories.contains("Cardio"))
        assertTrue(categories.contains("Mobility & flexibility"))
        assertTrue(ExerciseCatalog.all.size >= 100)
    }

    @Test
    fun searchMatchesExerciseNamesAndMuscles() {
        assertTrue(ExerciseCatalog.search("lat pulldown").isNotEmpty())
        assertTrue(ExerciseCatalog.search("chest").isNotEmpty())
        assertTrue(ExerciseCatalog.search("kettlebell", category = "Kettlebells").isNotEmpty())
    }
}
