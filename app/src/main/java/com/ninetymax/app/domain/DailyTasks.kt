package com.ninetymax.app.domain

import com.ninetymax.app.data.AppData
import com.ninetymax.app.data.Area
import com.ninetymax.app.data.CustomTask
import com.ninetymax.app.data.DayStatus
import com.ninetymax.app.data.ProductCategory
import com.ninetymax.app.data.ProductStatus
import com.ninetymax.app.data.RoutineItem
import com.ninetymax.app.data.ScheduleType
import com.ninetymax.app.data.Slot
import com.ninetymax.app.data.Timing
import com.ninetymax.app.data.iso
import com.ninetymax.app.data.newId
import java.time.LocalDate

data class DailyTask(
    val id: String,
    val title: String,
    val detail: String,
    val area: Area,
    val slot: Slot,
    val productId: String? = null,
)

data class DayProgress(val total: Int, val done: Int) {
    val percent: Int get() = if (total == 0) 0 else (done * 100) / total
}

object DailyTasks {
    const val WORKOUT_ID = "w:workout"
    const val WATER_ID = "n:water"
    const val PROTEIN_ID = "n:protein"

    fun matches(item: RoutineItem, programDay: Int, isoWeekday: Int): Boolean {
        val s = item.schedule
        if (!item.enabled) return false
        if (programDay < s.startDay || programDay > s.endDay) return false
        return when (s.type) {
            ScheduleType.DAILY -> true
            ScheduleType.WEEKDAYS -> isoWeekday in s.weekdays
            ScheduleType.EVERY_N_DAYS -> (programDay - s.startDay) % s.everyN.coerceAtLeast(1) == 0
        }
    }

    fun forDate(data: AppData, date: LocalDate): List<DailyTask> {
        val day = Program.dayOf(data, date)
        // Program bitse de temel rutinler devam etsin diye 90. günden sonrası son haftanın kurallarını kullanır.
        val effectiveDay = if (day > PROGRAM_DAYS) PROGRAM_DAYS - 6 + ((day - PROGRAM_DAYS - 1) % 7) else day
        val weekday = date.dayOfWeek.value
        val tasks = mutableListOf<DailyTask>()

        data.routines.sortedBy { it.order }.filter { matches(it, effectiveDay.coerceAtLeast(1), weekday) }.forEach {
            tasks += DailyTask("r:${it.id}", it.title, it.detail, it.area, it.slot, it.productId)
        }

        val template = WorkoutPlan.templateForDate(data.workout, weekday)
        if (!template.isRest) {
            tasks += DailyTask(WORKOUT_ID, "Antrenman: ${template.name}", template.focus, Area.WORKOUT, Slot.DAY)
        }

        val t = Nutrition.targets(data)
        tasks += DailyTask(PROTEIN_ID, "Protein hedefi: ${t.protein} g", "Her öğünde bir protein kaynağı", Area.NUTRITION, Slot.DAY)
        tasks += DailyTask(WATER_ID, "Su: ${"%.1f".format(t.waterMl / 1000.0)} L", "Gün boyu yay", Area.NUTRITION, Slot.DAY)

        data.products.filter { it.category == ProductCategory.SUPPLEMENT && it.status == ProductStatus.IN_USE && it.dailyTracking }
            .forEach { p ->
                val slot = when (p.timing) { Timing.EVENING -> Slot.EVENING; Timing.MORNING -> Slot.MORNING; else -> Slot.DAY }
                tasks += DailyTask("s:${p.id}", "${p.name}${if (p.dose.isNotBlank()) " · ${p.dose}" else ""}", p.timing.label, Area.SUPPLEMENT, slot, p.id)
            }

        if (day in CHECKPOINT_DAYS) {
            tasks += DailyTask("p:eval$day", "Gelişim kontrolü (Gün $day): ön/yan/arka fotoğraf + ölçüler", "Gelişim > Değerlendirme", Area.GENERAL, Slot.MORNING)
        }
        data.settings.dentistNextDate?.let {
            if (it == date.iso()) tasks += DailyTask("o:dentist", "Diş hekimi kontrolü", "Randevunu kontrol et", Area.ORAL, Slot.DAY)
        }

        data.customTasks.filter { it.date == date.iso() }.forEach {
            tasks += DailyTask("c:${it.id}", it.title, it.note, it.area, it.slot)
        }
        return tasks
    }

    fun status(data: AppData, date: LocalDate): DayStatus = data.taskStatus[date.iso()] ?: DayStatus()

    fun progress(data: AppData, date: LocalDate): DayProgress {
        val tasks = forDate(data, date)
        val st = status(data, date)
        val counted = tasks.filter { it.id !in st.skipped && it.id !in st.moved }
        return DayProgress(counted.size, counted.count { it.id in st.done })
    }

    fun toggle(data: AppData, date: LocalDate, taskId: String, done: Boolean? = null): AppData {
        val key = date.iso()
        val st = data.taskStatus[key] ?: DayStatus()
        val isDone = done ?: (taskId !in st.done)
        val newSt = st.copy(
            done = if (isDone) st.done + taskId else st.done - taskId,
            skipped = st.skipped - taskId,
        )
        return data.copy(taskStatus = data.taskStatus + (key to newSt))
    }

    fun skip(data: AppData, date: LocalDate, taskId: String, skipped: Boolean = true): AppData {
        val key = date.iso()
        val st = data.taskStatus[key] ?: DayStatus()
        val newSt = st.copy(skipped = if (skipped) st.skipped + taskId else st.skipped - taskId, done = st.done - taskId)
        return data.copy(taskStatus = data.taskStatus + (key to newSt))
    }

    /** Görevi cezasız biçimde başka bir güne taşır: o gün sayılmaz, hedef güne tek seferlik görev eklenir. */
    fun move(data: AppData, from: LocalDate, task: DailyTask, to: LocalDate): AppData {
        val key = from.iso()
        val st = data.taskStatus[key] ?: DayStatus()
        val newSt = st.copy(moved = st.moved + task.id, done = st.done - task.id)
        val copy = CustomTask(newId(), to.iso(), task.title, task.area, task.slot, "Ertelendi: ${from.iso()}")
        return data.copy(taskStatus = data.taskStatus + (key to newSt), customTasks = data.customTasks + copy)
    }

    fun unfinished(data: AppData, date: LocalDate): List<DailyTask> {
        val st = status(data, date)
        return forDate(data, date).filter { it.id !in st.done && it.id !in st.skipped && it.id !in st.moved }
    }

    /** Programın başlangıcından bugüne günlük tamamlanma oranları (grafik için). */
    fun completionHistory(data: AppData, today: LocalDate, maxDays: Int = 90): List<Pair<LocalDate, Int>> {
        val start = Program.startDate(data, today)
        val days = (today.toEpochDay() - start.toEpochDay()).toInt().coerceIn(0, maxDays - 1)
        return (days downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            d to progress(data, d).percent
        }
    }
}
