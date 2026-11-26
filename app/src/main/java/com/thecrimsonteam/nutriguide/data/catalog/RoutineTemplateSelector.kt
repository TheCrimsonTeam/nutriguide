package com.thecrimsonteam.nutriguide.data.catalog

object RoutineTemplateSelector {
    val templateNames = listOf(
        "Full Body", "Upper Body", "Lower Body", "Push", "Pull",
        "Legs", "Bodyweight", "General Fitness", "Mobility"
    )

    fun select(template: String, catalogue: List<ExerciseDefinition>): List<ExerciseDefinition> {
        val mode = template.lowercase()
        fun score(exercise: ExerciseDefinition): Int {
            val name = exercise.name.lowercase()
            val category = exercise.category.lowercase()
            val equipment = exercise.equipment.lowercase()
            val muscles = (exercise.primaryMuscles + " " + exercise.secondaryMuscles).lowercase()
            return when (mode) {
                "full body" -> listOf("chest", "back", "quad", "leg", "glute", "hamstring", "shoulder", "core")
                    .count { muscles.contains(it) } * 2
                "upper body" -> listOf("chest", "back", "shoulder", "bicep", "tricep", "arm", "lat")
                    .count { muscles.contains(it) } * 2
                "lower body", "legs" -> listOf("leg", "quad", "glute", "hamstring", "calf")
                    .count { muscles.contains(it) } * 2
                "push" -> listOf("chest", "shoulder", "tricep")
                    .count { muscles.contains(it) } * 2
                "pull" -> listOf("back", "bicep", "lat", "trap", "forearm")
                    .count { muscles.contains(it) } * 2
                "bodyweight" -> if (
                    equipment.contains("bodyweight") || equipment.contains("none") ||
                    equipment.contains("no equipment") || equipment.isBlank()
                ) 5 else 0
                "general fitness" -> (
                    if (category.contains("cardio") || category.contains("mobility")) 4 else 0
                ) + if (listOf("walk", "cycle", "bike", "run", "mobility").any(name::contains)) 2 else 0
                "mobility" -> if (
                    category.contains("mobility") || category.contains("stretch") ||
                    listOf("stretch", "mobility", "rotation", "warm-up", "warmup", "foam roll").any(name::contains)
                ) 5 else 0
                else -> 0
            }
        }
        return catalogue.asSequence()
            .map { it to score(it) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<ExerciseDefinition, Int>> { it.second }.thenBy { it.first.name })
            .map { it.first }
            .distinctBy { it.id }
            .take(6)
            .toList()
    }
}
