package com.ninetymax.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ninetymax.app.data.*
import com.ninetymax.app.domain.*
import com.ninetymax.app.ui.LocalRepo
import com.ninetymax.app.ui.appData
import com.ninetymax.app.ui.components.*
import java.time.LocalDate
import kotlin.math.roundToInt

private val TAGS = listOf("et", "balık", "laktoz", "takviye", "vejetaryen")

@Composable
fun NutritionScreen() {
    TabbedPage("Beslenme", listOf("Bugün", "Plan & hedefler", "Haftalık")) { tab ->
        when (tab) { 0 -> NutritionToday(); 1 -> NutritionPlan(); else -> NutritionWeekly() }
    }
}

@Composable
private fun NutritionToday() {
    val data = appData()
    val repo = LocalRepo.current
    val date = today()
    val t = Nutrition.targets(data)
    val (kcal, protein) = Nutrition.dayTotals(data, date.iso())
    val water = data.water[date.iso()] ?: 0
    var addSlot by remember { mutableStateOf<MealSlot?>(null) }
    var swapSlot by remember { mutableStateOf<MealSlot?>(null) }
    AdaptiveColumns(
        left = {
            AppCard {
                SectionHeader("Günlük hedefler", Icons.Rounded.Flag)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Kalori", "$kcal / ${t.kcal}", Icons.Rounded.LocalFireDepartment, Modifier.weight(1f))
                    StatTile("Protein", "${protein.roundToInt()} / ${t.protein} g", Icons.Rounded.Egg, Modifier.weight(1f))
                    StatTile("Su", "${"%.1f".format(water / 1000.0)} / ${"%.1f".format(t.waterMl / 1000.0)} L", Icons.Rounded.WaterDrop, Modifier.weight(1f))
                }
                LabeledProgress("Protein", (protein / t.protein).toFloat(), "%${(protein * 100 / t.protein).roundToInt()}")
                LabeledProgress("Kalori", kcal / t.kcal.toFloat(), "%${kcal * 100 / t.kcal}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { repo.update { addWater(it, date, 250) } }) { Icon(Icons.Rounded.LocalDrink, null); Text(" 250 ml") }
                    FilledTonalButton(onClick = { repo.update { addWater(it, date, 500) } }) { Text("500 ml") }
                    TextButton(onClick = { repo.update { addWater(it, date, -250) } }, enabled = water > 0) { Text("−250") }
                }
            }
            DayLog(data, date)
        },
        right = {
            MealSlot.entries.forEach { slot ->
                val opt = Nutrition.chosen(data, date, slot) ?: return@forEach
                val eaten = data.foodLogs.any { it.date == date.iso() && it.slot == slot && it.name == opt.name }
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(slot.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(opt.name, style = MaterialTheme.typography.titleMedium)
                            Muted("~${opt.kcal} kcal · ${opt.protein} g protein")
                        }
                        if (eaten) Icon(Icons.Rounded.CheckCircle, "Yendi", tint = MaterialTheme.colorScheme.secondary)
                    }
                    BulletList(opt.items)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            repo.update { d ->
                                val withLog = d.copy(foodLogs = d.foodLogs + FoodEntry(newId(), date.iso(), slot, opt.name, opt.kcal, opt.protein.toDouble()))
                                checkProtein(withLog, date)
                            }
                        }, enabled = !eaten) { Text("Yedim") }
                        OutlinedButton(onClick = { swapSlot = slot }) { Text("Alternatif") }
                        TextButton(onClick = { addSlot = slot }) { Text("Başka bir şey") }
                    }
                }
            }
            InfoBanner("Porsiyonlar ve kaloriler yaklaşık değerlerdir. Açlık durumuna ve haftalık kilo trendine göre porsiyonları artır veya azalt.")
        },
    )
    addSlot?.let { s -> AddFoodDialog(s, date) { addSlot = null } }
    swapSlot?.let { s ->
        val opts = Nutrition.options(data, s)
        AlertDialog(onDismissRequest = { swapSlot = null }, title = { Text("${s.label} alternatifleri") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 420.dp).verticalScrollSafe()) {
                opts.forEach { o ->
                    OutlinedCard(onClick = {
                        repo.update { d -> d.copy(meals = d.meals.copy(choices = d.meals.choices + ("${date.iso()}|${s.name}" to o.id))) }
                        swapSlot = null
                    }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(o.name, style = MaterialTheme.typography.titleSmall)
                            Muted("~${o.kcal} kcal · ${o.protein} g protein" + if ("ekonomik" in o.tags) " · ekonomik" else "")
                            Muted(o.items.joinToString(", "), maxLines = 2)
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { swapSlot = null }) { Text("Kapat") } })
    }
}

@Composable
fun Modifier.verticalScrollSafe(): Modifier = this.then(Modifier.verticalScroll(rememberScrollState()))

fun checkProtein(d: AppData, date: LocalDate): AppData {
    val (_, p) = Nutrition.dayTotals(d, date.iso())
    return DailyTasks.toggle(d, date, DailyTasks.PROTEIN_ID, p >= Nutrition.targets(d).protein)
}

@Composable
private fun DayLog(data: AppData, date: LocalDate) {
    val repo = LocalRepo.current
    val logs = data.foodLogs.filter { it.date == date.iso() }
    var add by remember { mutableStateOf(false) }
    AppCard {
        SectionHeader("Bugünkü kayıt", Icons.Rounded.ListAlt) { TextButton(onClick = { add = true }) { Text("+ Ekle") } }
        if (logs.isEmpty()) Muted("Henüz kayıt yok. Plandaki öğünde 'Yedim' de veya besin ekle.")
        MealSlot.entries.forEach { slot ->
            val l = logs.filter { it.slot == slot }
            if (l.isNotEmpty()) {
                Text(slot.label, style = MaterialTheme.typography.labelLarge)
                l.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, style = MaterialTheme.typography.bodyMedium)
                            Muted("${e.kcal} kcal · ${e.protein.fmt()} g protein")
                        }
                        IconButton(onClick = { repo.update { d -> checkProtein(d.copy(foodLogs = d.foodLogs.filterNot { it.id == e.id }), date) } }) { Icon(Icons.Rounded.Delete, "Sil") }
                    }
                }
            }
        }
    }
    if (add) AddFoodDialog(MealSlot.SNACK1, date) { add = false }
}

@Composable
private fun AddFoodDialog(initialSlot: MealSlot, date: LocalDate, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    var slot by remember { mutableStateOf(initialSlot) }
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<FoodItem?>(null) }
    var amount by remember { mutableStateOf("1") }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    FormDialog("Besin ekle", onDismiss, saveEnabled = picked != null || (name.isNotBlank() && kcal.toIntOrNull() != null), onSave = {
        val entry = picked?.let { f ->
            val m = amount.toDoubleTr() ?: 1.0
            FoodEntry(newId(), date.iso(), slot, "${f.name} (${if (m != 1.0) "${m.fmt()} × " else ""}${f.portion})", (f.kcal * m).roundToInt(), f.protein * m, f.carbs * m, f.fat * m)
        } ?: FoodEntry(newId(), date.iso(), slot, name.trim(), kcal.toIntOrNull() ?: 0, protein.toDoubleTr() ?: 0.0)
        repo.update { d -> checkProtein(d.copy(foodLogs = d.foodLogs + entry), date) }
    }) {
        ChipSelector(MealSlot.entries, slot, { it.label }) { slot = it }
        OutlinedTextField(query, { query = it; picked = null }, label = { Text("Besin ara") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        val results = MealLibrary.foods.filter { query.isBlank() || it.name.contains(query, true) }.take(if (query.isBlank()) 6 else 12)
        results.forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = picked == f, onClick = { picked = f })
                Column { Text(f.name, style = MaterialTheme.typography.bodyMedium); Muted("${f.portion} · ${f.kcal} kcal · ${f.protein.fmt()} g") }
            }
        }
        if (picked != null) NumberField("Porsiyon sayısı", amount, { amount = it })
        HorizontalDivider()
        Text("veya elle gir", style = MaterialTheme.typography.labelLarge)
        TextInput("Ad", name, { name = it; picked = null })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("kcal", kcal, { kcal = it }, Modifier.weight(1f), decimal = false)
            NumberField("Protein", protein, { protein = it }, Modifier.weight(1f), suffix = "g")
        }
    }
}

@Composable
private fun NutritionPlan() {
    val data = appData()
    val repo = LocalRepo.current
    val t = Nutrition.targets(data)
    val s = data.settings
    var proteinPerKg by remember(s.proteinPerKg) { mutableStateOf(s.proteinPerKg.fmt()) }
    var surplus by remember(s.calorieSurplus) { mutableStateOf(s.calorieSurplus.toString()) }
    var override by remember(s.calorieOverride) { mutableStateOf(s.calorieOverride?.toString() ?: "") }
    var water by remember(s.waterTargetMl) { mutableStateOf(s.waterTargetMl.toString()) }
    var addOpen by remember { mutableStateOf(false) }
    AdaptiveColumns(
        left = {
            AppCard {
                SectionHeader("Hedef hesabı", Icons.Rounded.Calculate)
                KeyValue("Güncel kilo", "${t.weightKg.fmt()} kg")
                KeyValue("Bazal metabolizma", "${t.bmr} kcal (tahmini)")
                KeyValue("Günlük harcama", "${t.tdee} kcal (aktivite ×${data.profile.activityFactor.fmt(2)})")
                KeyValue("Hedef kalori", "${t.kcal} kcal")
                KeyValue("Protein", "${t.protein} g (${s.proteinPerKg.fmt()} g/kg)")
                Muted("Hesap Mifflin-St Jeor formülüne dayalı bir tahmindir. Asıl rehber haftalık tartı trendidir: kontrollü kas kazanımı için haftada ~0,25-0,5 kg artış hedeflenir.")
            }
            AppCard {
                SectionHeader("Hedefleri düzenle", Icons.Rounded.Tune)
                NumberField("Protein (g/kg)", proteinPerKg, { proteinPerKg = it }, Modifier.fillMaxWidth())
                NumberField("Kalori fazlası", surplus, { surplus = it }, Modifier.fillMaxWidth(), suffix = "kcal", decimal = false)
                NumberField("Sabit kalori hedefi (boş = otomatik)", override, { override = it }, Modifier.fillMaxWidth(), suffix = "kcal", decimal = false)
                NumberField("Su hedefi", water, { water = it }, Modifier.fillMaxWidth(), suffix = "ml", decimal = false)
                Button(onClick = {
                    repo.update { d -> d.copy(settings = d.settings.copy(
                        proteinPerKg = proteinPerKg.toDoubleTr()?.coerceIn(1.0, 3.0) ?: d.settings.proteinPerKg,
                        calorieSurplus = surplus.toIntOrNull()?.coerceIn(-1000, 1000) ?: d.settings.calorieSurplus,
                        calorieOverride = override.toIntOrNull()?.takeIf { it in 1200..6000 },
                        waterTargetMl = water.toIntOrNull()?.coerceIn(1000, 6000) ?: d.settings.waterTargetMl,
                    )) }
                }) { Text("Kaydet") }
            }
            AppCard {
                SectionHeader("Tercihler & bütçe", Icons.Rounded.Savings)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(s.preferEconomic, { v -> repo.update { d -> d.copy(settings = d.settings.copy(preferEconomic = v)) } })
                    Text("  Ekonomik seçenekleri öne çıkar")
                }
                Text("Hariç tut", style = MaterialTheme.typography.labelLarge)
                MultiChipSelector(TAGS, s.excludedTags, { it }) { tag ->
                    repo.update { d -> val e = d.settings.excludedTags; d.copy(settings = d.settings.copy(excludedTags = if (tag in e) e - tag else e + tag)) }
                }
            }
        },
        right = {
            SectionHeader("Öğün seçenekleri", Icons.Rounded.MenuBook) { TextButton(onClick = { addOpen = true }) { Text("+ Kendi öğünüm") } }
            Muted("Gizlediğin seçenekler plana gelmez. Plan her gün seçenekler arasında döner; Bugün sekmesinden değiştirebilirsin.")
            MealSlot.entries.forEach { slot ->
                AppCard {
                    Text(slot.label, style = MaterialTheme.typography.titleSmall)
                    (MealLibrary.options + data.meals.customOptions).filter { it.slot == slot }.forEach { o ->
                        val hidden = o.id in data.meals.hiddenOptionIds
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(o.name + if (o.custom) " (benim)" else "", style = MaterialTheme.typography.bodyMedium, color = if (hidden) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                                Muted("~${o.kcal} kcal · ${o.protein} g", maxLines = 1)
                            }
                            Switch(!hidden, { show -> repo.update { d -> val h = d.meals.hiddenOptionIds; d.copy(meals = d.meals.copy(hiddenOptionIds = if (show) h - o.id else h + o.id)) } })
                            if (o.custom) IconButton(onClick = { repo.update { d -> d.copy(meals = d.meals.copy(customOptions = d.meals.customOptions.filterNot { it.id == o.id })) } }) { Icon(Icons.Rounded.Delete, "Sil") }
                        }
                    }
                }
            }
        },
    )
    if (addOpen) {
        var slot by remember { mutableStateOf(MealSlot.LUNCH) }
        var name by remember { mutableStateOf("") }
        var items by remember { mutableStateOf("") }
        var kcal by remember { mutableStateOf("") }
        var protein by remember { mutableStateOf("") }
        FormDialog("Kendi öğünün", { addOpen = false }, saveEnabled = name.isNotBlank(), onSave = {
            val o = MealOption(newId(), slot, name.trim(), items.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() }, kcal.toIntOrNull() ?: 0, protein.toIntOrNull() ?: 0, custom = true)
            repo.update { d -> d.copy(meals = d.meals.copy(customOptions = d.meals.customOptions + o)) }
        }) {
            ChipSelector(MealSlot.entries, slot, { it.label }) { slot = it }
            TextInput("Öğün adı", name, { name = it })
            TextInput("İçerik ve porsiyonlar (virgülle ayır)", items, { items = it }, singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("kcal", kcal, { kcal = it }, Modifier.weight(1f), decimal = false)
                NumberField("Protein", protein, { protein = it }, Modifier.weight(1f), suffix = "g", decimal = false)
            }
        }
    }
}

@Composable
private fun NutritionWeekly() {
    val data = appData()
    val date = today()
    val w = Nutrition.weekly(data, date)
    val t = Nutrition.targets(data)
    AdaptiveColumns(
        left = {
            AppCard {
                SectionHeader("Son 7 gün", Icons.Rounded.Insights)
                KeyValue("Kayıt girilen gün", "${w.daysLogged} / 7")
                KeyValue("Ortalama kalori", if (w.daysLogged > 0) "${w.avgKcal} kcal (hedef ${t.kcal})" else "—")
                KeyValue("Ortalama protein", if (w.daysLogged > 0) "${w.avgProtein} g (hedef ${t.protein})" else "—")
                KeyValue("Protein hedefi tutan gün", "${w.proteinHitDays}")
                KeyValue("Ortalama su", "${w.avgWaterMl} ml")
                KeyValue("Kilo değişimi (14 gün)", w.weightChangeKg?.let { (if (it >= 0) "+" else "") + it.fmt() + " kg" } ?: "Yetersiz veri")
            }
            AppCard {
                SectionHeader("Günlük protein (% hedef)", Icons.Rounded.BarChart)
                val values = (6 downTo 0).map { back -> val d = date.minusDays(back.toLong()); (Nutrition.dayTotals(data, d.iso()).second * 100 / t.protein).roundToInt() }
                BarChart(values)
                Row { (6 downTo 0).forEach { back -> Text(date.minusDays(back.toLong()).dayOfWeek.shortTr(), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall) } }
            }
        },
        right = {
            AppCard {
                SectionHeader("Değerlendirme ve öneriler", Icons.Rounded.Lightbulb)
                BulletList(w.suggestions)
            }
        },
    )
}
