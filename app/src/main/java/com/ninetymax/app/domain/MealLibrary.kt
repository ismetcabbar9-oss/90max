package com.ninetymax.app.domain

import com.ninetymax.app.data.MealOption
import com.ninetymax.app.data.MealSlot
import com.ninetymax.app.data.MealSlot.*

/** Kalori ve protein değerleri genel besin tablolarına dayanan yaklaşık değerlerdir. */
object MealLibrary {
    private fun m(id: String, slot: MealSlot, name: String, items: List<String>, kcal: Int, protein: Int, vararg tags: String) =
        MealOption(id, slot, name, items, kcal, protein, tags.toSet())

    val options: List<MealOption> = listOf(
        m("b1", BREAKFAST, "Klasik yumurtalı kahvaltı", listOf("3 yumurta (haşlanmış veya omlet)", "2 dilim tam buğday ekmek", "1 dilim beyaz peynir (30 g)", "Domates, salatalık, 5 zeytin"), 620, 34, "ekonomik", "vejetaryen"),
        m("b2", BREAKFAST, "Yulaflı protein kase", listOf("80 g yulaf", "250 ml süt", "1 muz", "1 yemek kaşığı fıstık ezmesi", "Tarçın"), 640, 25, "ekonomik", "vejetaryen", "laktoz"),
        m("b3", BREAKFAST, "Menemen + yoğurt", listOf("3 yumurtalı menemen (az yağ)", "1 dilim ekmek", "150 g yoğurt"), 560, 30, "ekonomik", "vejetaryen", "laktoz"),
        m("b4", BREAKFAST, "Lor/peynirli tost", listOf("2 dilim tam buğday ekmek", "60 g kaşar veya lor", "2 haşlanmış yumurta", "1 bardak ayran"), 600, 38, "vejetaryen", "laktoz"),
        m("s1a", SNACK1, "Yoğurt + meyve + ceviz", listOf("200 g yoğurt (veya süzme)", "1 orta meyve", "3 ceviz içi"), 330, 15, "ekonomik", "vejetaryen", "laktoz"),
        m("s1b", SNACK1, "Muz + fıstık ezmesi", listOf("1 muz", "1 yemek kaşığı fıstık ezmesi", "1 bardak süt"), 360, 13, "ekonomik", "vejetaryen", "laktoz"),
        m("s1c", SNACK1, "Kuruyemiş + kuru meyve", listOf("30 g çiğ badem/fındık", "3 kuru kayısı veya hurma"), 260, 6, "vejetaryen"),
        m("l1", LUNCH, "Tavuk + bulgur pilavı", listOf("150 g ızgara tavuk göğsü/but", "1,5 kepçe bulgur pilavı (~200 g)", "Büyük salata + zeytinyağı"), 720, 48, "et"),
        m("l2", LUNCH, "Kuru fasulye/nohut + pilav", listOf("1,5 kepçe kuru fasulye veya nohut", "1 kepçe pilav", "1 kase yoğurt veya cacık"), 700, 30, "ekonomik", "vejetaryen"),
        m("l3", LUNCH, "Ton balıklı salata + ekmek", listOf("1 kutu ton balığı (süzülmüş)", "Bol yeşillik, mısır, fasulye", "2 dilim ekmek"), 560, 38, "balık"),
        m("l4", LUNCH, "Kıymalı makarna", listOf("100 g (çiğ) makarna", "120 g az yağlı kıyma, domates sos", "Salata"), 760, 42, "et"),
        m("s2a", SNACK2, "Antrenman öncesi: ekmek + peynir + meyve", listOf("1 dilim ekmek", "1 dilim peynir", "1 meyve"), 300, 12, "ekonomik", "vejetaryen"),
        m("s2b", SNACK2, "Ayran + simit/ekmek", listOf("1 büyük ayran", "Yarım simit veya 2 dilim ekmek"), 320, 12, "ekonomik", "vejetaryen", "laktoz"),
        m("s2c", SNACK2, "Protein shake (isteğe bağlı)", listOf("1 ölçek protein tozu (30 g)", "300 ml süt veya su", "1 muz"), 330, 30, "takviye", "laktoz"),
        m("d1", DINNER, "Somon/uskumru + patates", listOf("150 g fırın balık (somon, uskumru, levrek)", "200 g fırın patates", "Közlenmiş sebze"), 680, 38, "balık"),
        m("d2", DINNER, "Kırmızı mercimek + yumurta", listOf("1 kase mercimek çorbası", "Mercimek köftesi veya 2 yumurta", "Salata, 1 dilim ekmek"), 620, 30, "ekonomik", "vejetaryen"),
        m("d3", DINNER, "Izgara köfte + pirinç", listOf("150 g ızgara köfte", "1 kepçe pirinç pilavı", "Piyaz veya salata"), 760, 40, "et"),
        m("d4", DINNER, "Tavuklu sebze sote + yoğurt", listOf("150 g tavuk", "Biber, kabak, mantar", "1 kepçe bulgur", "150 g yoğurt"), 640, 45, "et", "laktoz"),
        m("d5", DINNER, "Fırın tavuk but + makarna", listOf("2 tavuk but (derisiz)", "80 g (çiğ) makarna", "Salata"), 700, 45, "et", "ekonomik"),
        m("p1", PRE_SLEEP, "Süzme yoğurt / lor", listOf("200 g süzme yoğurt veya lor", "1 tatlı kaşığı bal (isteğe bağlı)"), 220, 20, "ekonomik", "vejetaryen", "laktoz"),
        m("p2", PRE_SLEEP, "Süt + 2 haşlanmış yumurta", listOf("1 bardak süt", "2 haşlanmış yumurta"), 280, 20, "ekonomik", "vejetaryen", "laktoz"),
    )

    val foods: List<FoodItem> = listOf(
        FoodItem("Yumurta", "1 adet (50 g)", 72, 6.3, 0.4, 5.0),
        FoodItem("Yoğurt (tam yağlı)", "200 g", 122, 7.0, 9.4, 6.4),
        FoodItem("Süzme yoğurt", "200 g", 190, 18.0, 8.0, 9.0),
        FoodItem("Süt (tam yağlı)", "250 ml", 155, 8.0, 12.0, 8.5),
        FoodItem("Ayran", "300 ml", 110, 5.5, 7.5, 6.0),
        FoodItem("Beyaz peynir", "30 g", 80, 5.0, 0.5, 6.5),
        FoodItem("Lor peyniri", "100 g", 100, 12.0, 3.0, 4.0),
        FoodItem("Kaşar peyniri", "30 g", 110, 7.5, 0.5, 8.5),
        FoodItem("Tavuk göğsü (pişmiş)", "150 g", 248, 46.0, 0.0, 5.4),
        FoodItem("Tavuk but (derisiz, pişmiş)", "150 g", 280, 39.0, 0.0, 13.0),
        FoodItem("Kırmızı et / dana (pişmiş)", "150 g", 330, 39.0, 0.0, 19.0),
        FoodItem("Köfte (ızgara)", "150 g", 360, 30.0, 6.0, 24.0),
        FoodItem("Ton balığı (süzülmüş)", "1 kutu (~120 g)", 140, 30.0, 0.0, 1.5),
        FoodItem("Somon (pişmiş)", "150 g", 310, 33.0, 0.0, 19.0),
        FoodItem("Uskumru / hamsi (pişmiş)", "150 g", 300, 30.0, 0.0, 20.0),
        FoodItem("Kırmızı mercimek (pişmiş)", "1 kase (200 g)", 230, 18.0, 40.0, 0.8),
        FoodItem("Nohut (pişmiş)", "1 kepçe (150 g)", 245, 13.0, 41.0, 4.0),
        FoodItem("Kuru fasulye yemeği", "1 kepçe (250 g)", 280, 14.0, 40.0, 7.0),
        FoodItem("Bulgur pilavı", "1 kepçe (150 g)", 220, 6.0, 40.0, 4.0),
        FoodItem("Pirinç pilavı", "1 kepçe (150 g)", 250, 4.5, 48.0, 5.0),
        FoodItem("Makarna (pişmiş)", "200 g", 300, 11.0, 60.0, 2.0),
        FoodItem("Tam buğday ekmek", "1 dilim (30 g)", 75, 3.5, 13.0, 1.0),
        FoodItem("Yulaf", "50 g", 190, 6.5, 33.0, 3.5),
        FoodItem("Patates (fırın)", "200 g", 180, 4.0, 40.0, 0.2),
        FoodItem("Muz", "1 orta", 105, 1.3, 27.0, 0.4),
        FoodItem("Elma", "1 orta", 95, 0.5, 25.0, 0.3),
        FoodItem("Portakal", "1 orta", 65, 1.3, 15.0, 0.2),
        FoodItem("Hurma", "3 adet", 70, 0.6, 18.0, 0.0),
        FoodItem("Badem / fındık", "30 g", 175, 6.0, 5.0, 15.0),
        FoodItem("Ceviz", "3 tam (~25 g)", 165, 3.8, 3.5, 16.0),
        FoodItem("Fıstık ezmesi", "1 yemek kaşığı (16 g)", 95, 4.0, 3.0, 8.0),
        FoodItem("Zeytinyağı", "1 yemek kaşığı", 120, 0.0, 0.0, 14.0),
        FoodItem("Zeytin", "5 adet", 45, 0.3, 1.0, 4.5),
        FoodItem("Salata (yağsız)", "1 büyük kase", 40, 2.0, 8.0, 0.3),
        FoodItem("Protein tozu (whey)", "1 ölçek (30 g)", 120, 24.0, 3.0, 1.5),
        FoodItem("Simit", "1 adet", 300, 9.0, 55.0, 5.0),
    )
}
