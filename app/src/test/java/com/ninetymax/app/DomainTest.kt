package com.ninetymax.app

import com.ninetymax.app.data.*
import com.ninetymax.app.domain.*
import com.ninetymax.app.notify.Reminders
import com.ninetymax.app.seed.ProductCatalog
import com.ninetymax.app.seed.Seed
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DomainTest {
    // 2026-11-02 bir Pazartesi
    private val start = LocalDate.of(2026, 11, 2)
    private fun fresh(): AppData = Seed.apply(AppData(), start).copy(onboarded = true)

    @Test fun programDayAndPhases() {
        val d = fresh()
        assertEquals(1, Program.dayOf(d, start))
        assertEquals(30, Program.dayOf(d, start.plusDays(29)))
        assertEquals(0, Program.dayOf(d, start.minusDays(1)))
        assertEquals(1, Program.phaseOf(28).index)
        assertEquals(2, Program.phaseOf(29).index)
        assertEquals(3, Program.phaseOf(60).index)
        assertEquals(4, Program.phaseOf(90).index)
    }

    @Test fun dailyTasksFollowSchedules() {
        val d = fresh()
        val day1 = DailyTasks.forDate(d, start).map { it.id }
        assertTrue("sabah temizliği", "r:am_cleanse" in day1)
        assertTrue("yıkama günü 1", "r:am_hair_wash" in day1)
        assertFalse("aktifler varsayılan kapalı", "r:pm_active_1" in day1)
        assertTrue("pazartesi antrenman", DailyTasks.WORKOUT_ID in day1)
        assertTrue("başlangıç kontrolü", "p:eval1" in day1)
        val day2 = DailyTasks.forDate(d, start.plusDays(1)).map { it.id }
        assertFalse("3 günde bir yıkama", "r:am_hair_wash" in day2)
        assertTrue("4. gün yıkama", "r:am_hair_wash" in DailyTasks.forDate(d, start.plusDays(3)).map { it.id })
        val sunday = DailyTasks.forDate(d, start.plusDays(6)).map { it.id }
        assertFalse("pazar dinlenme", DailyTasks.WORKOUT_ID in sunday)
    }

    @Test fun enabledActiveStartsOnlyFromItsStartDay() {
        val d = fresh().let { it.copy(routines = it.routines.map { r -> if (r.id == "pm_active_1") r.copy(enabled = true) else r }) }
        assertFalse("r:pm_active_1" in DailyTasks.forDate(d, start).map { it.id })
        // Gün 15 = 16 Kasım 2026 Pazartesi
        assertTrue("r:pm_active_1" in DailyTasks.forDate(d, start.plusDays(14)).map { it.id })
    }

    @Test fun toggleSkipMoveAndProgress() {
        var d = fresh()
        val total = DailyTasks.progress(d, start).total
        assertEquals(0, DailyTasks.progress(d, start).done)
        d = DailyTasks.toggle(d, start, "r:am_cleanse")
        assertEquals(1, DailyTasks.progress(d, start).done)
        d = DailyTasks.toggle(d, start, "r:am_cleanse")
        assertEquals(0, DailyTasks.progress(d, start).done)
        d = DailyTasks.skip(d, start, "r:am_moist")
        assertEquals(total - 1, DailyTasks.progress(d, start).total)
        val task = DailyTasks.forDate(d, start).first { it.id == "r:am_spf" }
        d = DailyTasks.move(d, start, task, start.plusDays(1))
        assertEquals(total - 2, DailyTasks.progress(d, start).total)
        assertTrue(DailyTasks.forDate(d, start.plusDays(1)).any { it.title == task.title && it.id.startsWith("c:") })
    }

    @Test fun supplementInUseCreatesDailyTask() {
        var d = fresh()
        assertFalse(DailyTasks.forDate(d, start).any { it.id == "s:creatine" })
        d = d.copy(products = d.products.map { if (it.id == "creatine") it.copy(status = ProductStatus.IN_USE) else it })
        assertTrue(DailyTasks.forDate(d, start).any { it.id == "s:creatine" })
    }

    @Test fun workoutRespectsEquipment() {
        val bodyweightOnly = fresh().let { it.copy(settings = it.settings.copy(equipment = setOf(Equip.BODYWEIGHT))) }
        WorkoutPlan.templates.forEach { t ->
            WorkoutPlan.plan(bodyweightOnly, t.id, 10).exercises.forEach { pe ->
                assertTrue("${t.id}: ${pe.exercise.id} ekipman gerektirmemeli", pe.exercise.requires.isEmpty())
            }
        }
        val gym = fresh().let { it.copy(settings = it.settings.copy(equipment = Equip.entries.toSet())) }
        assertEquals("lat_pulldown", WorkoutPlan.plan(gym, "upperA", 1).exercises.first().exercise.id)
        val home = fresh()
        assertEquals("pullup", WorkoutPlan.plan(home, "upperA", 1).exercises.first().exercise.id)
    }

    @Test fun workoutOverridesAndPhases() {
        var d = fresh()
        d = d.copy(workout = d.workout.copy(slotOverrides = mapOf("upperA:0" to "negative_pullup"), slotParams = mapOf("upperA:1" to SlotParam(sets = 5))))
        val plan = WorkoutPlan.plan(d, "upperA", 1)
        assertEquals("negative_pullup", plan.exercises[0].exercise.id)
        assertEquals(5, plan.exercises[1].sets)
        val normal = WorkoutPlan.plan(fresh(), "upperA", 40).exercises.sumOf { it.sets }
        val deload = WorkoutPlan.plan(fresh(), "upperA", 60).exercises.sumOf { it.sets }
        assertTrue("toparlanma haftası daha az set", deload < normal)
    }

    @Test fun nutritionTargets() {
        val t = Nutrition.targets(fresh())
        assertEquals(131, t.protein) // 73 kg × 1.8
        assertTrue(t.kcal in 2600..3300)
        assertTrue(Nutrition.weekly(fresh(), start).suggestions.isNotEmpty())
        MealSlot.entries.forEach { assertNotNull(Nutrition.chosen(fresh(), start, it)) }
        val noMeat = fresh().let { it.copy(settings = it.settings.copy(excludedTags = setOf("et"))) }
        assertTrue(Nutrition.options(noMeat, MealSlot.LUNCH).none { "et" in it.tags })
    }

    @Test fun catalogIntegrity() {
        val ids = ProductCatalog.products.map { it.id }
        assertEquals("benzersiz kimlik", ids.size, ids.toSet().size)
        ProductCatalog.products.forEach { p ->
            p.alternativeIds.forEach { assertTrue("${p.id} alternatifi $it", it in ids) }
            assertNull("fiyat uydurulmamalı: ${p.id}", p.priceTl)
            assertTrue(p.purpose.isNotBlank() && p.howTo.isNotBlank())
        }
        ProductCategory.entries.forEach { c -> assertTrue("kategori boş: $c", ProductCatalog.products.any { it.category == c }) }
        Seed.defaultRoutines().mapNotNull { it.productId }.forEach { assertTrue("rutin ürünü $it", it in ids) }
        WorkoutPlan.templates.flatMap { it.slots }.flatMap { it.candidates }.forEach { assertNotNull("egzersiz $it", Exercises.get(it)) }
    }

    @Test fun seedIsIdempotentAndRespectsDeletes() {
        val d = fresh()
        val again = Seed.apply(d, start)
        assertEquals(d.products.size, again.products.size)
        val deleted = d.copy(products = d.products.filterNot { it.id == "creatine" }, deletedSeedProductIds = setOf("creatine"))
        assertFalse(Seed.apply(deleted, start).products.any { it.id == "creatine" })
    }

    @Test fun reminderNextTrigger() {
        val r = Reminder("x", "Test", "08:00", days = setOf(1))
        val mondayMorning = LocalDateTime.of(2026, 11, 2, 7, 0)
        assertEquals(LocalDateTime.of(2026, 11, 2, 8, 0), Reminders.nextTrigger(r, mondayMorning))
        assertEquals(LocalDateTime.of(2026, 11, 9, 8, 0), Reminders.nextTrigger(r, mondayMorning.plusHours(2)))
        assertNull(Reminders.nextTrigger(r.copy(enabled = false), mondayMorning))
    }
}
