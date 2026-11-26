package com.thecrimsonteam.nutriguide.data.catalog

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val category: String,
    val equipment: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val difficulty: String,
    val trackingType: String = "STRENGTH",
    val aliases: List<String> = emptyList(),
    val instructions: String = "Use the equipment's setup guidance and work within a comfortable, controlled range. Ask a qualified coach for technique help when needed."
)

object ExerciseCatalog {
    private fun slug(value: String) = value.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

    private fun group(
        category: String,
        equipment: String,
        primary: String,
        secondary: String,
        names: String,
        tracking: String = "STRENGTH"
    ): List<ExerciseDefinition> = names.split("|").map { it.trim() }.filter { it.isNotEmpty() }.map { name ->
        ExerciseDefinition(
            id = slug(name),
            name = name,
            category = category,
            equipment = equipment,
            primaryMuscles = primary,
            secondaryMuscles = secondary,
            difficulty = if (name.contains("one-arm", true) || name.contains("pistol", true) || name.contains("muscle-up", true)) "Advanced" else "General",
            trackingType = tracking,
            aliases = listOf(name.replace("Dumbbell", "DB").replace("Barbell", "BB"))
        )
    }

    val all: List<ExerciseDefinition> = buildList {
        addAll(group("Dumbbells", "Dumbbell", "Chest", "Shoulders, triceps", "Dumbbell bench press|Incline dumbbell press|Dumbbell floor press|Dumbbell fly|Dumbbell pullover|Dumbbell shoulder press|Arnold press|Dumbbell lateral raise|Dumbbell front raise|Dumbbell rear delt fly|Dumbbell row|One-arm dumbbell row|Chest-supported dumbbell row|Dumbbell shrug|Dumbbell biceps curl|Hammer curl|Incline dumbbell curl|Concentration curl|Dumbbell goblet squat|Dumbbell Romanian deadlift|Dumbbell lunge|Dumbbell step-up"))
        addAll(group("Barbells", "Barbell", "Full body", "Core, stabilisers", "Barbell back squat|Barbell front squat|Barbell overhead squat|Barbell bench press|Close-grip bench press|Incline barbell press|Barbell overhead press|Push press|Barbell row|Pendlay row|Deadlift|Romanian deadlift|Sumo deadlift|Good morning|Barbell hip thrust|Barbell curl|Skull crusher|Barbell reverse lunge|Barbell calf raise"))
        addAll(group("Kettlebells", "Kettlebell", "Full body", "Core, grip", "Kettlebell swing|Kettlebell goblet squat|Kettlebell deadlift|Kettlebell clean|Kettlebell press|Kettlebell snatch|Kettlebell Turkish get-up|Kettlebell row|Kettlebell reverse lunge"))
        addAll(group("Weight plates", "Weight plate", "Shoulders", "Arms, core", "Plate front raise|Plate steering wheel|Plate pinch hold|Plate ground-to-overhead|Plate Russian twist"))
        addAll(group("Cable machine", "Cable", "Back", "Biceps, rear delts", "Cable chest fly|Cable crossover|Cable row|Seated cable row|Single-arm cable row|Lat pulldown|Straight-arm pulldown|Face pull|Cable lateral raise|Cable front raise|Cable rear delt fly|Cable biceps curl|Cable hammer curl|Cable triceps pushdown|Overhead cable triceps extension|Cable wood chop|Cable pull-through|Cable crunch"))
        addAll(group("Smith machine", "Smith machine", "Legs", "Glutes, core", "Smith machine squat|Smith machine bench press|Smith machine incline press|Smith machine shoulder press|Smith machine split squat|Smith machine Romanian deadlift|Smith machine calf raise|Smith machine hip thrust"))
        addAll(group("Selectorised machines", "Gym machine", "Target muscle", "Supporting muscles", "Chest press machine|Pec deck|Assisted pull-up machine|Lat pulldown machine|Seated row machine|Shoulder press machine|Lateral raise machine|Leg press|Hack squat machine|Leg extension|Seated leg curl|Lying leg curl|Leg adductor machine|Leg abductor machine|Seated calf raise|Standing calf raise machine|Back extension machine|Ab crunch machine"))
        addAll(group("Plate-loaded machines", "Plate-loaded machine", "Target muscle", "Supporting muscles", "Plate-loaded chest press|Plate-loaded incline press|Plate-loaded row|T-bar row machine|Plate-loaded shoulder press|Pendulum squat|Belt squat|Plate-loaded leg press|Plate-loaded hack squat|Plate-loaded calf raise"))
        addAll(group("Calisthenics", "Bodyweight", "Chest", "Shoulders, triceps", "Push-up|Wide push-up|Close-grip push-up|Incline push-up|Decline push-up|Diamond push-up|Pike push-up|Pull-up|Chin-up|Neutral-grip pull-up|Assisted pull-up|Dip|Bench dip|Ring dip|Inverted row|Australian pull-up|Bodyweight squat|Pistol squat|Bulgarian split squat|Walking lunge|Glute bridge|Single-leg glute bridge|Nordic curl|Calf raise|Hanging knee raise|Hanging leg raise|Plank|Side plank|Dead bug|Bird dog|Hollow body hold|L-sit|Muscle-up"))
        addAll(group("Home workouts", "No equipment", "Full body", "Core, stabilisers", "Bodyweight good morning|Wall sit|Reverse lunge|Step-back lunge|Marching bridge|Mountain climber|High knees|Jumping jack|Bear crawl|Squat to chair|Standing calf raise|Prone Y-T-W|Superman hold"))
        addAll(group("Resistance bands", "Resistance band", "Full body", "Supporting muscles", "Band pull-apart|Band row|Band chest press|Band chest fly|Band overhead press|Band lateral raise|Band biceps curl|Band triceps extension|Band squat|Band Romanian deadlift|Band glute kickback|Band lateral walk|Band face pull"))
        addAll(group("Cardio", "Cardio equipment or outdoors", "Cardiovascular system", "Lower body", "Walking|Brisk walking|Running|Treadmill running|Treadmill walking|Outdoor cycling|Stationary cycling|Elliptical|Rowing machine|Stair climber|Swimming|Jump rope|Hiking|Dancing|Indoor cycling", "CARDIO"))
        addAll(group("Mobility & flexibility", "Mat or no equipment", "Mobility", "Varies by movement", "Cat-cow|Child's pose|Thoracic rotation|World's greatest stretch|Hip flexor stretch|Hamstring stretch|Calf stretch|Chest doorway stretch|Shoulder circles|Ankle rocks|Wrist circles|Couch stretch|90/90 hip switches|Open book rotation|Neck mobility|Foam rolling", "DURATION"))
        addAll(group("Warm-up & cool-down", "Mat or no equipment", "General", "Varies by movement", "Arm circles|Leg swings|Bodyweight squat warm-up|Walking warm-up|Easy cycle warm-up|Gentle walking cool-down|Breathing recovery", "DURATION"))
    }.distinctBy { it.id }.sortedWith(compareBy({ it.category }, { it.name }))

    fun search(
        query: String,
        category: String? = null,
        equipment: String? = null,
        muscle: String? = null
    ): List<ExerciseDefinition> = all.filter { exercise ->
        val matchesQuery = query.isBlank() ||
            exercise.name.contains(query, true) ||
            exercise.aliases.any { it.contains(query, true) } ||
            exercise.primaryMuscles.contains(query, true) ||
            exercise.secondaryMuscles.contains(query, true)
        val matchesCategory = category.isNullOrBlank() || exercise.category == category
        val matchesEquipment = equipment.isNullOrBlank() || exercise.equipment.contains(equipment, true)
        val matchesMuscle = muscle.isNullOrBlank() ||
            exercise.primaryMuscles.contains(muscle, true) ||
            exercise.secondaryMuscles.contains(muscle, true)
        matchesQuery && matchesCategory && matchesEquipment && matchesMuscle
    }

    val categories: List<String> get() = all.map { it.category }.distinct()
}
