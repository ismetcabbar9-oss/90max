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
import com.ninetymax.app.domain.*
import com.ninetymax.app.notify.Reminders
import com.ninetymax.app.ui.LocalNav
import com.ninetymax.app.ui.LocalRepo
import com.ninetymax.app.ui.Page
import com.ninetymax.app.ui.Routes
import com.ninetymax.app.ui.appData
import com.ninetymax.app.ui.components.*
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun TodayScreen() {
    val data = appData()
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val date = today()
    val day = Program.dayOf(data, date)
    val tasks = DailyTasks.forDate(data, date)
    val status = DailyTasks.status(data, date)
    val progress = DailyTasks.progress(data, date)
    val visible = tasks.filter { it.id !in status.moved }
    var addOpen by remember { mutableStateOf(false) }

    Page("Bugün", actions = {
        IconButton(onClick = { addOpen = true }) { Icon(Icons.Rounded.AddTask, "Görev ekle") }
    }) {
        AdaptiveColumns(
            left = {
                HeaderCard(data, date, day, progress)
                QuickStats(data, date)
                listOf(Slot.MORNING to "Sabah rutini", Slot.DAY to "Gün içi", Slot.EVENING to "Akşam rutini").forEach { (slot, title) ->
                    val list = visible.filter { it.slot == slot }
                    if (list.isNotEmpty()) TaskCard(title, slot, list, status, date)
                }
                YesterdayCard(data, date)
            },
            right = {
                WorkoutSummary(data, date, day)
                NutritionSummary(data, date)
                ProductsToday(data, tasks)
                UpcomingReminders(data)
                UnfinishedCard(data, date)
                ReviewCard(data, date)
            },
        )
    }
    if (addOpen) AddTaskDialog(date, onDismiss = { addOpen = false }) { task -> repo.update { it.copy(customTasks = it.customTasks + task) } }
}

@Composable
private fun HeaderCard(data: AppData, date: LocalDate, day: Int, progress: DayProgress) {
    val phase = Program.phaseOf(day.coerceIn(1, PROGRAM_DAYS))
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Muted(date.longTr())
                val greet = data.profile.name.takeIf { it.isNotBlank() }?.let { "Merhaba $it" } ?: "Merhaba"
                Text(greet, style = MaterialTheme.typography.headlineSmall)
                when {
                    day < 1 -> Text("Program ${1 - day} gün sonra başlıyor", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    day > PROGRAM_DAYS -> Text("90 günlük program tamamlandı 🎉", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    else -> Text("Gün $day / $PROGRAM_DAYS · Hafta ${Program.weekOf(day)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (day in 1..PROGRAM_DAYS) Muted(phase.name)
            }
            ProgressRing(progress.percent, label = "${progress.done}/${progress.total}")
        }
        if (day in 1..PROGRAM_DAYS) {
            LinearProgressIndicator(progress = { day / PROGRAM_DAYS.toFloat() }, modifier = Modifier.fillMaxWidth())
        }
        Muted(
            when {
                progress.total == 0 -> "Bugün için görev yok. Dinlen ve keyfini çıkar."
                progress.percent >= 100 -> "Bugünkü tüm görevler tamam. Harika iş!"
                progress.percent >= 60 -> "Çok iyi gidiyorsun. Kalanlar için acele yok."
                else -> "Mükemmel olmak zorunda değil. Bugün birkaç adım bile ilerlemedir."
            },
        )
    }
}

@Composable
private fun QuickStats(data: AppData, date: LocalDate) {
    val repo = LocalRepo.current
    val t = Nutrition.targets(data)
    val water = data.water[date.iso()] ?: 0
    val (kcal, protein) = Nutrition.dayTotals(data, date.iso())
    AppCard {
        SectionHeader("Beslenme & su", Icons.Rounded.WaterDrop)
        LabeledProgress("Su", water / t.waterMl.toFloat(), "${water} / ${t.waterMl} ml")
        LabeledProgress("Protein", (protein / t.protein).toFloat(), "${protein.toInt()} / ${t.protein} g")
        LabeledProgress("Kalori", kcal / t.kcal.toFloat(), "$kcal / ${t.kcal} kcal")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { repo.update { addWater(it, date, 250) } }) { Text("+250 ml") }
            FilledTonalButton(onClick = { repo.update { addWater(it, date, 500) } }) { Text("+500 ml") }
            TextButton(onClick = { repo.update { addWater(it, date, -250) } }, enabled = water > 0) { Text("−250") }
        }
    }
}

fun addWater(d: AppData, date: LocalDate, ml: Int): AppData {
    val key = date.iso()
    val v = ((d.water[key] ?: 0) + ml).coerceAtLeast(0)
    val withWater = d.copy(water = d.water + (key to v))
    val reached = v >= Nutrition.targets(withWater).waterMl
    return DailyTasks.toggle(withWater, date, DailyTasks.WATER_ID, reached)
}

@Composable
fun TaskCard(title: String, slot: Slot, tasks: List<DailyTask>, status: DayStatus, date: LocalDate) {
    val repo = LocalRepo.current
    val done = tasks.count { it.id in status.done }
    AppCard {
        SectionHeader(title, when (slot) { Slot.MORNING -> Icons.Rounded.WbTwilight; Slot.DAY -> Icons.Rounded.LightMode; Slot.EVENING -> Icons.Rounded.Bedtime }) {
            Muted("$done/${tasks.size}")
        }
        tasks.forEach { task ->
            val skipped = task.id in status.skipped
            TaskRow(
                title = task.title + if (skipped) " (atlandı)" else "",
                detail = task.detail,
                area = task.area,
                done = task.id in status.done,
                onToggle = { repo.update { DailyTasks.toggle(it, date, task.id) } },
                trailing = { TaskMenu(task, date, skipped) },
            )
        }
    }
}

@Composable
private fun TaskMenu(task: DailyTask, date: LocalDate, skipped: Boolean) {
    val repo = LocalRepo.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, "Seçenekler") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Yarına ertele") }, leadingIcon = { Icon(Icons.Rounded.Redo, null) }, onClick = {
                open = false; repo.update { DailyTasks.move(it, date, task, date.plusDays(1)) }
            })
            DropdownMenuItem(text = { Text(if (skipped) "Atlamayı geri al" else "Bugün atla") }, leadingIcon = { Icon(Icons.Rounded.SkipNext, null) }, onClick = {
                open = false; repo.update { DailyTasks.skip(it, date, task.id, !skipped) }
            })
            if (task.id.startsWith("c:")) {
                DropdownMenuItem(text = { Text("Sil") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = {
                    open = false; val id = task.id.removePrefix("c:")
                    repo.update { d -> d.copy(customTasks = d.customTasks.filterNot { it.id == id }) }
                })
            }
        }
    }
}

@Composable
private fun YesterdayCard(data: AppData, date: LocalDate) {
    val repo = LocalRepo.current
    val y = date.minusDays(1)
    if (Program.dayOf(data, y) < 1) return
    val left = DailyTasks.unfinished(data, y).filter { it.id != DailyTasks.WATER_ID && it.id != DailyTasks.PROTEIN_ID }
    if (left.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    AppCard {
        SectionHeader("Dünden kalanlar (${left.size})", Icons.Rounded.History) {
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Gizle" else "Göster") }
        }
        Muted("Ceza yok. İstediğini bugüne al, gerisini kapat.")
        if (expanded) {
            left.forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { repo.update { DailyTasks.move(it, y, task, date) } }) { Text("Bugüne al") }
                    IconButton(onClick = { repo.update { DailyTasks.skip(it, y, task.id) } }) { Icon(Icons.Rounded.Close, "Kapat") }
                }
            }
            TextButton(onClick = { repo.update { d -> left.fold(d) { acc, t -> DailyTasks.skip(acc, y, t.id) } } }) { Text("Hepsini kapat") }
        }
    }
}

@Composable
private fun WorkoutSummary(data: AppData, date: LocalDate, day: Int) {
    val nav = LocalNav.current
    val template = WorkoutPlan.templateForDate(data.workout, date.dayOfWeek.value)
    val plan = WorkoutPlan.plan(data, template.id, day)
    val doneToday = data.workoutLogs.any { it.date == date.iso() && it.completed }
    AppCard(onClick = { nav.navigate(Routes.WORKOUT) }) {
        SectionHeader("Günün antrenmanı", Icons.Rounded.FitnessCenter) { if (doneToday) AssistChip(onClick = {}, label = { Text("Tamamlandı") }) }
        Text(template.name, style = MaterialTheme.typography.titleMedium)
        Muted("${template.focus} · ~${template.estMinutes} dk · ${plan.phase.rir}")
        plan.exercises.take(4).forEach { Text("• ${it.exercise.name} — ${it.sets} × ${it.reps}", style = MaterialTheme.typography.bodyMedium) }
        if (plan.exercises.size > 4) Muted("+${plan.exercises.size - 4} hareket")
        if (!template.isRest) Button(onClick = { nav.navigate(Routes.session(template.id)) }) {
            Icon(Icons.Rounded.PlayArrow, null); Text(if (doneToday) " Tekrar kaydet" else " Antrenmana başla")
        }
    }
}

@Composable
private fun NutritionSummary(data: AppData, date: LocalDate) {
    val nav = LocalNav.current
    AppCard(onClick = { nav.navigate(Routes.NUTRITION) }) {
        SectionHeader("Bugünün yemek planı", Icons.Rounded.Restaurant)
        listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER).forEach { slot ->
            Nutrition.chosen(data, date, slot)?.let { KeyValue(slot.label, "${it.name} (${it.protein} g protein)") }
        }
        Muted("Ara öğünler ve kayıt için Beslenme sekmesi.")
    }
}

@Composable
private fun ProductsToday(data: AppData, tasks: List<DailyTask>) {
    val nav = LocalNav.current
    val ids = tasks.mapNotNull { it.productId }.distinct()
    val products = ids.mapNotNull { id -> data.products.firstOrNull { it.id == id } }
    if (products.isEmpty()) return
    AppCard {
        SectionHeader("Bugün kullanılacak ürünler", Icons.Rounded.ShoppingBag)
        products.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconBadge(categoryIcon(p.category), com.ninetymax.app.ui.theme.areaColor(categoryArea(p.category)), 30.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Muted("${p.brand} · ${p.status.label}", maxLines = 1)
                }
                IconButton(onClick = { nav.navigate(Routes.product(p.id)) }) { Icon(Icons.Rounded.ChevronRight, "Aç") }
            }
        }
    }
}

@Composable
private fun UpcomingReminders(data: AppData) {
    val now = LocalDateTime.now()
    val upcoming = data.reminders.mapNotNull { r -> Reminders.nextTrigger(r, now)?.let { r to it } }
        .filter { it.second.toLocalDate() == now.toLocalDate() }
        .sortedBy { it.second }
    AppCard {
        SectionHeader("Yaklaşan hatırlatmalar", Icons.Rounded.NotificationsActive)
        if (upcoming.isEmpty()) Muted("Bugün için başka hatırlatma yok.")
        upcoming.take(4).forEach { (r, t) -> KeyValue(String.format("%02d:%02d", t.hour, t.minute), r.title) }
        if (!Reminders.canNotify(androidx.compose.ui.platform.LocalContext.current)) {
            InfoBanner("Bildirim izni kapalı. Hatırlatmaları almak için Android ayarlarından 90Max bildirimlerine izin ver.", Icons.Rounded.NotificationsOff, MaterialTheme.colorScheme.tertiaryContainer)
        }
    }
}

@Composable
private fun UnfinishedCard(data: AppData, date: LocalDate) {
    val left = DailyTasks.unfinished(data, date)
    AppCard {
        SectionHeader("Tamamlanmamış görevler (${left.size})", Icons.Rounded.PendingActions)
        if (left.isEmpty()) Muted("Hepsi tamam!")
        left.take(6).forEach { Text("• ${it.title}", style = MaterialTheme.typography.bodyMedium) }
        if (left.size > 6) Muted("+${left.size - 6} görev daha")
    }
}

@Composable
private fun ReviewCard(data: AppData, date: LocalDate) {
    val repo = LocalRepo.current
    val existing = data.reviews.firstOrNull { it.date == date.iso() } ?: DayReview(date.iso())
    var note by remember(existing.note) { mutableStateOf(existing.note) }
    fun save(r: DayReview) = repo.update { d ->
        val updated = DailyTasks.toggle(d.copy(reviews = d.reviews.filterNot { it.date == r.date } + r), date, "r:pm_review", true)
        updated
    }
    AppCard {
        SectionHeader("Günlük değerlendirme", Icons.Rounded.Mood)
        RatingRow("Ruh hali", existing.mood) { save(existing.copy(mood = it, note = note)) }
        RatingRow("Enerji", existing.energy) { save(existing.copy(energy = it, note = note)) }
        OutlinedTextField(note, { note = it }, label = { Text("Kısa not (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { save(existing.copy(note = note)) }) { Text("Kaydet") }
    }
}

@Composable
fun AddTaskDialog(date: LocalDate, onDismiss: () -> Unit, onAdd: (CustomTask) -> Unit) {
    var title by remember { mutableStateOf("") }
    var slot by remember { mutableStateOf(Slot.DAY) }
    var area by remember { mutableStateOf(Area.GENERAL) }
    var day by remember { mutableStateOf(date) }
    FormDialog("Görev ekle", onDismiss, onSave = { onAdd(CustomTask(newId(), day.iso(), title.trim(), area, slot)) }, saveEnabled = title.isNotBlank()) {
        TextInput("Görev", title, { title = it })
        DateButton("Tarih", day.iso(), { day = it })
        Text("Zaman", style = MaterialTheme.typography.labelLarge)
        ChipSelector(Slot.entries, slot, { it.label }) { slot = it }
        Text("Alan", style = MaterialTheme.typography.labelLarge)
        ChipSelector(Area.entries, area, { it.label }) { area = it }
    }
}
