package com.ninetymax.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ninetymax.app.data.*
import com.ninetymax.app.domain.Exercises
import com.ninetymax.app.seed.Guide
import com.ninetymax.app.seed.Guides
import com.ninetymax.app.ui.LocalNav
import com.ninetymax.app.ui.LocalRepo
import com.ninetymax.app.ui.Routes
import com.ninetymax.app.ui.appData
import com.ninetymax.app.ui.components.*
import java.time.Duration
import java.time.LocalTime

@Composable
fun GuideCards(guides: List<Guide>) = guides.forEach { g -> AppCard { SectionHeader(g.title, Icons.Rounded.MenuBook); BulletList(g.points) } }

@Composable
private fun AreaProducts(categories: Set<ProductCategory>) {
    val data = appData()
    val list = data.products.filter { it.category in categories && (it.status == ProductStatus.IN_USE || it.status == ProductStatus.BOUGHT || it.favorite) }
    AppCard {
        SectionHeader("Ürünlerim", Icons.Rounded.ShoppingBag)
        if (list.isEmpty()) Muted("Bu alanda kullandığın veya favorilediğin ürün yok. Ürünler > Katalog'dan ekleyebilirsin.")
    }
    list.forEach { ProductRow(it) }
}

@Composable
fun AppearanceLog(area: Area) {
    val data = appData()
    val repo = LocalRepo.current
    var score by remember { mutableIntStateOf(3) }
    var note by remember { mutableStateOf("") }
    val entries = data.appearance.filter { it.area == area }.sortedBy { it.date }
    AppCard {
        SectionHeader("${area.label} görünüm takibi", Icons.Rounded.Insights)
        Muted("Haftada bir kendine 1-5 arası puan ver. Fotoğraf için Gelişim > Fotoğraflar.")
        RatingRow("Bugünkü puan", score) { score = it }
        OutlinedTextField(note, { note = it }, label = { Text("Not (ör. kuruluk, sivilce, bukle tanımı)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            repo.update { d -> d.copy(appearance = d.appearance.filterNot { it.area == area && it.date == today().iso() } + AppearanceEntry(newId(), today().iso(), area, score, note)) }
            note = ""
        }) { Text("Kaydet") }
        if (entries.size >= 2) LineChart(entries.map { it.score.toFloat() }, Modifier.fillMaxWidth().height(100.dp))
        entries.takeLast(5).reversed().forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${e.date.toDateOrNull()?.shortTr() ?: e.date} · ${"★".repeat(e.score)}", Modifier.weight(1f))
                if (e.note.isNotBlank()) Muted(e.note, Modifier.weight(1f), maxLines = 1)
                IconButton(onClick = { repo.update { d -> d.copy(appearance = d.appearance.filterNot { it.id == e.id }) } }) { Icon(Icons.Rounded.Delete, "Sil") }
            }
        }
    }
}

@Composable
fun CareScreen() {
    TabbedPage("Bakım", listOf("Cilt", "Saç", "Sakal", "Ağız & diş", "Vücut")) { tab ->
        when (tab) {
            0 -> AdaptiveColumns(left = {
                RoutineSection(Area.SKIN)
                AppearanceLog(Area.SKIN)
            }, right = {
                InfoBanner("Cildinin toleransını gözlemle: tek seferde bir yeni ürün/aktif. Aktif adımlar varsayılan olarak kapalıdır.", Icons.Rounded.Spa)
                GuideCards(Guides.skin)
                AreaProducts(setOf(ProductCategory.CLEANSER, ProductCategory.MOISTURIZER, ProductCategory.SUNSCREEN, ProductCategory.SKIN_TREATMENT))
            })
            1 -> AdaptiveColumns(left = {
                RoutineSection(Area.HAIR)
                AppearanceLog(Area.HAIR)
            }, right = {
                GuideCards(Guides.hair)
                AreaProducts(setOf(ProductCategory.HAIR, ProductCategory.CURL))
            })
            2 -> AdaptiveColumns(left = {
                RoutineSection(Area.BEARD)
                AppearanceLog(Area.BEARD)
            }, right = {
                GuideCards(Guides.beard)
                AreaProducts(setOf(ProductCategory.BEARD))
            })
            3 -> AdaptiveColumns(left = {
                RoutineSection(Area.ORAL)
                DentistCard()
                AppearanceLog(Area.ORAL)
            }, right = {
                GuideCards(Guides.oral)
                AreaProducts(setOf(ProductCategory.ORAL))
            })
            else -> AdaptiveColumns(left = {
                RoutineSection(Area.BODY, "Vücut, el & hijyen rutini")
            }, right = {
                AreaProducts(setOf(ProductCategory.BODY, ProductCategory.HAND_NAIL, ProductCategory.FRAGRANCE))
            })
        }
    }
}

@Composable
private fun DentistCard() {
    val data = appData()
    val repo = LocalRepo.current
    AppCard {
        SectionHeader("Diş hekimi kontrolü", Icons.Rounded.EventAvailable)
        val next = data.settings.dentistNextDate?.toDateOrNull()
        Muted(next?.let { "Sonraki kontrol: ${it.longTr()}" } ?: "Kontrol tarihi girilmedi. Genel öneri 6 ayda bir.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateButton("Tarih", data.settings.dentistNextDate, { d -> repo.update { it.copy(settings = it.settings.copy(dentistNextDate = d.iso())) } })
            TextButton(onClick = { repo.update { it.copy(settings = it.settings.copy(dentistNextDate = today().plusMonths(6).iso())) } }) { Text("6 ay sonra") }
        }
    }
}

@Composable
fun BodyScreen() {
    TabbedPage("Duruş & Uyku", listOf("Duruş & mobilite", "Uyku", "Aktivite")) { tab ->
        when (tab) { 0 -> PostureTab(); 1 -> SleepTab(); else -> ActivityTab() }
    }
}

@Composable
private fun PostureTab() {
    val nav = LocalNav.current
    AdaptiveColumns(left = {
        RoutineSection(Area.POSTURE)
        AppCard {
            SectionHeader("Mobilite & esneme hareketleri", Icons.Rounded.SelfImprovement)
            Exercises.all.filter { it.kind == com.ninetymax.app.domain.ExKind.MOBILITY }.forEach { ex ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(ex.name, style = MaterialTheme.typography.bodyMedium); Muted(ex.steps.first(), maxLines = 1) }
                    IconButton(onClick = { nav.navigate(Routes.exercise(ex.id)) }) { Icon(Icons.Rounded.ChevronRight, "Aç") }
                }
            }
        }
    }, right = { GuideCards(Guides.posture) })
}

fun sleepHours(bed: String, wake: String): Double {
    val b = bed.toTimeOrNull() ?: return 0.0
    val w = wake.toTimeOrNull() ?: return 0.0
    var mins = Duration.between(b, w).toMinutes()
    if (mins <= 0) mins += 24 * 60
    return mins / 60.0
}

@Composable
private fun SleepTab() {
    val data = appData()
    val repo = LocalRepo.current
    val s = data.settings
    var bed by remember { mutableStateOf(s.bedTime) }
    var wake by remember { mutableStateOf(s.wakeTime) }
    var quality by remember { mutableIntStateOf(3) }
    val entries = data.sleep.sortedBy { it.date }
    AdaptiveColumns(left = {
        AppCard {
            SectionHeader("Uyku hedefi", Icons.Rounded.Bedtime)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeButton("Yatış", s.bedTime, { v -> repo.update { it.copy(settings = it.settings.copy(bedTime = v)) } })
                TimeButton("Kalkış", s.wakeTime, { v -> repo.update { it.copy(settings = it.settings.copy(wakeTime = v)) } })
            }
            Muted("Hedef süre: ${sleepHours(s.bedTime, s.wakeTime).fmt()} saat")
        }
        AppCard {
            SectionHeader("Dün geceki uyku", Icons.Rounded.NightsStay)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeButton("Yattım", bed, { bed = it })
                TimeButton("Kalktım", wake, { wake = it })
            }
            RatingRow("Kalite", quality) { quality = it }
            Button(onClick = {
                repo.update { d -> d.copy(sleep = d.sleep.filterNot { it.date == today().iso() } + SleepEntry(newId(), today().iso(), bed, wake, quality)) }
            }) { Text("Kaydet (${sleepHours(bed, wake).fmt()} saat)") }
        }
        AppCard {
            SectionHeader("Uyku süresi", Icons.Rounded.ShowChart)
            val last = entries.takeLast(14)
            LineChart(last.map { sleepHours(it.bed, it.wake).toFloat() }, unit = " sa")
            if (last.isNotEmpty()) Muted("Son ${last.size} kayıt ortalaması: ${last.map { sleepHours(it.bed, it.wake) }.average().fmt()} saat")
        }
    }, right = {
        entries.reversed().take(10).forEach { e ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.date.toDateOrNull()?.longTr() ?: e.date, style = MaterialTheme.typography.bodyMedium)
                        Muted("${e.bed} → ${e.wake} · ${sleepHours(e.bed, e.wake).fmt()} sa · ${"★".repeat(e.quality)}")
                    }
                    IconButton(onClick = { repo.update { d -> d.copy(sleep = d.sleep.filterNot { it.id == e.id }) } }) { Icon(Icons.Rounded.Delete, "Sil") }
                }
            }
        }
        RoutineSection(Area.SLEEP)
        GuideCards(Guides.sleep)
    })
}

@Composable
private fun ActivityTab() {
    val data = appData()
    val repo = LocalRepo.current
    var type by remember { mutableStateOf(ActivityType.WALK) }
    var minutes by remember { mutableStateOf("30") }
    var steps by remember { mutableStateOf("") }
    val week = (0..6).map { today().minusDays(it.toLong()).iso() }
    val weekEntries = data.activity.filter { it.date in week }
    AdaptiveColumns(left = {
        AppCard {
            SectionHeader("Aktivite ekle", Icons.Rounded.DirectionsWalk)
            ChipSelector(ActivityType.entries, type, { it.label }) { type = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Süre", minutes, { minutes = it }, Modifier.weight(1f), suffix = "dk", decimal = false)
                NumberField("Adım (isteğe bağlı)", steps, { steps = it }, Modifier.weight(1f), decimal = false)
            }
            Button(onClick = {
                val m = minutes.toIntOrNull() ?: return@Button
                repo.update { d ->
                    val updated = d.copy(activity = d.activity + ActivityEntry(newId(), today().iso(), type, m, steps.toIntOrNull()))
                    if (type == ActivityType.WALK) com.ninetymax.app.domain.DailyTasks.toggle(updated, today(), "r:day_walk", true) else updated
                }
            }) { Text("Kaydet") }
        }
        AppCard {
            SectionHeader("Bu hafta", Icons.Rounded.Insights)
            KeyValue("Toplam süre", "${weekEntries.sumOf { it.minutes }} dk")
            KeyValue("Yürüyüş", "${weekEntries.filter { it.type == ActivityType.WALK }.sumOf { it.minutes }} dk")
            KeyValue("Adım", weekEntries.mapNotNull { it.steps }.sum().toString() + " (hedef günlük ${data.settings.stepsTarget})")
            BarChart(week.reversed().map { d -> data.activity.filter { it.date == d }.sumOf { it.minutes } }, maxValue = 60)
        }
    }, right = {
        data.activity.sortedByDescending { it.date }.take(20).forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${e.type.label} · ${e.minutes} dk" + (e.steps?.let { " · $it adım" } ?: ""), style = MaterialTheme.typography.bodyMedium)
                    Muted(e.date)
                }
                IconButton(onClick = { repo.update { d -> d.copy(activity = d.activity.filterNot { it.id == e.id }) } }) { Icon(Icons.Rounded.Delete, "Sil") }
            }
        }
        if (data.activity.isEmpty()) EmptyState("Henüz aktivite kaydı yok.", Icons.Rounded.DirectionsWalk)
    })
}
