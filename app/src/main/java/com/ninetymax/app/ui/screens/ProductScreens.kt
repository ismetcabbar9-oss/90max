package com.ninetymax.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.ninetymax.app.domain.DailyTasks
import com.ninetymax.app.seed.Guides
import com.ninetymax.app.seed.ProductCatalog
import com.ninetymax.app.ui.LocalNav
import com.ninetymax.app.ui.LocalRepo
import com.ninetymax.app.ui.Page
import com.ninetymax.app.ui.Routes
import com.ninetymax.app.ui.appData
import com.ninetymax.app.ui.components.*
import com.ninetymax.app.ui.theme.areaColor
import java.io.File
import java.time.LocalDate

@Composable
fun ProductsScreen() {
    val nav = LocalNav.current
    TabbedPage("Ürünler", listOf("Katalog", "Benim ürünlerim", "Alışveriş & bütçe", "Kullanım takvimi"), fab = { tab ->
        if (tab <= 1) ExtendedFloatingActionButton(onClick = { nav.navigate(Routes.productEdit("new")) }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Ürün ekle") })
    }) { tab ->
        when (tab) { 0 -> Catalog(); 1 -> MyProducts(); 2 -> Shopping(); else -> UsageCalendar() }
    }
}

@Composable
fun ProductImage(p: Product, size: androidx.compose.ui.unit.Dp) {
    val repo = LocalRepo.current
    val f = p.imageFile?.let { File(repo.photoDir, it) }
    if (f != null && f.exists()) PhotoThumb(f, Modifier.size(size))
    else IconBadge(categoryIcon(p.category), areaColor(categoryArea(p.category)), size)
}

@Composable
fun ProductRow(p: Product) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    AppCard(onClick = { nav.navigate(Routes.product(p.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductImage(p, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Muted("${p.brand} · ${p.category.label}", maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (p.status != ProductStatus.NONE) StatusPill(p.status)
                    Muted(p.priceTl?.let { formatTl(it) } ?: "Fiyat girilmedi")
                }
            }
            IconButton(onClick = { repo.update { d -> d.copy(products = d.products.map { if (it.id == p.id) it.copy(favorite = !it.favorite) else it }) } }) {
                Icon(if (p.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favori", tint = if (p.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun StatusPill(s: ProductStatus) {
    val color = when (s) {
        ProductStatus.TO_BUY -> MaterialTheme.colorScheme.tertiaryContainer
        ProductStatus.IN_USE -> MaterialTheme.colorScheme.secondaryContainer
        ProductStatus.BOUGHT -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = color, shape = MaterialTheme.shapes.small) { Text(s.label, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun Catalog() {
    val data = appData()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ProductCategory?>(null) }
    var favOnly by remember { mutableStateOf(false) }
    val list = data.products.filter {
        (query.isBlank() || it.name.contains(query, true) || it.brand.contains(query, true) || it.purpose.contains(query, true)) &&
            (category == null || it.category == category) && (!favOnly || it.favorite)
    }.sortedWith(compareByDescending<Product> { it.favorite }.thenBy { it.category.ordinal })
    ScreenColumn {
        OutlinedTextField(query, { query = it }, label = { Text("Ürün, marka veya amaç ara") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChipSelector(listOf<ProductCategory?>(null) + ProductCategory.entries, category, { it?.label ?: "Tümü" }) { category = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(favOnly, { favOnly = it }); Text("  Sadece favoriler") }
        InfoBanner("Katalogdaki ürünler Türkiye'de yaygın bulunan gerçek ürünlerdir. Fiyatlar sık değiştiği için uydurulmadı: fiyatı kontrol edip kendin gir. 'Fiyat karşılaştır' bağlantısı internet gerektirir.")
        Muted("${list.size} ürün")
        ProductGrid(list)
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun ProductGrid(list: List<Product>) {
    if (LocalWide.current) {
        list.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { Box(Modifier.weight(1f)) { ProductRow(it) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    } else list.forEach { ProductRow(it) }
}

@Composable
private fun MyProducts() {
    val data = appData()
    ScreenColumn {
        val mine = data.products.filter { it.status != ProductStatus.NONE || it.custom }
        if (mine.isEmpty()) EmptyState("Katalogdan bir ürünün durumunu 'Satın alınacak' veya 'Kullanılıyor' yaptığında burada görünür.", Icons.Rounded.Inventory2)
        listOf(ProductStatus.IN_USE, ProductStatus.BOUGHT, ProductStatus.TO_BUY, ProductStatus.FINISHED, ProductStatus.STOPPED, ProductStatus.NONE).forEach { s ->
            val l = mine.filter { it.status == s }
            if (l.isNotEmpty()) {
                SectionHeader("${s.label} (${l.size})")
                ProductGrid(l)
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun Shopping() {
    val data = appData()
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val toBuy = data.products.filter { it.status == ProductStatus.TO_BUY }
    val clothes = data.wardrobe.filter { it.status == ClothingStatus.WISHLIST }
    val knownTotal = toBuy.sumOf { it.priceTl ?: 0.0 }
    val unknown = toBuy.count { it.priceTl == null }
    val month = today().withDayOfMonth(1)
    val spent = data.products.flatMap { it.purchases }.filter { p -> p.date.toDateOrNull()?.let { !it.isBefore(month) } == true }.sumOf { it.priceTl ?: 0.0 }
    val budget = data.settings.monthlyCareBudget
    var buying by remember { mutableStateOf<Product?>(null) }
    var budgetOpen by remember { mutableStateOf(false) }
    AdaptiveColumns(
        left = {
            AppCard {
                SectionHeader("Aylık bakım bütçesi", Icons.Rounded.AccountBalanceWallet) { TextButton(onClick = { budgetOpen = true }) { Text("Düzenle") } }
                LabeledProgress("Bu ay harcanan", (spent / budget).toFloat(), "${formatTl(spent)} / ${formatTl(budget)}")
                KeyValue("Listedeki tahmini maliyet", formatTl(knownTotal) + if (unknown > 0) " + $unknown ürün fiyatsız" else "")
                KeyValue("Kalan bütçe", formatTl((budget - spent).coerceAtLeast(0.0)))
                if (spent + knownTotal > budget) InfoBanner("Liste bu ayki bütçeyi aşıyor. Öncelikli ürünleri seç veya katalogdaki alternatiflere bak.", Icons.Rounded.Warning, MaterialTheme.colorScheme.tertiaryContainer)
            }
            if (clothes.isNotEmpty()) AppCard {
                SectionHeader("Kıyafet listesi", Icons.Rounded.Checkroom)
                clothes.forEach { KeyValue(it.name, it.priceTl?.let { p -> formatTl(p) } ?: "fiyat yok") }
                KeyValue("Toplam", formatTl(clothes.sumOf { it.priceTl ?: 0.0 }) + " / bütçe ${formatTl(data.settings.clothingBudget)}")
            }
        },
        right = {
            SectionHeader("Alışveriş listesi (${toBuy.size})", Icons.Rounded.ShoppingCart)
            if (toBuy.isEmpty()) EmptyState("Liste boş. Bir ürünün durumunu 'Satın alınacak' yap.", Icons.Rounded.ShoppingCart)
            toBuy.forEach { p ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProductImage(p, 40.dp); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            Muted("${p.brand} · ${p.priceTl?.let { formatTl(it) } ?: "fiyat girilmedi"}")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { buying = p }) { Text("Satın aldım") }
                        OutlinedButton(onClick = { nav.navigate(Routes.product(p.id)) }) { Text("Detay") }
                    }
                }
            }
        },
    )
    buying?.let { p ->
        var price by remember { mutableStateOf(p.priceTl?.fmt() ?: "") }
        FormDialog("Satın alındı: ${p.name}", { buying = null }, onSave = {
            val pr = price.toDoubleTr()
            repo.update { d -> d.copy(products = d.products.map {
                if (it.id == p.id) it.copy(status = ProductStatus.BOUGHT, purchases = it.purchases + Purchase(today().iso(), pr),
                    priceTl = pr ?: it.priceTl, priceCheckedDate = if (pr != null) today().iso() else it.priceCheckedDate) else it
            }) }
        }) { NumberField("Ödenen fiyat (isteğe bağlı)", price, { price = it }, Modifier.fillMaxWidth(), suffix = "₺") }
    }
    if (budgetOpen) {
        var b by remember { mutableStateOf(budget.fmt()) }
        var c by remember { mutableStateOf(data.settings.clothingBudget.fmt()) }
        FormDialog("Bütçeler", { budgetOpen = false }, onSave = {
            repo.update { d -> d.copy(settings = d.settings.copy(monthlyCareBudget = b.toDoubleTr() ?: d.settings.monthlyCareBudget, clothingBudget = c.toDoubleTr() ?: d.settings.clothingBudget)) }
        }) {
            NumberField("Aylık bakım/takviye bütçesi", b, { b = it }, Modifier.fillMaxWidth(), suffix = "₺")
            NumberField("Toplam kıyafet bütçesi", c, { c = it }, Modifier.fillMaxWidth(), suffix = "₺")
        }
    }
}

@Composable
private fun UsageCalendar() {
    val data = appData()
    val nav = LocalNav.current
    val start = today()
    ScreenColumn {
        Muted("Rutinlerine bağlı ürünler ve kullandığın takviyeler önümüzdeki 7 gün için. Rutinleri Ayarlar > Rutinler'den değiştirebilirsin.")
        (0..6).forEach { off ->
            val date = start.plusDays(off.toLong())
            val tasks = DailyTasks.forDate(data, date).filter { it.productId != null }
            AppCard {
                Text(if (off == 0) "Bugün · ${date.shortTr()}" else date.longTr(), style = MaterialTheme.typography.titleSmall)
                if (tasks.isEmpty()) Muted("Bağlı ürün yok")
                tasks.groupBy { it.slot }.forEach { (slot, list) ->
                    Text(slot.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    list.forEach { t ->
                        val p = data.products.firstOrNull { it.id == t.productId }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ${p?.name ?: t.title}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (p != null) IconButton(onClick = { nav.navigate(Routes.product(p.id)) }) { Icon(Icons.Rounded.ChevronRight, "Aç") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailScreen(id: String) {
    val data = appData()
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val p = data.products.firstOrNull { it.id == id }
    var confirmDelete by remember { mutableStateOf(false) }
    Page(p?.name ?: "Ürün", back = true, actions = {
        if (p != null) {
            IconButton(onClick = { nav.navigate(Routes.productEdit(p.id)) }) { Icon(Icons.Rounded.Edit, "Düzenle") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Sil") }
        }
    }) {
        if (p == null) { EmptyState("Ürün bulunamadı."); return@Page }
        fun upd(f: (Product) -> Product) = repo.update { d -> d.copy(products = d.products.map { if (it.id == p.id) f(it) else it }) }
        val usedIn = data.routines.filter { it.productId == p.id }
        AdaptiveColumns(
            left = {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProductImage(p, 88.dp); Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.brand, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(p.name, style = MaterialTheme.typography.titleLarge)
                            Muted(p.category.label)
                        }
                        IconButton(onClick = { upd { it.copy(favorite = !it.favorite) } }) {
                            Icon(if (p.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favori", tint = if (p.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (p.imageFile == null) Muted("Görsel: ürün fotoğrafı eklemek için Düzenle'ye dokun (katalog görselleri telif nedeniyle eklenmedi).")
                }
                AppCard {
                    SectionHeader("Durum", Icons.Rounded.Flag)
                    ChipSelector(ProductStatus.entries, p.status, { it.label }) { s -> upd { it.copy(status = s) } }
                    if (p.category == ProductCategory.SUPPLEMENT || p.dailyTracking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(p.dailyTracking, { v -> upd { it.copy(dailyTracking = v) } })
                            Text("  Kullanılırken günlük görev olarak takip et", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                AppCard {
                    SectionHeader("Fiyat", Icons.Rounded.Sell)
                    KeyValue("Tahmini fiyat", p.priceTl?.let { formatTl(it) } ?: "Girilmedi")
                    KeyValue("Kontrol tarihi", p.priceCheckedDate?.toDateOrNull()?.longTr() ?: "Kontrol edilmedi")
                    p.link?.let { link ->
                        OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) } }) {
                            Icon(Icons.Rounded.OpenInNew, null); Text(" Fiyat karşılaştır / satın al")
                        }
                        Muted("İnternet bağlantısı gerektirir; tarayıcıda açılır.")
                    }
                    if (p.purchases.isNotEmpty()) {
                        Text("Satın alma geçmişi", style = MaterialTheme.typography.labelLarge)
                        p.purchases.forEach { KeyValue(it.date, it.priceTl?.let { v -> formatTl(v) } ?: "—") }
                    }
                }
            },
            right = {
                AppCard {
                    SectionHeader("Ne işe yarar", Icons.Rounded.Lightbulb); Text(p.purpose.ifBlank { "—" })
                    SectionHeader("Nasıl kullanılır", Icons.Rounded.MenuBook); Text(p.howTo.ifBlank { "—" })
                    KeyValue("Sıklık", p.frequency.ifBlank { "—" })
                    KeyValue("Zaman", p.timing.label)
                    if (p.dose.isNotBlank()) KeyValue("Genel kullanım", p.dose)
                }
                if (p.warnings.isNotBlank() || p.category == ProductCategory.SUPPLEMENT) AppCard {
                    SectionHeader("Uyarılar", Icons.Rounded.WarningAmber)
                    if (p.warnings.isNotBlank()) Text(p.warnings)
                    if (p.category == ProductCategory.SUPPLEMENT || p.category == ProductCategory.SKIN_TREATMENT) Muted(Guides.DISCLAIMER)
                }
                if (usedIn.isNotEmpty()) AppCard {
                    SectionHeader("Rutinlerde", Icons.Rounded.EventRepeat)
                    usedIn.forEach { r -> Text("• ${r.slot.label}: ${r.title}" + if (!r.enabled) " (kapalı)" else "", style = MaterialTheme.typography.bodyMedium) }
                }
                val alts = p.alternativeIds.mapNotNull { a -> data.products.firstOrNull { it.id == a } }
                if (alts.isNotEmpty()) AppCard {
                    SectionHeader("Alternatifler", Icons.Rounded.SwapHoriz)
                    alts.forEach { a ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(a.name, style = MaterialTheme.typography.bodyMedium); Muted(a.brand) }
                            IconButton(onClick = { nav.navigate(Routes.product(a.id)) }) { Icon(Icons.Rounded.ChevronRight, "Aç") }
                        }
                    }
                }
                if (p.notes.isNotBlank()) AppCard { SectionHeader("Notlarım", Icons.Rounded.Notes); Text(p.notes) }
            },
        )
    }
    if (confirmDelete && p != null) ConfirmDialog("Ürünü sil", "${p.name} silinsin mi? Bağlı rutinlerdeki ürün bağlantısı kaldırılır.", onConfirm = {
        repo.update { d ->
            val seed = ProductCatalog.products.any { it.id == p.id }
            d.copy(
                products = d.products.filterNot { it.id == p.id },
                deletedSeedProductIds = if (seed) d.deletedSeedProductIds + p.id else d.deletedSeedProductIds,
                routines = d.routines.map { if (it.productId == p.id) it.copy(productId = null) else it },
            )
        }
        nav.popBackStack()
    }, onDismiss = { confirmDelete = false })
}

@Composable
fun ProductEditScreen(id: String) {
    val data = appData()
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val existing = data.products.firstOrNull { it.id == id }
    val base = existing ?: Product(newId(), "", "", ProductCategory.SKIN_TREATMENT, custom = true)
    var name by remember { mutableStateOf(base.name) }
    var brand by remember { mutableStateOf(base.brand) }
    var category by remember { mutableStateOf(base.category) }
    var purpose by remember { mutableStateOf(base.purpose) }
    var howTo by remember { mutableStateOf(base.howTo) }
    var frequency by remember { mutableStateOf(base.frequency) }
    var timing by remember { mutableStateOf(base.timing) }
    var warnings by remember { mutableStateOf(base.warnings) }
    var price by remember { mutableStateOf(base.priceTl?.fmt() ?: "") }
    var checked by remember { mutableStateOf(base.priceCheckedDate) }
    var link by remember { mutableStateOf(base.link ?: "") }
    var notes by remember { mutableStateOf(base.notes) }
    var dose by remember { mutableStateOf(base.dose) }
    var image by remember { mutableStateOf(base.imageFile) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { Photos.importImage(context, it, repo.photoDir)?.let { f -> image = f } }
    }
    Page(if (existing == null) "Yeni ürün" else "Ürünü düzenle", back = true) {
        ScreenColumn {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val f = image?.let { File(repo.photoDir, it) }
                if (f != null) PhotoThumb(f, Modifier.size(72.dp)) else IconBadge(categoryIcon(category), areaColor(categoryArea(category)), 72.dp)
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Rounded.AddAPhoto, null); Text(" Görsel seç") }
                if (image != null) TextButton(onClick = { image = null }) { Text("Kaldır") }
            }
            TextInput("Ürün adı *", name, { name = it })
            TextInput("Marka", brand, { brand = it })
            Text("Kategori", style = MaterialTheme.typography.labelLarge)
            ChipSelector(ProductCategory.entries, category, { it.label }) { category = it }
            TextInput("Ne işe yarar", purpose, { purpose = it }, singleLine = false)
            TextInput("Nasıl kullanılır", howTo, { howTo = it }, singleLine = false)
            TextInput("Kullanım sıklığı", frequency, { frequency = it })
            Text("Kullanım zamanı", style = MaterialTheme.typography.labelLarge)
            ChipSelector(Timing.entries, timing, { it.label }) { timing = it }
            if (category == ProductCategory.SUPPLEMENT) TextInput("Genel kullanım miktarı (etiket)", dose, { dose = it })
            TextInput("Uyarılar", warnings, { warnings = it }, singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField("Tahmini fiyat", price, { price = it; if (checked == null) checked = today().iso() }, Modifier.weight(1f), suffix = "₺")
                DateButton("Kontrol", checked, { checked = it.iso() })
            }
            TextInput("Satın alma bağlantısı", link, { link = it })
            TextInput("Notlarım", notes, { notes = it }, singleLine = false)
            Button(onClick = {
                val updated = base.copy(
                    name = name.trim(), brand = brand.trim(), category = category, purpose = purpose, howTo = howTo, frequency = frequency,
                    timing = timing, warnings = warnings, priceTl = price.toDoubleTr(), priceCheckedDate = if (price.isBlank()) null else checked,
                    link = link.trim().ifBlank { null }, notes = notes, dose = dose, imageFile = image,
                    dailyTracking = if (existing == null) category == ProductCategory.SUPPLEMENT else base.dailyTracking,
                )
                repo.update { d -> d.copy(products = if (existing == null) d.products + updated else d.products.map { if (it.id == base.id) updated else it }) }
                nav.popBackStack()
            }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Kaydet") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SupplementsScreen() {
    val data = appData()
    val repo = LocalRepo.current
    val date = today()
    val st = DailyTasks.status(data, date)
    val supps = data.products.filter { it.category == ProductCategory.SUPPLEMENT }
    Page("Takviyeler") {
        AdaptiveColumns(
            left = {
                InfoBanner(Guides.DISCLAIMER, Icons.Rounded.HealthAndSafety, MaterialTheme.colorScheme.tertiaryContainer)
                Guides.supplements.forEach { g -> AppCard { SectionHeader(g.title, Icons.Rounded.Lightbulb); BulletList(g.points) } }
            },
            right = {
                val inUse = supps.filter { it.status == ProductStatus.IN_USE && it.dailyTracking }
                AppCard {
                    SectionHeader("Bugünkü takip", Icons.Rounded.TaskAlt)
                    if (inUse.isEmpty()) Muted("Kullandığın bir takviye yok. Aşağıdan durumunu 'Kullanılıyor' yaparsan burada ve Bugün ekranında görünür.")
                    inUse.forEach { p ->
                        val tid = "s:${p.id}"
                        TaskRow(p.name, "${p.dose} · ${p.timing.label}", Area.SUPPLEMENT, tid in st.done, { repo.update { DailyTasks.toggle(it, date, tid) } })
                    }
                    if (inUse.isNotEmpty()) {
                        val last7 = (0..6).map { date.minusDays(it.toLong()).iso() }
                        val taken = last7.sumOf { d -> inUse.count { "s:${it.id}" in (data.taskStatus[d]?.done ?: emptySet()) } }
                        Muted("Son 7 gün: ${taken} / ${inUse.size * 7} kullanım işaretlendi")
                    }
                }
                supps.forEach { ProductRow(it) }
            },
        )
    }
}
