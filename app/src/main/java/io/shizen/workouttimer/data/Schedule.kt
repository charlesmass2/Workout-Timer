package io.shizen.workouttimer.data

/** The default "Full Body Calisthenics" workout, matching the design spec. */
fun makeDefaultWorkout(): Workout {
    fun ss(name: String, exs: List<Pair<String, Int>>, sets: Int, restSets: Int, restAfter: Int) =
        Superset(
            id = uid(), name = name, sets = sets,
            restBetweenSets = restSets, restAfter = restAfter,
            exercises = exs.map { (n, d) -> Exercise(uid(), n, d) },
        )
    return Workout(
        id = "default",
        name = "Full Body Calisthenics",
        createdAt = System.currentTimeMillis(),
        supersets = listOf(
            ss("Superset 1", listOf("Tuck Planche" to 30, "Abs" to 30), 6, 60, 120),
            ss("Superset 2", listOf("Pull-ups" to 30, "Push-ups" to 30), 6, 60, 120),
            ss("Plank Hold", listOf("Plank" to 90), 1, 60, 120),
            ss("Superset 3", listOf("Chin-ups" to 30, "Dips" to 30), 6, 60, 120),
        ),
    )
}

/** Flatten a workout into an ordered list of timed steps. Mirrors `buildSchedule`. */
fun buildSchedule(workout: Workout, countdown: Int = 5): List<Step> {
    val steps = mutableListOf<Step>()
    if (countdown > 0) steps.add(Step(kind = StepKind.COUNTDOWN, duration = countdown))
    var workIndex = 0
    val sss = workout.supersets
    sss.forEachIndexed { si, ss ->
        val isLastSuperset = si == sss.lastIndex
        for (s in 0 until ss.sets) {
            val isLastSet = s == ss.sets - 1
            ss.exercises.forEachIndexed { ei, ex ->
                val isLastExOfSet = ei == ss.exercises.lastIndex
                steps.add(
                    Step(
                        kind = StepKind.WORK, duration = ex.duration,
                        supersetIndex = si, supersetName = ss.name,
                        exerciseIndex = ei, exerciseId = ex.id, exerciseName = ex.name,
                        exCount = ss.exercises.size,
                        setNumber = s + 1, totalSets = ss.sets,
                        workIndex = workIndex++,
                    )
                )
                val endOfSuperset = isLastExOfSet && isLastSet
                val veryLast = endOfSuperset && isLastSuperset
                if (veryLast) return@forEachIndexed
                if (endOfSuperset) {
                    steps.add(
                        Step(
                            kind = StepKind.REST, duration = ss.restAfter,
                            restType = RestType.SUPERSET,
                            fromSuperset = ss.name,
                            nextSuperset = sss[si + 1].name,
                        )
                    )
                } else {
                    steps.add(
                        Step(
                            kind = StepKind.REST, duration = ss.restBetweenSets,
                            restType = RestType.SET,
                            nextExercise = ss.exercises[(ei + 1) % ss.exercises.size].name,
                            supersetNameRest = ss.name,
                        )
                    )
                }
            }
        }
    }
    return steps
}

fun totalWorkSteps(workout: Workout): Int =
    workout.supersets.sumOf { it.sets * it.exercises.size }

/** Estimated total duration in seconds, if every timing is respected. */
fun estimateDuration(workout: Workout, countdown: Int = 5): Int =
    buildSchedule(workout, countdown).sumOf { it.duration }
