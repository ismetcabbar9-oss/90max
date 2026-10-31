package com.ninetymax.app.data

import kotlinx.serialization.Serializable

/** Uygulamanın tüm kalıcı durumu. Tek bir JSON dosyasında saklanır. */
@Serializable
data class AppData(
    val version: Int = 1,
    val seedVersion: Int = 0,
    val onboarded: Boolean = false,
    val profile: Profile = Profile(),
    val settings: Settings = Settings(),
    val routines: List<RoutineItem> = emptyList(),
    val customTasks: List<CustomTask> = emptyList(),
    val taskStatus: Map<String, DayStatus> = emptyMap(),
    val products: List<Product> = emptyList(),
    val deletedSeedProductIds: Set<String> = emptySet(),
    val workout: WorkoutConfig = WorkoutConfig(),
    val workoutLogs: List<WorkoutLog> = emptyList(),
    val meals: MealConfig = MealConfig(),
    val foodLogs: List<FoodEntry> = emptyList(),
    val water: Map<String, Int> = emptyMap(),
    val weights: List<WeightEntry> = emptyList(),
    val measurements: List<MeasurementEntry> = emptyList(),
    val photos: List<PhotoRecord> = emptyList(),
    val sleep: List<SleepEntry> = emptyList(),
    val activity: List<ActivityEntry> = emptyList(),
    val reviews: List<DayReview> = emptyList(),
    val appearance: List<AppearanceEntry> = emptyList(),
    val evaluations: List<Evaluation> = emptyList(),
    val wardrobe: List<ClothingItem> = emptyList(),
    val outfits: List<Outfit> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
)

@Serializable
data class Profile(
    val name: String = "",
    val startDate: String = "",
    val startWeightKg: Double = 73.0,
    val targetWeightKg: Double = 78.0,
    val heightCm: Int = 178,
    val age: Int = 24,
    val activityFactor: Double = 1.55,
)

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val equipment: Set<Equip> = setOf(Equip.BODYWEIGHT, Equip.DUMBBELL, Equip.BAND, Equip.PULLUP_BAR),
    val proteinPerKg: Double = 1.8,
    val calorieSurplus: Int = 300,
    val calorieOverride: Int? = null,
    val waterTargetMl: Int = 2800,
    val monthlyCareBudget: Double = 1500.0,
    val clothingBudget: Double = 5000.0,
    val bedTime: String = "23:30",
    val wakeTime: String = "07:30",
    val stepsTarget: Int = 8000,
    val dentistNextDate: String? = null,
    val preferEconomic: Boolean = false,
    val excludedTags: Set<String> = emptySet(),
)

@Serializable
enum class Area(val label: String) {
    SKIN("Cilt"), HAIR("Saç"), BEARD("Sakal"), ORAL("Ağız & Diş"), BODY("Vücut & Hijyen"),
    POSTURE("Duruş & Kondisyon"), SLEEP("Uyku"), NUTRITION("Beslenme"), SUPPLEMENT("Takviye"),
    WORKOUT("Antrenman"), STYLE("Stil"), GENERAL("Genel")
}

@Serializable
enum class Slot(val label: String) { MORNING("Sabah"), DAY("Gün içi"), EVENING("Akşam") }

@Serializable
enum class ScheduleType { DAILY, WEEKDAYS, EVERY_N_DAYS }

@Serializable
data class Schedule(
    val type: ScheduleType = ScheduleType.DAILY,
    /** ISO gün numaraları: 1 = Pazartesi ... 7 = Pazar */
    val weekdays: Set<Int> = (1..7).toSet(),
    val everyN: Int = 1,
    val startDay: Int = 1,
    val endDay: Int = 90,
)

@Serializable
data class RoutineItem(
    val id: String,
    val title: String,
    val detail: String = "",
    val area: Area,
    val slot: Slot,
    val schedule: Schedule = Schedule(),
    val productId: String? = null,
    val enabled: Boolean = true,
    val order: Int = 0,
)

/** Belirli bir güne ait tek seferlik görev (kullanıcı ekler veya ertelenen görevden oluşur). */
@Serializable
data class CustomTask(
    val id: String,
    val date: String,
    val title: String,
    val area: Area = Area.GENERAL,
    val slot: Slot = Slot.DAY,
    val note: String = "",
)

@Serializable
data class DayStatus(
    val done: Set<String> = emptySet(),
    val skipped: Set<String> = emptySet(),
    val moved: Set<String> = emptySet(),
)

@Serializable
enum class ProductCategory(val label: String) {
    CLEANSER("Yüz temizleyici"), MOISTURIZER("Nemlendirici"), SUNSCREEN("Güneş koruyucu"),
    SKIN_TREATMENT("Cilt bakım / aktif"), HAIR("Şampuan & saç bakımı"), CURL("Bukle belirginleştirici"),
    BEARD("Sakal & bıyık"), ORAL("Diş & ağız"), HAND_NAIL("El & tırnak"), BODY("Vücut bakımı"),
    FRAGRANCE("Parfüm & hijyen"), SPORT("Spor ekipmanı"), SUPPLEMENT("Besin desteği / takviye")
}

@Serializable
enum class ProductStatus(val label: String) {
    NONE("Katalogda"), TO_BUY("Satın alınacak"), BOUGHT("Satın alındı"),
    IN_USE("Kullanılıyor"), FINISHED("Bitti"), STOPPED("Kullanmayı bıraktım")
}

@Serializable
enum class Timing(val label: String) {
    MORNING("Sabah"), EVENING("Akşam"), BOTH("Sabah & akşam"), ANY("Günün herhangi bir saati"),
    AS_NEEDED("Gerektiğinde"), WORKOUT("Antrenman çevresinde")
}

@Serializable
data class Purchase(val date: String, val priceTl: Double?)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val brand: String,
    val category: ProductCategory,
    val purpose: String = "",
    val howTo: String = "",
    val frequency: String = "",
    val timing: Timing = Timing.ANY,
    val warnings: String = "",
    val alternativeIds: List<String> = emptyList(),
    val priceTl: Double? = null,
    val priceCheckedDate: String? = null,
    val link: String? = null,
    val status: ProductStatus = ProductStatus.NONE,
    val favorite: Boolean = false,
    val custom: Boolean = false,
    val imageFile: String? = null,
    val notes: String = "",
    val purchases: List<Purchase> = emptyList(),
    /** Takviyeler için günlük takip görevi oluşturulsun mu (yalnızca durum "Kullanılıyor" iken). */
    val dailyTracking: Boolean = false,
    val dose: String = "",
)

@Serializable
enum class Equip(val label: String) {
    BODYWEIGHT("Vücut ağırlığı"), DUMBBELL("Dambıl"), BAND("Direnç bandı"), PULLUP_BAR("Barfiks barı"),
    BENCH("Ayarlı bench"), BARBELL("Barbell"), CABLE("Kablo / makine (spor salonu)")
}

@Serializable
data class SlotParam(val sets: Int? = null, val reps: String? = null, val restSec: Int? = null)

@Serializable
data class WorkoutConfig(
    /** Haftanın 7 günü (Pzt..Paz) için antrenman şablonu kimlikleri. */
    val weekPlan: List<String> = listOf("upperA", "lowerA", "recovery", "upperB", "lowerB", "upperC", "rest"),
    /** "şablon:indeks" -> egzersiz kimliği */
    val slotOverrides: Map<String, String> = emptyMap(),
    val slotParams: Map<String, SlotParam> = emptyMap(),
)

@Serializable
data class SetLog(val exerciseId: String, val weightKg: Double = 0.0, val reps: Int = 0)

@Serializable
data class WorkoutLog(
    val id: String,
    val date: String,
    val templateId: String,
    val sets: List<SetLog> = emptyList(),
    val minutes: Int = 0,
    val note: String = "",
    val completed: Boolean = false,
)

@Serializable
enum class MealSlot(val label: String) {
    BREAKFAST("Kahvaltı"), SNACK1("Ara öğün 1"), LUNCH("Öğle"), SNACK2("Ara öğün 2"), DINNER("Akşam"), PRE_SLEEP("Yatmadan önce")
}

@Serializable
data class MealOption(
    val id: String,
    val slot: MealSlot,
    val name: String,
    val items: List<String>,
    val kcal: Int,
    val protein: Int,
    val tags: Set<String> = emptySet(),
    val custom: Boolean = false,
)

@Serializable
data class MealConfig(
    val customOptions: List<MealOption> = emptyList(),
    val hiddenOptionIds: Set<String> = emptySet(),
    /** "tarih|slot" -> seçilen seçenek kimliği */
    val choices: Map<String, String> = emptyMap(),
)

@Serializable
data class FoodEntry(
    val id: String,
    val date: String,
    val slot: MealSlot,
    val name: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
)

@Serializable
data class WeightEntry(val id: String, val date: String, val kg: Double)

@Serializable
data class MeasurementEntry(val id: String, val date: String, val values: Map<String, Double>)

@Serializable
enum class PhotoAngle(val label: String) {
    FRONT("Ön"), SIDE("Yan"), BACK("Arka"), FACE("Yüz / cilt"), SMILE("Gülüş / diş"), HAIR("Saç"), OUTFIT("Kombin")
}

@Serializable
data class PhotoRecord(val id: String, val date: String, val angle: PhotoAngle, val fileName: String, val note: String = "")

@Serializable
data class SleepEntry(val id: String, val date: String, val bed: String, val wake: String, val quality: Int = 3)

@Serializable
enum class ActivityType(val label: String) { WALK("Yürüyüş"), CARDIO("Kardiyo"), MOBILITY("Mobilite / esneme"), BREATH("Nefes çalışması") }

@Serializable
data class ActivityEntry(val id: String, val date: String, val type: ActivityType, val minutes: Int, val steps: Int? = null)

@Serializable
data class DayReview(val date: String, val mood: Int = 3, val energy: Int = 3, val note: String = "")

@Serializable
data class AppearanceEntry(val id: String, val date: String, val area: Area, val score: Int, val note: String = "")

@Serializable
data class Evaluation(
    val day: Int,
    val date: String,
    val ratings: Map<String, Int> = emptyMap(),
    val notes: String = "",
)

@Serializable
enum class ClothingType(val label: String) {
    TOP("Üst"), BOTTOM("Alt"), OUTER("Dış giyim"), SHOES("Ayakkabı"), ACCESSORY("Aksesuar")
}

@Serializable
enum class ClothingStatus(val label: String) { OWNED("Gardırobumda"), WISHLIST("Alınacak") }

@Serializable
data class ClothingItem(
    val id: String,
    val name: String,
    val type: ClothingType,
    val color: String = "",
    val status: ClothingStatus = ClothingStatus.OWNED,
    val priceTl: Double? = null,
    val imageFile: String? = null,
    val note: String = "",
)

@Serializable
data class Outfit(val id: String, val name: String, val itemIds: List<String>, val occasion: String = "", val note: String = "")

@Serializable
data class Reminder(
    val id: String,
    val title: String,
    val time: String,
    val days: Set<Int> = (1..7).toSet(),
    val enabled: Boolean = true,
    val message: String = "",
)
