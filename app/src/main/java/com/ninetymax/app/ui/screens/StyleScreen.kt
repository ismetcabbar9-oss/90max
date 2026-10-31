package com.ninetymax.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ninetymax.app.data.*
import com.ninetymax.app.seed.Guides
import com.ninetymax.app.ui.LocalRepo
import com.ninetymax.app.ui.appData
import com.ninetymax.app.ui.components.*
import com.ninetymax.app.ui.theme.areaColor
import java.io.File

@Composable
fun StyleScreen() {
    var editItem by remember { mutableStateOf<ClothingItem?>(null) }
    var newItem by remember { mutableStateOf(false) }
    var newOutfit by remember { mutableStateOf(false) }
    TabbedPage("Stil", listOf("Clean Boy rehberi", "Gardırop", "Kombinler"), fab = { tab ->
        if (tab == 1) ExtendedFloatingActionButton(onClick = { newItem = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Parça ekle") })
        if (tab == 2) ExtendedFloatingActionButton(onClick = { newOutfit = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Kombin oluştur") })
    }) { tab ->
        when (tab) {
            0 -> AdaptiveColumns(left = { GuideCards(Guides.style.take(2)) }, right = { GuideCards(Guides.style.drop(2)); RoutineSection(Area.STYLE, "Stil görevleri") })
            1 -> Wardrobe { editItem = it }
            else -> Outfits()
        }
    }
    if (newItem) ClothingDialog(null) { newItem = false }
    editItem?.let { ClothingDialog(it) { editItem = null } }
    if (newOutfit) OutfitDialog { newOutfit = false }
}

@Composable
private fun ClothingThumb(item: ClothingItem, size: androidx.compose.ui.unit.Dp) {
    val repo = LocalRepo.current
    val f = item.imageFile?.let { File(repo.photoDir, it) }
    if (f != null && f.exists()) PhotoThumb(f, Modifier.size(size)) else IconBadge(Icons.Rounded.Checkroom, areaColor(Area.STYLE), size)
}

@Composable
private fun Wardrobe(onEdit: (ClothingItem) -> Unit) {
    val data = appData()
    var type by remember { mutableStateOf<ClothingType?>(null) }
    var status by remember { mutableStateOf<ClothingStatus?>(null) }
    val list = data.wardrobe.filter { (type == null || it.type == type) && (status == null || it.status == status) }
    val wish = data.wardrobe.filter { it.status == ClothingStatus.WISHLIST }.sumOf { it.priceTl ?: 0.0 }
    ScreenColumn {
        AppCard {
            SectionHeader("Kıyafet bütçesi", Icons.Rounded.AccountBalanceWallet)
            LabeledProgress("Alınacaklar toplamı", (wish / data.settings.clothingBudget).toFloat(), "${formatTl(wish)} / ${formatTl(data.settings.clothingBudget)}")
            Muted("Bütçeyi Ürünler > Alışveriş & bütçe ekranından değiştirebilirsin.")
        }
        ChipSelector(listOf<ClothingType?>(null) + ClothingType.entries, type, { it?.label ?: "Tümü" }) { type = it }
        ChipSelector(listOf<ClothingStatus?>(null) + ClothingStatus.entries, status, { it?.label ?: "Hepsi" }) { status = it }
        if (list.isEmpty()) EmptyState("Gardırobuna parça ekle; kombin oluştururken kullanacaksın.", Icons.Rounded.Checkroom)
        list.forEach { item ->
            AppCard(onClick = { onEdit(item) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClothingThumb(item, 52.dp); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Muted("${item.type.label} · ${item.color.ifBlank { "renk yok" }} · ${item.status.label}" + (item.priceTl?.let { " · ${formatTl(it)}" } ?: ""))
                    }
                }
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun ClothingDialog(item: ClothingItem?, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val base = item ?: ClothingItem(newId(), "", ClothingType.TOP)
    var name by remember { mutableStateOf(base.name) }
    var type by remember { mutableStateOf(base.type) }
    var color by remember { mutableStateOf(base.color) }
    var status by remember { mutableStateOf(base.status) }
    var price by remember { mutableStateOf(base.priceTl?.fmt() ?: "") }
    var note by remember { mutableStateOf(base.note) }
    var image by remember { mutableStateOf(base.imageFile) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { Photos.importImage(context, it, repo.photoDir)?.let { f -> image = f } }
    }
    FormDialog(if (item == null) "Parça ekle" else "Parçayı düzenle", onDismiss, saveEnabled = name.isNotBlank(), onSave = {
        val u = base.copy(name = name.trim(), type = type, color = color.trim(), status = status, priceTl = price.toDoubleTr(), note = note, imageFile = image)
        repo.update { d -> d.copy(wardrobe = if (item == null) d.wardrobe + u else d.wardrobe.map { if (it.id == u.id) u else it }) }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val f = image?.let { File(repo.photoDir, it) }
            if (f != null) PhotoThumb(f, Modifier.size(56.dp))
            TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Rounded.AddAPhoto, null); Text(" Fotoğraf") }
        }
        TextInput("Ad (ör. Beyaz oxford gömlek)", name, { name = it })
        ChipSelector(ClothingType.entries, type, { it.label }) { type = it }
        TextInput("Renk", color, { color = it })
        ChipSelector(ClothingStatus.entries, status, { it.label }) { status = it }
        NumberField("Fiyat", price, { price = it }, Modifier.fillMaxWidth(), suffix = "₺")
        TextInput("Not", note, { note = it })
        if (item != null) TextButton(onClick = {
            repo.update { d -> d.copy(wardrobe = d.wardrobe.filterNot { it.id == item.id }, outfits = d.outfits.map { o -> o.copy(itemIds = o.itemIds - item.id) }) }
            onDismiss()
        }) { Icon(Icons.Rounded.Delete, null); Text(" Sil") }
    }
}

@Composable
private fun Outfits() {
    val data = appData()
    val repo = LocalRepo.current
    ScreenColumn {
        if (data.outfits.isEmpty()) EmptyState("Henüz kombin yok. Önce gardırobuna parça ekle, sonra kombin oluştur.", Icons.Rounded.Style)
        data.outfits.forEach { o ->
            val items = o.itemIds.mapNotNull { id -> data.wardrobe.firstOrNull { it.id == id } }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(o.name, style = MaterialTheme.typography.titleMedium)
                        if (o.occasion.isNotBlank()) Muted(o.occasion)
                    }
                    IconButton(onClick = { repo.update { d -> d.copy(outfits = d.outfits.filterNot { it.id == o.id }) } }) { Icon(Icons.Rounded.Delete, "Sil") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items.take(5).forEach { ClothingThumb(it, 48.dp) } }
                Muted(items.joinToString(" + ") { it.name })
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun OutfitDialog(onDismiss: () -> Unit) {
    val data = appData()
    val repo = LocalRepo.current
    var name by remember { mutableStateOf("") }
    var occasion by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    FormDialog("Kombin oluştur", onDismiss, saveEnabled = name.isNotBlank() && selected.isNotEmpty(), onSave = {
        repo.update { d -> d.copy(outfits = d.outfits + Outfit(newId(), name.trim(), selected.toList(), occasion.trim())) }
    }) {
        TextInput("Kombin adı", name, { name = it })
        TextInput("Ortam (ör. günlük, akşam yemeği, ofis)", occasion, { occasion = it })
        val owned = data.wardrobe.filter { it.status == ClothingStatus.OWNED }
        if (owned.isEmpty()) Muted("Gardırobunda sahip olduğun parça yok.")
        ClothingType.entries.forEach { t ->
            val l = owned.filter { it.type == t }
            if (l.isNotEmpty()) {
                Text(t.label, style = MaterialTheme.typography.labelLarge)
                l.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(c.id in selected, { selected = if (it) selected + c.id else selected - c.id })
                        Text("${c.name} ${if (c.color.isNotBlank()) "(${c.color})" else ""}")
                    }
                }
            }
        }
    }
}
