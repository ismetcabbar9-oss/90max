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
import com.ninetymax.app.seed.Seed
import com.ninetymax.app.ui.LocalRepo
import com.ninetymax.app.ui.Page
import com.ninetymax.app.ui.appData
import com.ninetymax.app.ui.components.*

fun scheduleText(s: Schedule): String {
    val base = when (s.type) {
        ScheduleType.DAILY -> "Her gün"
        ScheduleType.WEEKDAYS -> s.weekdays.sorted().joinToString(", ") { isoDayShort(it) }
        ScheduleType.EVERY_N_DAYS -> "${s.everyN} günde bir"
    }
    val range = if (s.startDay > 1 || s.endDay < 90) " · gün ${s.startDay}-${s.endDay}" else ""
    return base + range
}

/** Bir alana ait rutin maddelerini (aç/kapa + düzenle) gösterir. */
@Composable
fun RoutineSection(area: Area, title: String = "${area.label} rutini") {
    val data = appData()
    val repo = LocalRepo.current
    var edit by remember { mutableStateOf<RoutineItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    val items = data.routines.filter { it.area == area }.sortedWith(compareBy({ it.slot.ordinal }, { it.order }))
    AppCard {
        SectionHeader(title, Icons.Rounded.EventRepeat) { TextButton(onClick = { adding = true }) { Text("+ Ekle") } }
        if (items.isEmpty()) Muted("Bu alanda rutin yok.")
        Slot.entries.forEach { slot ->
            val l = items.filter { it.slot == slot }
            if (l.isNotEmpty()) {
                Text(slot.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                l.forEach { r -> RoutineLine(r, onEdit = { edit = r }) { en -> repo.update { d -> d.copy(routines = d.routines.map { if (it.id == r.id) it.copy(enabled = en) else it }) } } }
            }
        }
    }
    edit?.let { r -> RoutineEditDialog(r, onDismiss = { edit = null }) }
    if (adding) RoutineEditDialog(null, defaultArea = area, onDismiss = { adding = false })
}

@Composable
fun RoutineLine(r: RoutineItem, onEdit: () -> Unit, onEnable: (Boolean) -> Unit) {
    val data = appData()
    val product = r.productId?.let { id -> data.products.firstOrNull { it.id == id } }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(r.title, style = MaterialTheme.typography.bodyMedium, color = if (r.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
            Muted(scheduleText(r.schedule) + (product?.let { " · ${it.brand} ${it.name}" } ?: ""), maxLines = 2)
        }
        Switch(r.enabled, onEnable)
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Düzenle") }
    }
}

@Composable
fun RoutineEditDialog(item: RoutineItem?, defaultArea: Area = Area.GENERAL, onDismiss: () -> Unit) {
    val data = appData()
    val repo = LocalRepo.current
    val base = item ?: RoutineItem(newId(), "", "", defaultArea, Slot.MORNING, order = (data.routines.maxOfOrNull { it.order } ?: 0) + 1)
    var title by remember { mutableStateOf(base.title) }
    var detail by remember { mutableStateOf(base.detail) }
    var area by remember { mutableStateOf(base.area) }
    var slot by remember { mutableStateOf(base.slot) }
    var type by remember { mutableStateOf(base.schedule.type) }
    var weekdays by remember { mutableStateOf(base.schedule.weekdays) }
    var everyN by remember { mutableStateOf(base.schedule.everyN.toString()) }
    var startDay by remember { mutableStateOf(base.schedule.startDay.toString()) }
    var endDay by remember { mutableStateOf(base.schedule.endDay.toString()) }
    var productId by remember { mutableStateOf(base.productId) }
    var productMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    FormDialog(if (item == null) "Rutin ekle" else "Rutini düzenle", onDismiss, saveEnabled = title.isNotBlank(), onSave = {
        val s = Schedule(type, weekdays.ifEmpty { (1..7).toSet() }, everyN.toIntOrNull()?.coerceIn(1, 60) ?: 1,
            startDay.toIntOrNull()?.coerceIn(1, 90) ?: 1, endDay.toIntOrNull()?.coerceIn(1, 90) ?: 90)
        val updated = base.copy(title = title.trim(), detail = detail.trim(), area = area, slot = slot, schedule = s, productId = productId)
        repo.update { d -> d.copy(routines = if (item == null) d.routines + updated else d.routines.map { if (it.id == base.id) updated else it }) }
    }) {
        TextInput("Başlık", title, { title = it })
        TextInput("Açıklama", detail, { detail = it }, singleLine = false)
        Text("Zaman", style = MaterialTheme.typography.labelLarge)
        ChipSelector(Slot.entries, slot, { it.label }) { slot = it }
        Text("Alan", style = MaterialTheme.typography.labelLarge)
        ChipSelector(Area.entries, area, { it.label }) { area = it }
        Text("Tekrar", style = MaterialTheme.typography.labelLarge)
        ChipSelector(ScheduleType.entries, type, { when (it) { ScheduleType.DAILY -> "Her gün"; ScheduleType.WEEKDAYS -> "Belirli günler"; ScheduleType.EVERY_N_DAYS -> "N günde bir" } }) { type = it }
        if (type == ScheduleType.WEEKDAYS) MultiChipSelector((1..7).toList(), weekdays, { isoDayShort(it) }) { d -> weekdays = if (d in weekdays) weekdays - d else weekdays + d }
        if (type == ScheduleType.EVERY_N_DAYS) NumberField("Kaç günde bir", everyN, { everyN = it }, Modifier.fillMaxWidth(), decimal = false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Başlangıç günü", startDay, { startDay = it }, Modifier.weight(1f), decimal = false)
            NumberField("Bitiş günü", endDay, { endDay = it }, Modifier.weight(1f), decimal = false)
        }
        Box {
            val p = productId?.let { id -> data.products.firstOrNull { it.id == id } }
            OutlinedButton(onClick = { productMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(p?.let { "Ürün: ${it.name}" } ?: "Ürün bağla (isteğe bağlı)", maxLines = 1) }
            DropdownMenu(productMenu, { productMenu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                DropdownMenuItem(text = { Text("Ürün yok") }, onClick = { productId = null; productMenu = false })
                data.products.sortedBy { it.category.ordinal }.forEach { pr ->
                    DropdownMenuItem(text = { Text("${pr.brand} – ${pr.name}", maxLines = 1) }, onClick = { productId = pr.id; productMenu = false })
                }
            }
        }
        if (item != null) TextButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, null); Text(" Rutini sil") }
    }
    if (confirmDelete && item != null) ConfirmDialog("Rutini sil", "\"${item.title}\" silinsin mi?", onConfirm = {
        repo.update { d -> d.copy(routines = d.routines.filterNot { it.id == item.id }) }
        onDismiss()
    }, onDismiss = { confirmDelete = false })
}

@Composable
fun RoutinesScreen() {
    val data = appData()
    val repo = LocalRepo.current
    var edit by remember { mutableStateOf<RoutineItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    Page("Rutinler", back = true, actions = {
        IconButton(onClick = { reset = true }) { Icon(Icons.Rounded.RestartAlt, "Varsayılana dön") }
    }, fab = { ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Rutin ekle") }) }) {
        ScreenColumn {
            Muted("Tüm günlük görevler bu rutinlerden oluşur. Kapattığın maddeler görev listesine gelmez; program günü aralığı ile aşamalı başlatabilirsin.")
            Slot.entries.forEach { slot ->
                val l = data.routines.filter { it.slot == slot }.sortedBy { it.order }
                if (l.isNotEmpty()) AppCard {
                    SectionHeader("${slot.label} (${l.count { it.enabled }}/${l.size} açık)")
                    l.forEach { r -> RoutineLine(r, onEdit = { edit = r }) { en -> repo.update { d -> d.copy(routines = d.routines.map { if (it.id == r.id) it.copy(enabled = en) else it }) } } }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }
    edit?.let { RoutineEditDialog(it, onDismiss = { edit = null }) }
    if (adding) RoutineEditDialog(null, onDismiss = { adding = false })
    if (reset) ConfirmDialog("Varsayılan rutinler", "Tüm rutinler başlangıç haline döndürülsün mü? Eklediğin rutinler silinir; görev geçmişin korunur.", confirm = "Sıfırla", onConfirm = {
        repo.update { it.copy(routines = Seed.defaultRoutines()) }
    }, onDismiss = { reset = false })
}
