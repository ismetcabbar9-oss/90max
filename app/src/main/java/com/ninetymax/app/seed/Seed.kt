package com.ninetymax.app.seed

import com.ninetymax.app.data.AppData
import com.ninetymax.app.data.Area
import com.ninetymax.app.data.Reminder
import com.ninetymax.app.data.RoutineItem
import com.ninetymax.app.data.Schedule
import com.ninetymax.app.data.ScheduleType
import com.ninetymax.app.data.Slot
import com.ninetymax.app.data.iso
import java.time.LocalDate

object Seed {
    const val VERSION = 1

    /** İlk açılışta varsayılanları kurar; sonraki sürümlerde yalnızca eksik katalog ürünlerini ekler. */
    fun apply(data: AppData, today: LocalDate): AppData {
        var d = data
        if (d.profile.startDate.isBlank()) d = d.copy(profile = d.profile.copy(startDate = today.iso()))
        if (d.seedVersion == 0) {
            d = d.copy(
                routines = if (d.routines.isEmpty()) defaultRoutines() else d.routines,
                reminders = if (d.reminders.isEmpty()) defaultReminders() else d.reminders,
                settings = d.settings.copy(dentistNextDate = d.settings.dentistNextDate),
            )
        }
        val existing = d.products.map { it.id }.toSet()
        val missing = ProductCatalog.products.filter { it.id !in existing && it.id !in d.deletedSeedProductIds }
        if (missing.isNotEmpty()) d = d.copy(products = d.products + missing)
        if (d.seedVersion < VERSION) d = d.copy(seedVersion = VERSION)
        return d
    }

    private var order = 0
    private fun r(
        id: String, title: String, area: Area, slot: Slot, detail: String = "",
        schedule: Schedule = Schedule(), productId: String? = null, enabled: Boolean = true,
    ) = RoutineItem(id, title, detail, area, slot, schedule, productId, enabled, order++)

    private fun every(n: Int, start: Int = 1, end: Int = 90) = Schedule(ScheduleType.EVERY_N_DAYS, everyN = n, startDay = start, endDay = end)
    private fun days(vararg d: Int, start: Int = 1, end: Int = 90) = Schedule(ScheduleType.WEEKDAYS, weekdays = d.toSet(), startDay = start, endDay = end)

    fun defaultRoutines(): List<RoutineItem> {
        order = 0
        return listOf(
            // Sabah
            r("am_wake", "Hedef saatte kalk, 1 bardak su iç", Area.SLEEP, Slot.MORNING, "Hafta sonu da ±1 saat içinde kal."),
            r("am_cleanse", "Yüzü nazik temizleyiciyle yıka", Area.SKIN, Slot.MORNING, "Kuru/normal ciltte sabah sadece ılık su da yeterli olabilir.", productId = "cerave_hydrating_cleanser"),
            r("am_moist", "Nemlendirici sür", Area.SKIN, Slot.MORNING, "Nemli cilde bezelye/fındık büyüklüğünde.", productId = "cerave_pm_lotion"),
            r("am_spf", "Güneş koruyucu (SPF 50) sür", Area.SKIN, Slot.MORNING, "Yüz + boyun için 2 parmak boyu. Dışarıdaysan 2-3 saatte bir yenile.", productId = "lrp_uvmune"),
            r("am_teeth", "Dişleri 2 dakika fırçala", Area.ORAL, Slot.MORNING, "Florürlü macun, nazik dairesel hareketler.", productId = "oralb_vitality"),
            r("am_hair_wash", "Saç yıkama günü: sülfatsız şampuan + krem", Area.HAIR, Slot.MORNING, "Saç kremini parmakla tara, durularken biraz krem bırak. Islakken bukle kremi + jel, havlu yerine tişörtle 'scrunch'.", every(3), "sm_curl_shine_shampoo"),
            r("am_curl_refresh", "Bukle tazeleme (yıkamasız günlerde)", Area.HAIR, Slot.MORNING, "Su spreyi + avuç içinde az bukle kremi; bukleleri sıkarak şekillendir, saçı tarama.", productId = "cantu_curling_cream"),
            r("am_beard", "Sakal/bıyık: yağ veya balm, tara", Area.BEARD, Slot.MORNING, "2-3 damla yağ; köke yedir, tarakla şekil ver.", productId = "proraso_beard_oil"),
            r("am_deo", "Deodorant / antiperspirant", Area.BODY, Slot.MORNING, "Antiperspirant gece kuru cilde uygulandığında daha etkili olabilir.", productId = "rexona_men"),
            r("am_posture", "Duruş kontrolü: 1 dk duvar testi + 10 chin tuck", Area.POSTURE, Slot.MORNING, "Topuk, kalça, sırt ve baş duvarda; kaburgalar aşağıda."),
            r("am_weigh", "Haftalık tartı (aç karna, tuvaletten sonra)", Area.GENERAL, Slot.MORNING, "Gelişim > Kilo bölümüne kaydet.", every(7)),
            // Gün içi
            r("day_walk", "Yürüyüş: 30 dk veya 8000 adım", Area.POSTURE, Slot.DAY, "Kondisyon ve toparlanma için. Gelişim > Aktivite'ye kaydet."),
            r("day_breaks", "Saatte bir kalk: omuzlar geri-aşağı, 5 nefes", Area.POSTURE, Slot.DAY, "Uzun oturma duruşa en çok zarar veren alışkanlıktır."),
            r("day_mobility", "10 dk mobilite: open book, wall slide, kalça esnetme", Area.POSTURE, Slot.DAY, "Antrenman > Aktif toparlanma şablonundaki hareketler.", days(1, 2, 4, 5, 6)),
            r("day_spf_reapply", "Dışarıdaysan güneş koruyucuyu yenile", Area.SKIN, Slot.DAY, "Sadece uzun süre dışarıdaysan.", days(6, 7)),
            // Akşam
            r("pm_cleanse", "Akşam yüz temizliği (güneş koruyucuyu çıkar)", Area.SKIN, Slot.EVENING, "SPF yoğunsa önce misel su, sonra temizleyici.", productId = "cerave_hydrating_cleanser"),
            r("pm_active_1", "Aktif #1: Azelaik asit (ince tabaka)", Area.SKIN, Slot.EVENING, "15. günden itibaren, haftada 3 akşam. Tahriş olursa ara ver. Bakım > Cilt rehberine bak.", days(1, 3, 5, start = 15), "to_azelaic", enabled = false),
            r("pm_active_2", "Aktif #2: Retinoid (bezelye kadar, tüm yüze)", Area.SKIN, Slot.EVENING, "Yalnızca Aktif #1 sorunsuzsa ve tercihen dermatoloğa danışarak. 43. günden itibaren haftada 2 akşam, azelaik olmayan günlerde.", days(2, 6, start = 43), "adapalene", enabled = false),
            r("pm_moist", "Gece nemlendiricisi", Area.SKIN, Slot.EVENING, productId = "cerave_pm_lotion"),
            r("pm_teeth", "Dişleri 2 dakika fırçala", Area.ORAL, Slot.EVENING, "Fırçaladıktan sonra tükür, ağzını suyla çalkalama (florür etkisi için).", productId = "oralb_vitality"),
            r("pm_floss", "Diş ipi", Area.ORAL, Slot.EVENING, "Her diş arası, C şeklinde sararak.", productId = "oralb_essential_floss"),
            r("pm_tongue", "Dil temizliği", Area.ORAL, Slot.EVENING, "Arkadan öne 3-4 nazik geçiş.", productId = "tongue_scraper"),
            r("pm_body", "Duş sonrası vücut nemlendirici", Area.BODY, Slot.EVENING, productId = "cerave_body_lotion"),
            r("pm_hair_protect", "Saçı koru: saten yastık kılıfı veya gevşek topuz", Area.HAIR, Slot.EVENING, "Bukleleri sürtünmeden korur.", productId = "satin_pillowcase"),
            r("pm_beard_trim", "Sakal hattını ve boyunu düzelt", Area.BEARD, Slot.EVENING, "Yanak ve boyun hattı temiz; bıyık dudak çizgisini geçmesin.", every(7, start = 3), "philips_beard_3000"),
            r("pm_nails", "El ve ayak tırnaklarını kes, eğele", Area.BODY, Slot.EVENING, "Ardından el kremi.", every(7, start = 5), "nail_kit"),
            r("pm_scrub", "Vücut peelingi (kese veya peeling eldiveni)", Area.BODY, Slot.EVENING, "Nazikçe; tahriş varsa atla.", every(7, start = 6), "hamam_kesesi"),
            r("pm_outfit", "Yarının kıyafetini hazırla (temiz, ütülü)", Area.STYLE, Slot.EVENING, "Clean Boy görünümü: sade, temiz, iyi oturan parçalar.", days(1, 2, 3, 4, 5, 7)),
            r("pm_shoes", "Sneaker'ları temizle", Area.STYLE, Slot.EVENING, "Beyaz sneaker'ı nemli bez ve az sabunla sil.", every(7, start = 7)),
            r("pm_screens", "Yatmadan 30 dk önce ekranları bırak", Area.SLEEP, Slot.EVENING, "Işıkları kıs, oda serin ve karanlık."),
            r("pm_sleep", "Hedef saatte yat (7,5-9 saat uyku)", Area.SLEEP, Slot.EVENING, "Kas gelişiminin büyük kısmı uykuda gerçekleşir."),
            r("pm_review", "Günü değerlendir (1 dk)", Area.GENERAL, Slot.EVENING, "Bugün ekranından ruh hali ve enerji puanı ver."),
        )
    }

    fun defaultReminders(): List<Reminder> = listOf(
        Reminder("rem_morning", "Sabah rutini", "08:00", message = "Cilt, diş, saç ve duruş rutinin seni bekliyor."),
        Reminder("rem_water1", "Su molası", "11:00", message = "Bir bardak su iç."),
        Reminder("rem_water2", "Su molası", "15:30", message = "Su hedefine ne kadar kaldı?"),
        Reminder("rem_workout", "Antrenman zamanı", "18:00", days = setOf(1, 2, 4, 5, 6), message = "Bugünün antrenmanı hazır."),
        Reminder("rem_evening", "Akşam rutini", "22:30", message = "Yüz temizliği, diş ipi ve bakım."),
        Reminder("rem_sleep", "Uyku hazırlığı", "23:00", message = "Ekranları bırak, yarın için dinlen."),
    )
}
