package com.ninetymax.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ninetymax.app.data.*
import com.ninetymax.app.domain.*
import com.ninetymax.app.ui.LocalRepo
import com.ninetymax.app.ui.appData
import com.ninetymax.app.ui.components.*
import java.io.File
import java.time.LocalDate

val MEASURES = listOf("shoulders" to "Omuz çevresi", "chest" to "Göğüs", "waist" to "Bel (göbek hizası)", "arm" to "Kol (pazu)", "thigh" to "Uyluk", "neck" to "Boyun")
val EVAL_RATINGS = listOf("energy" to "Enerji", "confidence" to "Özgüven", "skin" to "Cilt", "hair" to "Saç", "posture" to "Duruş", "fitness" to "Kondisyon")

@Composable
fun ProgressScreen() {
    TabbedPage("Gelişim", listOf("Özet", "Kilo & ölçü", "Fotoğraflar", "Değerlendirme")) { tab ->
        when (tab) { 0 -> Summary(); 1 -> WeightMeasures(); 2 -> PhotosTab(); else -> Evaluations() }
    }
}

@Composable
private fun Summary() {
    val data = appData()
    val date = today()
    val history = DailyTasks.completionHistory(data, date)
    val avg = history.map { it.second }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val current = Nutrition.currentWeight(data)
    val start = data.profile.startWeightKg
    val sleepAvg = data.sleep.takeLast(7).map { sleepHours(it.bed, it.wake) }.average().takeIf { !it.isNaN() }
    val day = Program.dayOf(data, date)
    AdaptiveColumns(left = {
        AppCard {
            SectionHeader("Program", Icons.Rounded.Flag)
            KeyValue("Gün", "${day.coerceIn(0, PROGRAM_DAYS)} / $PROGRAM_DAYS")
            LinearProgressIndicator(progress = { day.coerceIn(0, PROGRAM_DAYS) / PROGRAM_DAYS.toFloat() }, modifier = Modifier.fillMaxWidth())
            KeyValue("Ortalama görev tamamlama", "%$avg")
            KeyValue("Tamamlanan antrenman", data.workoutLogs.count { it.completed }.toString())
        }
        AppCard {
            SectionHeader("Günlük tamamlama (son ${history.takeLast(30).size} gün)", Icons.Rounded.BarChart)
            BarChart(history.takeLast(30).map { it.second })
            Muted("Eksik günler normal. Önemli olan eğilim: ortalamanın yükselmesi.")
        }
    }, right = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Güncel kilo", "${current.fmt()} kg", Icons.Rounded.MonitorWeight, Modifier.weight(1f))
            StatTile("Değişim", (if (current - start >= 0) "+" else "") + (current - start).fmt() + " kg", Icons.Rounded.TrendingUp, Modifier.weight(1f))
            StatTile("Hedef", "${data.profile.targetWeightKg.fmt()} kg", Icons.Rounded.Flag, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Uyku ort.", sleepAvg?.let { "${it.fmt()} sa" } ?: "—", Icons.Rounded.Bedtime, Modifier.weight(1f))
            StatTile("Fotoğraf", data.photos.size.toString(), Icons.Rounded.PhotoLibrary, Modifier.weight(1f))
            StatTile("Ölçüm", data.measurements.size.toString(), Icons.Rounded.Straighten, Modifier.weight(1f))
        }
        AppCard {
            SectionHeader("Kilo trendi", Icons.Rounded.ShowChart)
            LineChart(data.weights.sortedBy { it.date }.map { it.kg.toFloat() }, unit = " kg")
        }
        val w = Nutrition.weekly(data, date)
        AppCard { SectionHeader("Bu hafta", Icons.Rounded.Lightbulb); BulletList(w.suggestions.take(3)) }
    })
}

@Composable
private fun WeightMeasures() {
    val data = appData()
    val repo = LocalRepo.current
    var weight by remember { mutableStateOf("") }
    var wDate by remember { mutableStateOf(today()) }
    val values = remember { mutableStateMapOf<String, String>() }
    var chartKey by remember { mutableStateOf(MEASURES.first().first) }
    AdaptiveColumns(left = {
        AppCard {
            SectionHeader("Kilo ekle", Icons.Rounded.MonitorWeight)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField("Kilo", weight, { weight = it }, Modifier.weight(1f), suffix = "kg")
                DateButton("Tarih", wDate.iso(), { wDate = it })
            }
            Button(onClick = {
                val kg = weight.toDoubleTr()?.takeIf { it in 30.0..250.0 } ?: return@Button
                repo.update { d ->
                    val u = d.copy(weights = d.weights.filterNot { it.date == wDate.iso() } + WeightEntry(newId(), wDate.iso(), kg))
                    DailyTasks.toggle(u, wDate, "r:am_weigh", true)
                }
                weight = ""
            }) { Text("Kaydet") }
            LineChart(data.weights.sortedBy { it.date }.map { it.kg.toFloat() }, unit = " kg")
        }
        AppCard {
            SectionHeader("Kilo kayıtları", Icons.Rounded.History)
            if (data.weights.isEmpty()) Muted("Başlangıç: ${data.profile.startWeightKg.fmt()} kg")
            data.weights.sortedByDescending { it.date }.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.date.toDateOrNull()?.longTr() ?: e.date, Modifier.weight(1f))
                    Text("${e.kg.fmt()} kg", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { repo.update { d -> d.copy(weights = d.weights.filterNot { it.id == e.id }) } }) { Icon(Icons.Rounded.Delete, "Sil") }
                }
            }
        }
    }, right = {
        AppCard {
            SectionHeader("Vücut ölçüleri (cm)", Icons.Rounded.Straighten)
            Muted("Mezurayı sıkmadan, sabah aynı koşullarda ölç. 2 haftada bir yeterli.")
            MEASURES.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (k, label) -> NumberField(label, values[k] ?: "", { values[k] = it }, Modifier.weight(1f), suffix = "cm") }
                }
            }
            Button(onClick = {
                val map = values.mapNotNull { (k, v) -> v.toDoubleTr()?.let { k to it } }.toMap()
                if (map.isEmpty()) return@Button
                repo.update { d -> d.copy(measurements = d.measurements.filterNot { it.date == today().iso() } + MeasurementEntry(newId(), today().iso(), map)) }
                values.clear()
            }) { Text("Bugünün ölçülerini kaydet") }
        }
        AppCard {
            SectionHeader("Ölçü grafiği", Icons.Rounded.ShowChart)
            ChipSelector(MEASURES.map { it.first }, chartKey, { k -> MEASURES.first { it.first == k }.second }) { chartKey = it }
            LineChart(data.measurements.sortedBy { it.date }.mapNotNull { it.values[chartKey]?.toFloat() }, unit = " cm")
            val first = data.measurements.minByOrNull { it.date }; val last = data.measurements.maxByOrNull { it.date }
            if (first != null && last != null && first.id != last.id) {
                Text("İlk → son", style = MaterialTheme.typography.labelLarge)
                MEASURES.forEach { (k, label) ->
                    val a = first.values[k]; val b = last.values[k]
                    if (a != null && b != null) KeyValue(label, "${a.fmt()} → ${b.fmt()} (${if (b - a >= 0) "+" else ""}${(b - a).fmt()})")
                }
            }
        }
        data.measurements.sortedByDescending { it.date }.forEach { m ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.date.toDateOrNull()?.longTr() ?: m.date, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { repo.update { d -> d.copy(measurements = d.measurements.filterNot { it.id == m.id }) } }) { Icon(Icons.Rounded.Delete, "Sil") }
                }
                Muted(m.values.entries.joinToString(" · ") { (k, v) -> "${MEASURES.firstOrNull { it.first == k }?.second ?: k}: ${v.fmt()}" })
            }
        }
    })
}

@Composable
private fun PhotosTab() {
    val data = appData()
    val repo = LocalRepo.current
    val context = LocalContext.current
    var angle by remember { mutableStateOf(PhotoAngle.FRONT) }
    var pDate by remember { mutableStateOf(today()) }
    var viewing by remember { mutableStateOf<PhotoRecord?>(null) }
    var compareA by remember { mutableStateOf<String?>(null) }
    var compareB by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val f = Photos.importImage(context, uri, repo.photoDir)
            if (f == null) error = true
            else repo.update { d -> d.copy(photos = d.photos + PhotoRecord(newId(), pDate.iso(), angle, f)) }
        }
    }
    val dates = data.photos.map { it.date }.distinct().sortedDescending()
    AdaptiveColumns(left = {
        AppCard {
            SectionHeader("Fotoğraf ekle", Icons.Rounded.AddAPhoto)
            Muted("Aynı ışık, aynı mesafe ve aynı duruşla çek. Ön, yan ve arka fotoğraflar 1., 30., 60. ve 90. günlerde önerilir. Fotoğraflar sadece cihazında saklanır.")
            ChipSelector(PhotoAngle.entries, angle, { it.label }) { angle = it }
            DateButton("Tarih", pDate.iso(), { pDate = it })
            Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Rounded.PhotoLibrary, null); Text(" Galeriden seç") }
            if (error) Text("Görsel okunamadı, başka bir dosya dene.", color = MaterialTheme.colorScheme.error)
        }
        if (dates.size >= 2) AppCard {
            SectionHeader("Karşılaştır", Icons.Rounded.Compare)
            val a = compareA ?: dates.last(); val b = compareB ?: dates.first()
            Text("Önce", style = MaterialTheme.typography.labelLarge); ChipSelector(dates, a, { it.toDateOrNull()?.shortTr() ?: it }) { compareA = it }
            Text("Sonra", style = MaterialTheme.typography.labelLarge); ChipSelector(dates, b, { it.toDateOrNull()?.shortTr() ?: it }) { compareB = it }
            PhotoAngle.entries.forEach { ang ->
                val pa = data.photos.firstOrNull { it.date == a && it.angle == ang }
                val pb = data.photos.firstOrNull { it.date == b && it.angle == ang }
                if (pa != null && pb != null) {
                    Text(ang.label, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PhotoThumb(File(repo.photoDir, pa.fileName), Modifier.weight(1f).aspectRatio(0.75f))
                        PhotoThumb(File(repo.photoDir, pb.fileName), Modifier.weight(1f).aspectRatio(0.75f))
                    }
                }
            }
        }
    }, right = {
        if (dates.isEmpty()) EmptyState("Henüz fotoğraf yok.", Icons.Rounded.PhotoCamera)
        dates.forEach { d ->
            AppCard {
                val day = Program.dayOf(data, d.toDate())
                Text("${d.toDate().longTr()} · Gün $day", style = MaterialTheme.typography.titleSmall)
                val list = data.photos.filter { it.date == d }
                list.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { ph ->
                            Column(Modifier.weight(1f).clickable { viewing = ph }) {
                                PhotoThumb(File(repo.photoDir, ph.fileName), Modifier.fillMaxWidth().aspectRatio(0.75f))
                                Muted(ph.angle.label)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    })
    viewing?.let { ph ->
        AlertDialog(onDismissRequest = { viewing = null }, title = { Text("${ph.angle.label} · ${ph.date}") }, text = {
            PhotoThumb(File(repo.photoDir, ph.fileName), Modifier.fillMaxWidth().aspectRatio(0.75f), ContentScale.Fit)
        }, confirmButton = { TextButton(onClick = { viewing = null }) { Text("Kapat") } }, dismissButton = {
            TextButton(onClick = {
                File(repo.photoDir, ph.fileName).delete()
                repo.update { d -> d.copy(photos = d.photos.filterNot { it.id == ph.id }) }
                viewing = null
            }) { Text("Sil") }
        })
    }
}

private fun nearestWeight(data: AppData, date: LocalDate): Double? =
    data.weights.minByOrNull { kotlin.math.abs(it.date.toDate().toEpochDay() - date.toEpochDay()) }?.kg

@Composable
private fun Evaluations() {
    val data = appData()
    val repo = LocalRepo.current
    var editing by remember { mutableStateOf<Int?>(null) }
    val todayDay = Program.dayOf(data, today())
    AdaptiveColumns(left = {
        Muted("Başlangıç, 30., 60. ve 90. günlerde kendini değerlendir. Puanlar ve otomatik veriler yan yana karşılaştırılır.")
        CHECKPOINT_DAYS.forEach { cp ->
            val e = data.evaluations.firstOrNull { it.day == cp }
            val date = Program.dateOfDay(data, cp, today())
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (cp == 1) "Başlangıç (Gün 1)" else "Gün $cp değerlendirmesi", style = MaterialTheme.typography.titleMedium)
                        Muted(date.longTr() + when { e != null -> " · tamamlandı"; todayDay >= cp -> " · bekliyor"; else -> " · ${cp - todayDay} gün sonra" })
                    }
                    FilledTonalButton(onClick = { editing = cp }) { Text(if (e == null) "Doldur" else "Düzenle") }
                }
                if (e != null) {
                    EVAL_RATINGS.forEach { (k, label) -> e.ratings[k]?.let { KeyValue(label, "★".repeat(it)) } }
                    if (e.notes.isNotBlank()) Muted(e.notes)
                }
            }
        }
    }, right = {
        AppCard {
            SectionHeader("Karşılaştırma", Icons.Rounded.TableChart)
            val evals = data.evaluations.sortedBy { it.day }
            if (evals.isEmpty()) Muted("Değerlendirme yaptıkça burada tablo oluşur.")
            else {
                Row { Text("", Modifier.weight(1.4f)); evals.forEach { Text("G${it.day}", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge) } }
                Row { Text("Kilo", Modifier.weight(1.4f)); evals.forEach { ev -> Text(nearestWeight(data, ev.date.toDate())?.fmt() ?: "—", Modifier.weight(1f)) } }
                Row { Text("Antrenman", Modifier.weight(1.4f)); evals.forEach { ev -> Text(data.workoutLogs.count { it.completed && it.date <= ev.date }.toString(), Modifier.weight(1f)) } }
                MEASURES.take(3).forEach { (k, label) ->
                    Row { Text(label, Modifier.weight(1.4f)); evals.forEach { ev -> Text(data.measurements.filter { it.date <= ev.date }.maxByOrNull { it.date }?.values?.get(k)?.fmt() ?: "—", Modifier.weight(1f)) } }
                }
                EVAL_RATINGS.forEach { (k, label) ->
                    Row { Text(label, Modifier.weight(1.4f)); evals.forEach { ev -> Text(ev.ratings[k]?.toString() ?: "—", Modifier.weight(1f)) } }
                }
            }
        }
    })
    editing?.let { cp ->
        val e = data.evaluations.firstOrNull { it.day == cp }
        val ratings = remember(cp) { mutableStateMapOf<String, Int>().apply { putAll(e?.ratings ?: emptyMap()) } }
        var notes by remember(cp) { mutableStateOf(e?.notes ?: "") }
        FormDialog("Gün $cp değerlendirmesi", { editing = null }, onSave = {
            val ev = Evaluation(cp, today().iso(), ratings.toMap(), notes)
            repo.update { d -> DailyTasks.toggle(d.copy(evaluations = d.evaluations.filterNot { it.day == cp } + ev), today(), "p:eval$cp", true) }
        }) {
            Muted("Fotoğraf ve ölçüleri Gelişim sekmelerinden eklemeyi unutma.")
            EVAL_RATINGS.forEach { (k, label) -> RatingRow(label, ratings[k] ?: 0) { ratings[k] = it } }
            TextInput("Notlar: neler değişti, neyi değiştirmek istiyorsun?", notes, { notes = it }, singleLine = false)
        }
    }
}
