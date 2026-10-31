package com.ninetymax.app.domain

import com.ninetymax.app.data.AppData
import com.ninetymax.app.data.MealOption
import com.ninetymax.app.data.MealSlot
import com.ninetymax.app.data.iso
import com.ninetymax.app.data.toDateOrNull
import java.time.LocalDate
import kotlin.math.roundToInt

data class FoodItem(val name: String, val portion: String, val kcal: Int, val protein: Double, val carbs: Double, val fat: Double)

data class NutritionTargets(val kcal: Int, val protein: Int, val waterMl: Int, val bmr: Int, val tdee: Int, val weightKg: Double)

data class WeeklyNutrition(
    val daysLogged: Int,
    val avgKcal: Int,
    val avgProtein: Int,
    val proteinHitDays: Int,
    val avgWaterMl: Int,
    val weightChangeKg: Double?,
    val suggestions: List<String>,
)

object Nutrition {
    fun currentWeight(data: AppData): Double =
        data.weights.maxByOrNull { it.date }?.kg ?: data.profile.startWeightKg

    fun targets(data: AppData): NutritionTargets {
        val w = currentWeight(data)
        val p = data.profile
        // Mifflin-St Jeor (erkek). Sadece başlangıç tahminidir; tartı trendine göre ayarlanmalı.
        val bmr = 10 * w + 6.25 * p.heightCm - 5 * p.age + 5
        val tdee = bmr * p.activityFactor
        val kcal = data.settings.calorieOverride ?: (tdee + data.settings.calorieSurplus).roundToInt()
        val protein = (w * data.settings.proteinPerKg).roundToInt()
        return NutritionTargets(kcal, protein, data.settings.waterTargetMl, bmr.roundToInt(), tdee.roundToInt(), w)
    }

    fun options(data: AppData, slot: MealSlot): List<MealOption> {
        val all = (MealLibrary.options + data.meals.customOptions)
            .filter { it.slot == slot && it.id !in data.meals.hiddenOptionIds }
            .filter { opt -> opt.tags.none { it in data.settings.excludedTags } }
        return if (data.settings.preferEconomic) all.sortedByDescending { "ekonomik" in it.tags } else all
    }

    /** O gün için seçilen (veya sırayla dönen varsayılan) öğün seçeneği. */
    fun chosen(data: AppData, date: LocalDate, slot: MealSlot): MealOption? {
        val opts = options(data, slot)
        if (opts.isEmpty()) return null
        data.meals.choices["${date.iso()}|${slot.name}"]?.let { id -> opts.firstOrNull { it.id == id }?.let { return it } }
        return opts[(date.dayOfYear) % opts.size]
    }

    fun dayTotals(data: AppData, date: String): Pair<Int, Double> {
        val logs = data.foodLogs.filter { it.date == date }
        return logs.sumOf { it.kcal } to logs.sumOf { it.protein }
    }

    fun weekly(data: AppData, end: LocalDate): WeeklyNutrition {
        val t = targets(data)
        val days = (0..6).map { end.minusDays(it.toLong()).iso() }
        val logged = days.filter { d -> data.foodLogs.any { it.date == d } }
        val kcals = logged.map { dayTotals(data, it).first }
        val proteins = logged.map { dayTotals(data, it).second }
        val avgK = if (kcals.isEmpty()) 0 else kcals.average().roundToInt()
        val avgP = if (proteins.isEmpty()) 0 else proteins.average().roundToInt()
        val hit = proteins.count { it >= t.protein * 0.9 }
        val water = days.map { data.water[it] ?: 0 }
        val avgW = water.average().roundToInt()

        val windowStart = end.minusDays(13)
        val recent = data.weights.filter { e -> e.date.toDateOrNull()?.let { it >= windowStart && it <= end } == true }
            .sortedBy { it.date }
        val change = if (recent.size >= 2) recent.last().kg - recent.first().kg else null
        val weeks = if (recent.size >= 2) {
            val d0 = recent.first().date.toDateOrNull()!!
            val d1 = recent.last().date.toDateOrNull()!!
            ((d1.toEpochDay() - d0.toEpochDay()) / 7.0).coerceAtLeast(1.0)
        } else 1.0
        val perWeek = change?.div(weeks)

        val s = mutableListOf<String>()
        if (logged.size < 4) s += "Bu hafta ${logged.size} gün kayıt var. Daha doğru değerlendirme için en az 4 gün kaydetmeyi dene — eksik günler sorun değil."
        if (logged.isNotEmpty() && avgP < t.protein * 0.9) s += "Ortalama protein hedefin altında ($avgP / ${t.protein} g). Her öğüne bir avuç protein ekle: yumurta, yoğurt, tavuk, mercimek. Yetmezse protein tozu bir seçenek."
        if (logged.isNotEmpty() && avgP >= t.protein * 0.9) s += "Protein hedefini büyük ölçüde tutturuyorsun. Böyle devam."
        if (avgW < t.waterMl * 0.8) s += "Su tüketimi hedefin altında. Yanına şişe al, her öğünde 1 bardak iç."
        when {
            perWeek == null -> s += "Kilo trendi için haftada en az 1-2 kez aynı koşulda tartıl."
            perWeek > 0.5 -> s += "Haftalık artış ~${"%.1f".format(perWeek)} kg — hedeflenenden hızlı. Günlük kaloriyi 150-200 kcal azaltmayı düşünebilirsin."
            perWeek < 0.1 -> s += "Kilo neredeyse sabit. Kas gelişimi için günlük 150-200 kcal ekleyebilirsin (ör. 1 avuç kuruyemiş + 1 muz)."
            else -> s += "Kilo artışı kontrollü aralıkta (~${"%.1f".format(perWeek)} kg/hafta). Plan iyi gidiyor."
        }
        return WeeklyNutrition(logged.size, avgK, avgP, hit, avgW, change, s)
    }
}
