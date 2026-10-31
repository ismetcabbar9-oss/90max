package com.ninetymax.app.ui.screens

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ninetymax.app.BuildConfig
import com.ninetymax.app.data.*
import com.ninetymax.app.domain.Nutrition
import com.ninetymax.app.domain.Program
import com.ninetymax.app.notify.Reminders
import com.ninetymax.app.seed.Guides
import com.ninetymax.app.seed.Seed
import com.ninetymax.app.ui.LocalNav
import com.ninetymax.app.ui.LocalRepo
import com.ninetymax.app.ui.LocalSnackbar
import com.ninetymax.app.ui.Page
import com.ninetymax.app.ui.Routes
import com.ninetymax.app.ui.appData
import com.ninetymax.app.ui.components.*
import com.ninetymax.app.ui.moreDests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MoreScreen() {
    val nav = LocalNav.current
    Page("Daha fazla") {
        ScreenColumn {
            moreDests.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { d ->
                        AppCard(Modifier.weight(1f), onClick = { nav.navigate(d.route) }) {
                            Icon(d.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Text(d.label, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            AppCard(onClick = { nav.navigate(Routes.ROUTINES) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EventRepeat, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Rutinleri düzenle", style = MaterialTheme.typography.titleMedium)
                        Muted("Sabah/akşam görevlerini ekle, sil, zamanla")
                    }
                    Icon(Icons.Rounded.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val data = appData()
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    var editReminder by remember { mutableStateOf<Reminder?>(null) }
    var addReminder by remember { mutableStateOf(false) }
    var restart by remember { mutableStateOf(false) }
    var wipe by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { Backup.export(repo, it) } }.isSuccess }
            snackbar.showSnackbar(if (ok) "Yedek kaydedildi" else "Yedek kaydedilemedi")
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)?.use { Backup.import(repo, it) } } }
            snackbar.showSnackbar(if (result.isSuccess) "Yedek geri yüklendi" else "Geçersiz yedek dosyası; mevcut verilere dokunulmadı")
        }
    }
    Page("Ayarlar") {
        AdaptiveColumns(left = {
            ProfileCard(data)
            AppCard {
                SectionHeader("Program", Icons.Rounded.CalendarMonth)
                val day = Program.dayOf(data, today())
                KeyValue("Başlangıç", data.profile.startDate.toDateOrNull()?.longTr() ?: "—")
                KeyValue("Bugün", "Gün $day")
                Muted("Hasta oldun, tatildeydin veya ara verdin mi? Programı kaydırarak kaldığın yerden devam et; geçmiş kayıtlar silinmez.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { repo.update { d -> d.copy(profile = d.profile.copy(startDate = Program.startDate(d, today()).plusDays(1).iso())) } }) { Text("1 gün ertele") }
                    OutlinedButton(onClick = { repo.update { d -> d.copy(profile = d.profile.copy(startDate = Program.startDate(d, today()).minusDays(1).iso())) } }) { Text("1 gün ileri") }
                }
                DateButton("Başlangıç tarihi", data.profile.startDate, { dt -> repo.update { d -> d.copy(profile = d.profile.copy(startDate = dt.iso())) } })
                TextButton(onClick = { restart = true }) { Text("Programı bugünden yeniden başlat") }
            }
            AppCard {
                SectionHeader("Görünüm", Icons.Rounded.Palette)
                ChipSelector(ThemeMode.entries, data.settings.theme, { when (it) { ThemeMode.SYSTEM -> "Sistem"; ThemeMode.LIGHT -> "Açık"; ThemeMode.DARK -> "Koyu" } }) { m ->
                    repo.update { d -> d.copy(settings = d.settings.copy(theme = m)) }
                }
            }
            AppCard(onClick = { nav.navigate(Routes.ROUTINES) }) {
                SectionHeader("Rutinler ve görevler", Icons.Rounded.EventRepeat) { Icon(Icons.Rounded.ChevronRight, null) }
                Muted("${data.routines.count { it.enabled }} açık / ${data.routines.size} rutin maddesi")
            }
        }, right = {
            AppCard {
                SectionHeader("Hatırlatmalar", Icons.Rounded.Notifications) { TextButton(onClick = { addReminder = true }) { Text("+ Ekle") } }
                if (!Reminders.canNotify(context)) {
                    InfoBanner("Bildirim izni kapalı.", Icons.Rounded.NotificationsOff, MaterialTheme.colorScheme.tertiaryContainer)
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)) }
                    }) { Text("Bildirim ayarlarını aç") }
                }
                data.reminders.sortedBy { it.time }.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${r.time} · ${r.title}", style = MaterialTheme.typography.bodyMedium)
                            Muted(if (r.days.size == 7) "Her gün" else r.days.sorted().joinToString(", ") { isoDayShort(it) })
                        }
                        Switch(r.enabled, { en -> repo.update { d -> d.copy(reminders = d.reminders.map { if (it.id == r.id) it.copy(enabled = en) else it }) } })
                        IconButton(onClick = { editReminder = r }) { Icon(Icons.Rounded.Edit, "Düzenle") }
                    }
                }
                Muted("Hatırlatmalar pil tasarrufu nedeniyle birkaç dakika gecikebilir.")
            }
            AppCard {
                SectionHeader("Yedekleme ve veri", Icons.Rounded.Backup)
                Muted("Tüm veriler cihazında, çevrimdışı saklanır. Her gün otomatik yerel yedek alınır (son 7 gün) ve Android yedeklemesi açıksa Google hesabına da yedeklenir. Telefon değiştirirken veya sıfırlamadan önce ZIP yedeği al.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exporter.launch("90max-yedek-${today()}.zip") }) { Icon(Icons.Rounded.FileDownload, null); Text(" Dışa aktar") }
                    OutlinedButton(onClick = { importer.launch(arrayOf("application/zip", "application/json", "application/octet-stream")) }) { Icon(Icons.Rounded.FileUpload, null); Text(" İçe aktar") }
                }
                if (repo.recoveredFromBackup) InfoBanner("Ana veri dosyası okunamadığı için en son otomatik yedekten geri yüklendi.", Icons.Rounded.Restore)
                TextButton(onClick = { wipe = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Tüm verileri sil") }
            }
            AppCard {
                SectionHeader("Hakkında", Icons.Rounded.Info)
                KeyValue("Sürüm", BuildConfig.VERSION_NAME)
                Muted(Guides.DISCLAIMER)
                Muted("Fiyat karşılaştırma bağlantıları dışında uygulamanın tüm özellikleri internetsiz çalışır.")
            }
        })
    }
    editReminder?.let { r -> ReminderDialog(r) { editReminder = null } }
    if (addReminder) ReminderDialog(null) { addReminder = false }
    if (restart) ConfirmDialog("Yeniden başlat", "Program bugün Gün 1 olarak yeniden başlasın mı? Kayıtların silinmez.", confirm = "Başlat", onConfirm = {
        repo.update { d -> d.copy(profile = d.profile.copy(startDate = today().iso(), startWeightKg = Nutrition.currentWeight(d))) }
    }, onDismiss = { restart = false })
    if (wipe) ConfirmDialog("Tüm verileri sil", "Bütün kayıtlar, ürün durumları ve fotoğraflar kalıcı olarak silinecek. Önce yedek almanı öneririz.", confirm = "Kalıcı olarak sil", onConfirm = {
        repo.photoDir.listFiles()?.forEach { it.delete() }
        repo.replaceAll(AppData())
    }, onDismiss = { wipe = false })
}

@Composable
private fun ProfileCard(data: AppData) {
    val repo = LocalRepo.current
    val p = data.profile
    var name by remember(p.name) { mutableStateOf(p.name) }
    var start by remember(p.startWeightKg) { mutableStateOf(p.startWeightKg.fmt()) }
    var target by remember(p.targetWeightKg) { mutableStateOf(p.targetWeightKg.fmt()) }
    var height by remember(p.heightCm) { mutableStateOf(p.heightCm.toString()) }
    var age by remember(p.age) { mutableStateOf(p.age.toString()) }
    AppCard {
        SectionHeader("Profil", Icons.Rounded.Person)
        TextInput("Ad", name, { name = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Başlangıç kilosu", start, { start = it }, Modifier.weight(1f), suffix = "kg")
            NumberField("Hedef kilo", target, { target = it }, Modifier.weight(1f), suffix = "kg")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Boy", height, { height = it }, Modifier.weight(1f), suffix = "cm", decimal = false)
            NumberField("Yaş", age, { age = it }, Modifier.weight(1f), decimal = false)
        }
        Text("Günlük aktivite", style = MaterialTheme.typography.labelLarge)
        ChipSelector(ACTIVITY_LEVELS.map { it.first }, p.activityFactor, { f -> ACTIVITY_LEVELS.first { it.first == f }.second }) { f -> repo.update { d -> d.copy(profile = d.profile.copy(activityFactor = f)) } }
        Button(onClick = {
            repo.update { d -> d.copy(profile = d.profile.copy(
                name = name.trim(),
                startWeightKg = start.toDoubleTr()?.coerceIn(30.0, 250.0) ?: d.profile.startWeightKg,
                targetWeightKg = target.toDoubleTr()?.coerceIn(30.0, 250.0) ?: d.profile.targetWeightKg,
                heightCm = height.toIntOrNull()?.coerceIn(120, 230) ?: d.profile.heightCm,
                age = age.toIntOrNull()?.coerceIn(13, 99) ?: d.profile.age,
            )) }
        }) { Text("Profili kaydet") }
    }
}

val ACTIVITY_LEVELS = listOf(1.35 to "Az hareketli", 1.55 to "Orta (3-5 antrenman)", 1.7 to "Yüksek", 1.85 to "Çok yüksek")

@Composable
private fun ReminderDialog(r: Reminder?, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val base = r ?: Reminder(newId(), "", "09:00")
    var title by remember { mutableStateOf(base.title) }
    var message by remember { mutableStateOf(base.message) }
    var time by remember { mutableStateOf(base.time) }
    var days by remember { mutableStateOf(base.days) }
    FormDialog(if (r == null) "Hatırlatma ekle" else "Hatırlatmayı düzenle", onDismiss, saveEnabled = title.isNotBlank() && days.isNotEmpty(), onSave = {
        val u = base.copy(title = title.trim(), message = message.trim(), time = time, days = days)
        repo.update { d -> d.copy(reminders = if (r == null) d.reminders + u else d.reminders.map { if (it.id == u.id) u else it }) }
    }) {
        TextInput("Başlık", title, { title = it })
        TextInput("Mesaj", message, { message = it })
        TimeButton("Saat", time, { time = it })
        MultiChipSelector((1..7).toList(), days, { isoDayShort(it) }) { d -> days = if (d in days) days - d else days + d }
        if (r != null) TextButton(onClick = { repo.update { d -> d.copy(reminders = d.reminders.filterNot { it.id == r.id }) }; onDismiss() }) { Icon(Icons.Rounded.Delete, null); Text(" Sil") }
    }
}

@Composable
fun OnboardingScreen() {
    val repo = LocalRepo.current
    val data = appData()
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("73") }
    var target by remember { mutableStateOf("78") }
    var height by remember { mutableStateOf("178") }
    var age by remember { mutableStateOf("24") }
    var startDate by remember { mutableStateOf(today()) }
    var equipment by remember { mutableStateOf(data.settings.equipment) }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        // Tüm ekran kaydırılabilir; geniş ekranda içerik ortada 640 dp genişlikte kalır.
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Spacer(Modifier.height(12.dp))
                Text("90Max", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("90 günlük kişisel gelişim ve görünüm programın", style = MaterialTheme.typography.titleMedium)
                Muted("Antrenman, beslenme, bakım, stil ve alışkanlıkların tek yerde. Her şey sonradan değiştirilebilir.")
                AppCard {
                    SectionHeader("Hedefin", Icons.Rounded.Flag)
                    BulletList(listOf(
                        "Kontrollü kilo artışı ve kas gelişimi",
                        "V görünümü: geniş omuz, gelişmiş sırt ve üst göğüs, güçlü core, dik duruş",
                        "Orta/uzun saçta belirgin, sağlıklı bukleler",
                        "Clean Boy stili: sade, temiz, özenli",
                    ))
                }
                AppCard {
                    SectionHeader("Seni tanıyalım", Icons.Rounded.Person)
                    TextInput("Adın (isteğe bağlı)", name, { name = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Kilo", weight, { weight = it }, Modifier.weight(1f), suffix = "kg")
                        NumberField("Hedef", target, { target = it }, Modifier.weight(1f), suffix = "kg")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Boy", height, { height = it }, Modifier.weight(1f), suffix = "cm", decimal = false)
                        NumberField("Yaş", age, { age = it }, Modifier.weight(1f), decimal = false)
                    }
                    Muted("Boy ve yaş yalnızca kalori tahmini için kullanılır. Varsayılan değerler tahmindir; kendi değerlerini gir.")
                    DateButton("Başlangıç", startDate.iso(), { startDate = it })
                }
                AppCard {
                    SectionHeader("Ekipmanın", Icons.Rounded.Handyman)
                    Muted("Sahip olduklarını seç. Hiçbiri yoksa program vücut ağırlığıyla çalışır.")
                    MultiChipSelector(Equip.entries.filter { it != Equip.BODYWEIGHT }, equipment, { it.label }) { e -> equipment = if (e in equipment) equipment - e else equipment + e }
                }
                InfoBanner(Guides.DISCLAIMER, Icons.Rounded.HealthAndSafety)
                Button(onClick = {
                    repo.update { d ->
                        val w = weight.toDoubleTr()?.coerceIn(30.0, 250.0) ?: 73.0
                        d.copy(
                            onboarded = true,
                            profile = d.profile.copy(
                                name = name.trim(), startDate = startDate.iso(), startWeightKg = w,
                                targetWeightKg = target.toDoubleTr()?.coerceIn(30.0, 250.0) ?: (w + 5),
                                heightCm = height.toIntOrNull()?.coerceIn(120, 230) ?: 178, age = age.toIntOrNull()?.coerceIn(13, 99) ?: 24,
                            ),
                            settings = d.settings.copy(equipment = equipment + Equip.BODYWEIGHT),
                            weights = if (d.weights.isEmpty()) listOf(WeightEntry(newId(), startDate.iso(), w)) else d.weights,
                        )
                    }
                }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Programı başlat") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
