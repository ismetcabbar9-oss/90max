package com.ninetymax.app.domain

import com.ninetymax.app.data.AppData
import com.ninetymax.app.data.Equip
import com.ninetymax.app.data.WorkoutConfig

data class TemplateSlot(
    val role: String,
    val candidates: List<String>,
    val priority: Boolean = false,
)

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val focus: String,
    val slots: List<TemplateSlot>,
    val isRest: Boolean = false,
    val isRecovery: Boolean = false,
    val estMinutes: Int = 50,
)

data class PlannedExercise(
    val slotKey: String,
    val role: String,
    val exercise: Exercise,
    val sets: Int,
    val reps: String,
    val restSec: Int,
    val alternatives: List<Exercise>,
    val priority: Boolean,
)

data class PlannedWorkout(val template: WorkoutTemplate, val exercises: List<PlannedExercise>, val phase: Phase)

object WorkoutPlan {
    private val verticalPull = listOf("lat_pulldown", "pullup", "band_pullup", "negative_pullup", "straight_arm_pulldown", "inverted_row")
    private val lateral = listOf("cable_lateral", "db_lateral", "band_lateral", "prone_y_raise")
    private val lateral2 = listOf("lean_lateral", "cable_lateral", "band_lateral", "prone_y_raise")
    private val upperChest = listOf("incline_db_press", "incline_bb_press", "decline_pushup")
    private val row = listOf("chest_supported_row", "seated_cable_row", "one_arm_row", "bb_row", "band_row", "inverted_row")
    private val rear = listOf("face_pull_cable", "face_pull_band", "rear_delt_fly", "reverse_snow_angel")
    private val rear2 = listOf("rear_delt_fly", "band_pull_apart", "reverse_snow_angel")

    val templates: List<WorkoutTemplate> = listOf(
        WorkoutTemplate("upperA", "Üst A · Sırt genişliği & yan omuz", "Kanat kası, yan omuz, üst göğüs", listOf(
            TemplateSlot("Dikey çekiş", verticalPull, priority = true),
            TemplateSlot("Yan omuz", lateral, priority = true),
            TemplateSlot("Üst göğüs press", upperChest),
            TemplateSlot("Yatay çekiş", row),
            TemplateSlot("Arka omuz", rear, priority = true),
            TemplateSlot("Core", listOf("hanging_knee_raise", "dead_bug")),
        ), estMinutes = 55),
        WorkoutTemplate("lowerA", "Alt A · Bacak & core", "Ön bacak, kalça, core", listOf(
            TemplateSlot("Squat", listOf("bb_squat", "goblet_squat", "bulgarian_split", "bw_squat")),
            TemplateSlot("Kalça menteşe", listOf("bb_rdl", "db_rdl", "sl_rdl")),
            TemplateSlot("Tek bacak", listOf("reverse_lunge", "step_up")),
            TemplateSlot("Arka bacak", listOf("leg_curl", "slider_leg_curl")),
            TemplateSlot("Baldır", listOf("calf_raise")),
            TemplateSlot("Core", listOf("plank")),
            TemplateSlot("Anti-rotasyon", listOf("pallof_band", "side_plank")),
        ), estMinutes = 50),
        WorkoutTemplate("recovery", "Aktif toparlanma · Mobilite & yürüyüş", "Duruş, esneklik, kondisyon", listOf(
            TemplateSlot("Yürüyüş", listOf("brisk_walk")),
            TemplateSlot("Göğüs omurgası", listOf("open_book")),
            TemplateSlot("Omuz", listOf("wall_slide")),
            TemplateSlot("Omuz", listOf("band_dislocate", "doorway_stretch")),
            TemplateSlot("Kalça", listOf("hip_flexor_stretch")),
            TemplateSlot("Kalça", listOf("ninety_ninety")),
            TemplateSlot("Boyun", listOf("chin_tuck")),
            TemplateSlot("Nefes", listOf("box_breathing")),
        ), isRecovery = true, estMinutes = 40),
        WorkoutTemplate("upperB", "Üst B · Üst göğüs & omuz", "Üst göğüs, omuz, arka omuz", listOf(
            TemplateSlot("Üst göğüs press", upperChest, priority = true),
            TemplateSlot("Omuz press", listOf("db_ohp", "band_ohp", "pike_pushup")),
            TemplateSlot("Dikey çekiş", listOf("chinup", "lat_pulldown", "band_pullup", "negative_pullup", "inverted_row")),
            TemplateSlot("Üst göğüs fly", listOf("incline_cable_fly", "band_low_high_fly", "decline_pushup"), priority = true),
            TemplateSlot("Yan omuz", lateral2, priority = true),
            TemplateSlot("Arka omuz", rear2),
            TemplateSlot("Triceps", listOf("cable_pushdown", "overhead_ext", "band_pushdown", "diamond_pushup")),
        ), estMinutes = 55),
        WorkoutTemplate("lowerB", "Alt B · Kalça, arka bacak & core", "Kalça, arka bacak, core", listOf(
            TemplateSlot("Kalça itiş", listOf("hip_thrust", "glute_bridge")),
            TemplateSlot("Tek bacak squat", listOf("bulgarian_split", "reverse_lunge")),
            TemplateSlot("Kalça menteşe", listOf("db_rdl", "bb_rdl", "sl_rdl")),
            TemplateSlot("Step-up", listOf("step_up")),
            TemplateSlot("Baldır", listOf("calf_raise")),
            TemplateSlot("Alt karın", listOf("hanging_knee_raise", "reverse_crunch")),
            TemplateSlot("Yan core", listOf("side_plank")),
        ), estMinutes = 50),
        WorkoutTemplate("upperC", "Üst C · V-taper & kollar", "Kanat, omuz genişliği, kollar", listOf(
            TemplateSlot("Dikey çekiş", listOf("pullup", "lat_pulldown", "band_pullup", "negative_pullup", "inverted_row"), priority = true),
            TemplateSlot("Tek kol çekiş", listOf("one_arm_row", "seated_cable_row", "band_row", "inverted_row")),
            TemplateSlot("Yan omuz", lateral, priority = true),
            TemplateSlot("İtiş", listOf("decline_pushup", "pushup")),
            TemplateSlot("Arka omuz / duruş", rear, priority = true),
            TemplateSlot("Biceps", listOf("db_curl", "band_curl", "chinup", "underhand_inverted_row")),
            TemplateSlot("Triceps", listOf("overhead_ext", "cable_pushdown", "band_pushdown", "diamond_pushup")),
            TemplateSlot("Core", listOf("hollow_hold", "dead_bug")),
        ), estMinutes = 55),
        WorkoutTemplate("rest", "Dinlenme günü", "Kaslar dinlenirken büyür", listOf(
            TemplateSlot("Hafif yürüyüş", listOf("brisk_walk")),
            TemplateSlot("Nefes", listOf("box_breathing")),
        ), isRest = true, estMinutes = 20),
    )

    private val byId = templates.associateBy { it.id }
    fun template(id: String): WorkoutTemplate = byId[id] ?: byId.getValue("rest")

    fun templateForDate(config: WorkoutConfig, isoWeekday: Int): WorkoutTemplate =
        template(config.weekPlan.getOrElse(isoWeekday - 1) { "rest" })

    fun slotKey(templateId: String, index: Int) = "$templateId:$index"

    fun resolveExercise(slot: TemplateSlot, equipment: Set<Equip>, override: String?): Exercise {
        override?.let { id -> Exercises.get(id)?.let { return it } }
        val options = slot.candidates.mapNotNull { Exercises.get(it) }
        return options.firstOrNull { Exercises.available(it, equipment) }
            ?: options.firstOrNull { it.requires.isEmpty() }
            ?: options.first()
    }

    /** Aşamaya ve harekete göre set/tekrar/dinlenme. */
    fun prescription(ex: Exercise, priority: Boolean, phase: Phase): Triple<Int, String, Int> {
        val p = phase.index
        return when (ex.kind) {
            ExKind.COMPOUND -> {
                val sets = when (p) { 1 -> 3; 2 -> if (priority) 4 else 3; 3 -> 2; else -> if (priority) 4 else 3 }
                val reps = when (p) { 1 -> "10-12"; 2 -> "8-12"; 3 -> "8-10"; else -> "6-10" }
                Triple(sets, if (ex.id in setOf("negative_pullup")) "4-6" else reps, 120)
            }
            ExKind.ISOLATION -> {
                val sets = when (p) { 1 -> if (priority) 3 else 2; 2 -> if (priority) 4 else 3; 3 -> 2; else -> if (priority) 4 else 3 }
                val reps = when (p) { 1 -> "12-15"; 2 -> "12-20"; 3 -> "12-15"; else -> "10-15" }
                Triple(sets, reps, 75)
            }
            ExKind.CORE -> {
                val sets = if (p == 3) 2 else 3
                val reps = if (ex.timed) when (p) { 1 -> "20-30 sn"; 2 -> "30-45 sn"; 3 -> "20-30 sn"; else -> "45-60 sn" } else when (p) { 1 -> "8-10"; 2 -> "10-12"; 3 -> "8-10"; else -> "12-15" }
                Triple(sets, reps, 45)
            }
            ExKind.MOBILITY -> when (ex.id) {
                "brisk_walk" -> Triple(1, "30-45 dk", 0)
                "box_breathing" -> Triple(1, "5 tur", 0)
                else -> Triple(2, if (ex.timed) "30-45 sn" else "8-10", 20)
            }
        }
    }

    fun plan(data: AppData, templateId: String, programDay: Int): PlannedWorkout {
        val template = template(templateId)
        val phase = Program.phaseOf(programDay.coerceIn(1, PROGRAM_DAYS))
        val equipment = data.settings.equipment + Equip.BODYWEIGHT
        val exercises = template.slots.mapIndexed { i, slot ->
            val key = slotKey(template.id, i)
            val ex = resolveExercise(slot, equipment, data.workout.slotOverrides[key])
            val (sets, reps, rest) = prescription(ex, slot.priority, phase)
            val custom = data.workout.slotParams[key]
            val alternatives = (slot.candidates.mapNotNull { Exercises.get(it) } + sameMuscleAlternatives(ex))
                .distinctBy { it.id }
                .filter { it.id != ex.id && Exercises.available(it, equipment) }
            PlannedExercise(
                slotKey = key, role = slot.role, exercise = ex,
                sets = custom?.sets ?: sets, reps = custom?.reps ?: reps, restSec = custom?.restSec ?: rest,
                alternatives = alternatives, priority = slot.priority,
            )
        }
        return PlannedWorkout(template, exercises, phase)
    }

    private fun sameMuscleAlternatives(ex: Exercise): List<Exercise> =
        Exercises.all.filter { it.kind == ex.kind && it.muscles.firstOrNull() == ex.muscles.firstOrNull() }

    /** Basit tahmini 1 tekrar maksimum (Epley). */
    fun estimated1Rm(weight: Double, reps: Int): Double = if (reps <= 0) 0.0 else weight * (1 + reps / 30.0)
}
