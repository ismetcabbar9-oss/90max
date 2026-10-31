package com.ninetymax.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ninetymax.app.data.*
import com.ninetymax.app.domain.*
import com.ninetymax.app.ui.LocalNav
import com.ninetymax.app.ui.LocalRepo
import com.ninetymax.app.ui.Page
import com.ninetymax.app.ui.Routes
import com.ninetymax.app.ui.appData
import com.ninetymax.app.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen() {
    TabbedPage("Antrenman", listOf("Bugün", "Haftalık plan", "Geçmiş", "Hareketler")) { tab ->
        when (tab) {
            0 -> WorkoutToday()
            1 -> WeeklyPlan()
            2 -> WorkoutHistory()
            else -> ExerciseLibrary()
        }
    }
}

@Composable
private fun WorkoutToday() {
    val data = appData()
    val nav = LocalNav.current
    val date = today()
    val day = Program.dayOf(data, date)
    val defaultTemplate = WorkoutPlan.templateForDate(data.workout, date.dayOfWeek.value)
    var selected by remember(defaultTemplate.id) { mutableStateOf(defaultTemplate.id) }
    val plan = WorkoutPlan.plan(data, selected, day)
    AdaptiveColumns(
        left = {
            AppCard {
                SectionHeader(plan.phase.name, Icons.Rounded.Timeline)
                Muted(plan.phase.description)
                Text("Yoğunluk: ${plan.phase.rir} (setin sonunda yedekte kalan tekrar)", style = MaterialTheme.typography.bodySmall)
                Muted("Kademeli yükleme: Bir hareketin tüm setlerinde aralığın üst sınırına ulaştığında bir sonraki antrenmanda ağırlığı ~%5 artır veya daha zor varyasyona geç.")
            }
            AppCard {
                SectionHeader("Antrenman seç", Icons.Rounded.SwapHoriz)
                Muted("Plan bugün için: ${defaultTemplate.name}. İstersen başka bir şablon seçebilirsin.")
                ChipSelector(WorkoutPlan.templates.map { it.id }, selected, { WorkoutPlan.template(it).name.substringBefore(" ·") }) { selected = it }
            }
            AppCard {
                Text(plan.template.name, style = MaterialTheme.typography.titleLarge)
                Muted("${plan.template.focus} · ~${plan.template.estMinutes} dk")
                if (plan.template.isRest) InfoBanner("Dinlenme günü: kaslar antrenmanda yorulur, dinlenirken onarılıp güçlenir. Hafif yürüyüş ve esneme yeterli.")
                Button(onClick = { nav.navigate(Routes.session(plan.template.id)) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PlayArrow, null); Text(" Başlat ve kaydet")
                }
                Muted("Isınma: 5 dk tempolu yürüyüş/ip + ilk harekette 2 hafif ısınma seti.")
            }
        },
        right = {
            plan.exercises.forEach { pe -> PlannedExerciseCard(pe) }
        },
    )
}

@Composable
fun PlannedExerciseCard(pe: PlannedExercise) {
    val nav = LocalNav.current
    AppCard(onClick = { nav.navigate(Routes.exercise(pe.exercise.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pe.role + if (pe.priority) " · öncelikli" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(pe.exercise.name, style = MaterialTheme.typography.titleMedium)
                Muted("${pe.sets} set × ${pe.reps}" + if (pe.restSec > 0) " · dinlenme ${pe.restSec} sn" else "")
                Muted(pe.exercise.muscles.joinToString { it.label }, maxLines = 1)
            }
            BodyMap(pe.exercise.muscles, Modifier.size(width = 84.dp, height = 70.dp))
        }
    }
}

@Composable
private fun WeeklyPlan() {
    val data = appData()
    val repo = LocalRepo.current
    var editSlot by remember { mutableStateOf<Pair<String, Int>?>(null) }
    AdaptiveColumns(
        left = {
            AppCard {
                SectionHeader("Ekipmanım", Icons.Rounded.Handyman)
                Muted("Program sadece sahip olduğun ekipmana göre hareket seçer. Pahalı ekipman zorunlu değil; vücut ağırlığı her zaman açık.")
                MultiChipSelector(Equip.entries.filter { it != Equip.BODYWEIGHT }, data.settings.equipment, { it.label }) { e ->
                    repo.update { d ->
                        val s = d.settings.equipment
                        d.copy(settings = d.settings.copy(equipment = if (e in s) s - e else s + e))
                    }
                }
            }
            AppCard {
                SectionHeader("Haftalık düzen", Icons.Rounded.CalendarMonth)
                (1..7).forEach { wd ->
                    var open by remember { mutableStateOf(false) }
                    val t = WorkoutPlan.template(data.workout.weekPlan.getOrElse(wd - 1) { "rest" })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(isoDayShort(wd), Modifier.width(48.dp), fontWeight = FontWeight.SemiBold)
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(t.name, maxLines = 1) }
                            DropdownMenu(open, { open = false }) {
                                WorkoutPlan.templates.forEach { tpl ->
                                    DropdownMenuItem(text = { Text(tpl.name) }, onClick = {
                                        open = false
                                        repo.update { d ->
                                            val list = d.workout.weekPlan.toMutableList().apply { while (size < 7) add("rest"); set(wd - 1, tpl.id) }
                                            d.copy(workout = d.workout.copy(weekPlan = list))
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { repo.update { it.copy(workout = WorkoutConfig()) } }) { Text("Varsayılan plana dön") }
            }
        },
        right = {
            Muted("Şablonları düzenle: bir harekete dokunup alternatif seç veya set/tekrar değiştir.")
            WorkoutPlan.templates.filter { !it.isRest }.forEach { tpl ->
                val plan = WorkoutPlan.plan(data, tpl.id, Program.dayOf(data, today()))
                AppCard {
                    Text(tpl.name, style = MaterialTheme.typography.titleMedium)
                    plan.exercises.forEachIndexed { i, pe ->
                        val custom = pe.slotKey in data.workout.slotOverrides || pe.slotKey in data.workout.slotParams
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(pe.exercise.name + if (custom) " ✎" else "", style = MaterialTheme.typography.bodyMedium)
                                Muted("${pe.role} · ${pe.sets}×${pe.reps}")
                            }
                            IconButton(onClick = { editSlot = tpl.id to i }) { Icon(Icons.Rounded.Edit, "Düzenle") }
                        }
                    }
                }
            }
        },
    )
    editSlot?.let { (tid, idx) -> SlotEditDialog(tid, idx) { editSlot = null } }
}

@Composable
private fun SlotEditDialog(templateId: String, index: Int, onDismiss: () -> Unit) {
    val data = appData()
    val repo = LocalRepo.current
    val plan = WorkoutPlan.plan(data, templateId, Program.dayOf(data, today()))
    val pe = plan.exercises[index]
    var exId by remember { mutableStateOf(pe.exercise.id) }
    var sets by remember { mutableStateOf(pe.sets.toString()) }
    var reps by remember { mutableStateOf(pe.reps) }
    var rest by remember { mutableStateOf(pe.restSec.toString()) }
    val choices = (listOf(pe.exercise) + pe.alternatives).distinctBy { it.id }
    FormDialog("${pe.role} düzenle", onDismiss, onSave = {
        repo.update { d ->
            val w = d.workout
            val overrides = if (exId == WorkoutPlan.resolveExercise(WorkoutPlan.template(templateId).slots[index], d.settings.equipment + Equip.BODYWEIGHT, null).id) w.slotOverrides - pe.slotKey else w.slotOverrides + (pe.slotKey to exId)
            val param = SlotParam(sets.toIntOrNull(), reps.ifBlank { null }, rest.toIntOrNull())
            d.copy(workout = w.copy(slotOverrides = overrides, slotParams = w.slotParams + (pe.slotKey to param)))
        }
    }) {
        Text("Hareket", style = MaterialTheme.typography.labelLarge)
        choices.forEach { ex ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = exId == ex.id, onClick = { exId = ex.id })
                Text(ex.name, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Set", sets, { sets = it }, Modifier.weight(1f), decimal = false)
            NumberField("Dinlenme", rest, { rest = it }, Modifier.weight(1f), suffix = "sn", decimal = false)
        }
        TextInput("Tekrar (ör. 8-12 veya 30 sn)", reps, { reps = it })
        TextButton(onClick = {
            repo.update { d -> d.copy(workout = d.workout.copy(slotOverrides = d.workout.slotOverrides - pe.slotKey, slotParams = d.workout.slotParams - pe.slotKey)) }
            onDismiss()
        }) { Text("Program varsayılanına dön") }
    }
}

@Composable
private fun WorkoutHistory() {
    val data = appData()
    val repo = LocalRepo.current
    val logs = data.workoutLogs.sortedByDescending { it.date }
    var confirm by remember { mutableStateOf<WorkoutLog?>(null) }
    val loggedExercises = data.workoutLogs.flatMap { it.sets }.filter { it.reps > 0 }.map { it.exerciseId }.distinct()
    var chartEx by remember { mutableStateOf(loggedExercises.firstOrNull()) }
    AdaptiveColumns(
        left = {
            AppCard {
                SectionHeader("Performans gelişimi", Icons.Rounded.ShowChart)
                if (loggedExercises.isEmpty()) Muted("Antrenman kaydettikçe her hareket için tahmini güç grafiği burada oluşur.")
                else {
                    ChipSelector(loggedExercises, chartEx, { Exercises.get(it)?.name ?: it }) { chartEx = it }
                    chartEx?.let { id ->
                        val series = data.workoutLogs.sortedBy { it.date }.mapNotNull { log ->
                            log.sets.filter { it.exerciseId == id && it.reps > 0 }
                                .maxOfOrNull { s -> if (s.weightKg > 0) WorkoutPlan.estimated1Rm(s.weightKg, s.reps) else s.reps.toDouble() }
                        }
                        val weighted = data.workoutLogs.any { l -> l.sets.any { it.exerciseId == id && it.weightKg > 0 } }
                        Muted(if (weighted) "Tahmini 1 tekrar maksimumu (kg)" else "En iyi set tekrarı")
                        LineChart(series.map { it.toFloat() })
                    }
                }
            }
            AppCard {
                SectionHeader("Özet", Icons.Rounded.EmojiEvents)
                KeyValue("Tamamlanan antrenman", data.workoutLogs.count { it.completed }.toString())
                KeyValue("Toplam set", data.workoutLogs.sumOf { l -> l.sets.count { it.reps > 0 } }.toString())
                KeyValue("Toplam hacim", "${data.workoutLogs.sumOf { l -> l.sets.sumOf { it.weightKg * it.reps } }.fmt(0)} kg")
            }
        },
        right = {
            if (logs.isEmpty()) EmptyState("Henüz antrenman kaydı yok.", Icons.Rounded.FitnessCenter)
            logs.forEach { log ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(WorkoutPlan.template(log.templateId).name, style = MaterialTheme.typography.titleSmall)
                            Muted("${log.date.toDateOrNull()?.longTr() ?: log.date} · ${log.minutes} dk")
                        }
                        IconButton(onClick = { confirm = log }) { Icon(Icons.Rounded.Delete, "Sil") }
                    }
                    log.sets.filter { it.reps > 0 }.groupBy { it.exerciseId }.forEach { (id, sets) ->
                        Muted("${Exercises.get(id)?.name ?: id}: " + sets.joinToString { (if (it.weightKg > 0) "${it.weightKg.fmt()}kg×" else "") + it.reps })
                    }
                    if (log.note.isNotBlank()) Muted("Not: ${log.note}")
                }
            }
        },
    )
    confirm?.let { log ->
        ConfirmDialog("Kaydı sil", "${log.date} tarihli antrenman kaydı silinsin mi?", onConfirm = {
            repo.update { d -> d.copy(workoutLogs = d.workoutLogs.filterNot { it.id == log.id }) }
        }, onDismiss = { confirm = null })
    }
}

@Composable
private fun ExerciseLibrary() {
    val nav = LocalNav.current
    val data = appData()
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<Muscle?>(null) }
    var onlyAvailable by remember { mutableStateOf(true) }
    val equipment = data.settings.equipment + Equip.BODYWEIGHT
    val list = Exercises.all.filter {
        (query.isBlank() || it.name.contains(query, ignoreCase = true)) &&
            (muscle == null || muscle in it.muscles) &&
            (!onlyAvailable || Exercises.available(it, equipment))
    }
    ScreenColumn {
        OutlinedTextField(query, { query = it }, label = { Text("Hareket ara") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChipSelector(listOf<Muscle?>(null) + Muscle.entries, muscle, { it?.label ?: "Tümü" }) { muscle = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(onlyAvailable, { onlyAvailable = it }); Text("  Sadece ekipmanıma uygun olanlar")
        }
        list.forEach { ex ->
            AppCard(onClick = { nav.navigate(Routes.exercise(ex.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, style = MaterialTheme.typography.titleSmall)
                        Muted(ex.muscles.joinToString { it.label })
                        Muted(if (ex.requires.isEmpty()) "Ekipmansız / evde" else ex.requires.joinToString { it.label })
                    }
                    BodyMap(ex.muscles, Modifier.size(width = 72.dp, height = 60.dp))
                }
            }
        }
        if (list.isEmpty()) EmptyState("Eşleşen hareket yok.")
    }
}

@Composable
fun ExerciseDetailScreen(id: String) {
    val ex = Exercises.get(id)
    val data = appData()
    val nav = LocalNav.current
    Page(ex?.name ?: "Hareket", back = true) {
        if (ex == null) { EmptyState("Hareket bulunamadı."); return@Page }
        val equipment = data.settings.equipment + Equip.BODYWEIGHT
        val alternatives = Exercises.all.filter { it.id != ex.id && it.kind == ex.kind && it.muscles.firstOrNull() == ex.muscles.firstOrNull() }
        val last = data.workoutLogs.sortedByDescending { it.date }.firstOrNull { l -> l.sets.any { it.exerciseId == id && it.reps > 0 } }
        AdaptiveColumns(
            left = {
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BodyMap(ex.muscles, Modifier.size(width = 240.dp, height = 200.dp)) }
                    Muted("Koyu renk: ana kas · açık renk: yardımcı kaslar (ön ve arka görünüm)")
                    KeyValue("Kaslar", ex.muscles.joinToString { it.label })
                    KeyValue("Ekipman", if (ex.requires.isEmpty()) "Gerekmez" else ex.requires.joinToString { it.label })
                    if (!Exercises.available(ex, equipment)) InfoBanner("Bu hareket için gereken ekipman sende işaretli değil. Aşağıdaki alternatiflere göz at.", Icons.Rounded.Warning, MaterialTheme.colorScheme.tertiaryContainer)
                }
                AppCard {
                    SectionHeader("Nasıl yapılır", Icons.Rounded.FormatListNumbered)
                    ex.steps.forEachIndexed { i, s ->
                        Row { Text("${i + 1}.", Modifier.width(24.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(s, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            },
            right = {
                AppCard { SectionHeader("Doğru form ipuçları", Icons.Rounded.CheckCircle); BulletList(ex.cues) }
                AppCard { SectionHeader("Sık yapılan hatalar", Icons.Rounded.ErrorOutline); BulletList(ex.mistakes) }
                last?.let { l ->
                    AppCard {
                        SectionHeader("Son performans", Icons.Rounded.History)
                        Muted(l.date)
                        Text(l.sets.filter { it.exerciseId == id && it.reps > 0 }.joinToString { (if (it.weightKg > 0) "${it.weightKg.fmt()} kg × " else "") + "${it.reps}" })
                    }
                }
                if (alternatives.isNotEmpty()) AppCard {
                    SectionHeader("Alternatifler", Icons.Rounded.SwapHoriz)
                    alternatives.forEach { a ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(a.name, style = MaterialTheme.typography.bodyMedium)
                                Muted(if (a.requires.isEmpty()) "Evde, ekipmansız" else a.requires.joinToString { it.label })
                            }
                            IconButton(onClick = { nav.navigate(Routes.exercise(a.id)) }) { Icon(Icons.Rounded.ChevronRight, "Aç") }
                        }
                    }
                }
                InfoBanner("Ağrı (kas yorgunluğu dışında keskin, eklem ağrısı) hissedersen hareketi bırak. Sağlık sorunun varsa antrenmana başlamadan önce hekime danış.")
            },
        )
    }
}

private class SetDraft(weight: String, reps: String) {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
}

@Composable
fun SessionScreen(templateId: String) {
    val data = appData()
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val date = today()
    val plan = remember(templateId) { WorkoutPlan.plan(data, templateId, Program.dayOf(data, date)) }
    val startedAt = remember { System.currentTimeMillis() }
    val drafts = remember(templateId) {
        plan.exercises.associate { pe ->
            val last = data.workoutLogs.sortedByDescending { it.date }.firstOrNull { l -> l.sets.any { it.exerciseId == pe.exercise.id && it.reps > 0 } }
                ?.sets?.filter { it.exerciseId == pe.exercise.id && it.reps > 0 }
            pe.slotKey to List(pe.sets) { i ->
                val s = last?.getOrNull(i) ?: last?.lastOrNull()
                SetDraft(s?.weightKg?.takeIf { it > 0 }?.fmt() ?: "", "")
            }.toMutableStateList()
        }
    }
    var note by remember { mutableStateOf("") }
    var restLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(restLeft) { if (restLeft > 0) { delay(1000); restLeft -= 1 } }

    Page(plan.template.name, back = true) {
        Column(Modifier.fillMaxSize()) {
            if (restLeft > 0) Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null); Text("  Dinlenme: ${restLeft / 60}:${String.format("%02d", restLeft % 60)}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { restLeft += 15 }) { Text("+15 sn") }
                    TextButton(onClick = { restLeft = 0 }) { Text("Geç") }
                }
            }
            Box(Modifier.weight(1f)) {
                ScreenColumn {
                    Muted("${plan.phase.name} · ${plan.phase.rir}. Önceki ağırlıklar otomatik dolduruldu. Tekrar sayısını gir; boş setler kaydedilmez.")
                    plan.exercises.forEach { pe ->
                        val sets = drafts.getValue(pe.slotKey)
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(pe.exercise.name, style = MaterialTheme.typography.titleMedium)
                                    Muted("Hedef: ${pe.sets} × ${pe.reps}" + if (pe.restSec > 0) " · ${pe.restSec} sn dinlenme" else "")
                                }
                                IconButton(onClick = { nav.navigate(Routes.exercise(pe.exercise.id)) }) { Icon(Icons.Rounded.Info, "Form bilgisi") }
                            }
                            sets.forEachIndexed { i, s ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${i + 1}", Modifier.width(20.dp), fontWeight = FontWeight.Bold)
                                    if (!pe.exercise.timed && pe.exercise.kind != ExKind.MOBILITY) NumberField("kg", s.weight, { s.weight = it }, Modifier.weight(1f))
                                    NumberField(if (pe.exercise.timed || pe.exercise.kind == ExKind.MOBILITY) "sn / dk" else "Tekrar", s.reps, { s.reps = it }, Modifier.weight(1f), decimal = false)
                                    IconButton(onClick = { if (pe.restSec > 0) restLeft = pe.restSec }) { Icon(Icons.Rounded.Timer, "Dinlenme başlat") }
                                }
                            }
                            Row {
                                TextButton(onClick = { sets.add(SetDraft(sets.lastOrNull()?.weight ?: "", "")) }) { Text("+ Set") }
                                if (sets.size > 1) TextButton(onClick = { sets.removeAt(sets.lastIndex) }) { Text("− Set") }
                            }
                        }
                    }
                    OutlinedTextField(note, { note = it }, label = { Text("Not (nasıl hissettin?)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val logged = plan.exercises.flatMap { pe ->
                            drafts.getValue(pe.slotKey).mapNotNull { s ->
                                val reps = s.reps.toIntOrNull() ?: return@mapNotNull null
                                SetLog(pe.exercise.id, s.weight.toDoubleTr() ?: 0.0, reps)
                            }
                        }
                        val minutes = ((System.currentTimeMillis() - startedAt) / 60000).toInt().coerceAtLeast(1)
                        val log = WorkoutLog(newId(), date.iso(), templateId, logged, minutes, note, completed = true)
                        repo.update { d -> DailyTasks.toggle(d.copy(workoutLogs = d.workoutLogs + log), date, DailyTasks.WORKOUT_ID, true) }
                        nav.popBackStack()
                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Check, null); Text(" Antrenmanı bitir ve kaydet") }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
