package com.ninetymax.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ninetymax.app.data.Area
import com.ninetymax.app.data.Photos
import com.ninetymax.app.data.ProductCategory
import com.ninetymax.app.domain.Muscle
import com.ninetymax.app.ui.theme.areaColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Geniş ekran düzeni (tablet / yatay) için ortak bilgi. */
val LocalWide = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** Telefonda tek sütun, geniş ekranda iki sütun. */
@Composable
fun AdaptiveColumns(left: @Composable ColumnScope.() -> Unit, right: @Composable ColumnScope.() -> Unit) {
    if (LocalWide.current) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenColumn(Modifier.weight(1f), left)
            ScreenColumn(Modifier.weight(1f), right)
        }
    } else {
        ScreenColumn { left(); right() }
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = CardDefaults.outlinedCardBorder()
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
    if (onClick != null) Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, border = border, content = inner)
    else Card(modifier = modifier.fillMaxWidth(), colors = colors, border = border, content = inner)
}

@Composable
fun SectionHeader(title: String, icon: ImageVector? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) =
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

@Composable
fun ProgressRing(percent: Int, size: Dp = 96.dp, stroke: Dp = 10.dp, label: String? = null) {
    val animated by animateFloatAsState(percent / 100f, label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val color = MaterialTheme.colorScheme.primary
    Box(Modifier.size(size).semantics { contentDescription = "Tamamlanma yüzde $percent" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            drawArc(track, 0f, 360f, false, topLeft = Offset(s / 2, s / 2), size = arcSize, style = Stroke(s))
            drawArc(color, -90f, 360f * animated, false, topLeft = Offset(s / 2, s / 2), size = arcSize, style = Stroke(s, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%$percent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (label != null) Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LabeledProgress(label: String, value: Float, trailing: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row { Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)); Text(trailing, style = MaterialTheme.typography.labelLarge) }
        LinearProgressIndicator(progress = { value.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
    }
}

fun areaIcon(area: Area): ImageVector = when (area) {
    Area.SKIN -> Icons.Rounded.Face
    Area.HAIR -> Icons.Rounded.Brush
    Area.BEARD -> Icons.Rounded.ContentCut
    Area.ORAL -> Icons.Rounded.SentimentSatisfied
    Area.BODY -> Icons.Rounded.Shower
    Area.POSTURE -> Icons.Rounded.SelfImprovement
    Area.SLEEP -> Icons.Rounded.Bedtime
    Area.NUTRITION -> Icons.Rounded.Restaurant
    Area.SUPPLEMENT -> Icons.Rounded.Medication
    Area.WORKOUT -> Icons.Rounded.FitnessCenter
    Area.STYLE -> Icons.Rounded.Checkroom
    Area.GENERAL -> Icons.Rounded.TaskAlt
}

fun categoryIcon(c: ProductCategory): ImageVector = when (c) {
    ProductCategory.CLEANSER -> Icons.Rounded.WaterDrop
    ProductCategory.MOISTURIZER -> Icons.Rounded.Opacity
    ProductCategory.SUNSCREEN -> Icons.Rounded.WbSunny
    ProductCategory.SKIN_TREATMENT -> Icons.Rounded.Science
    ProductCategory.HAIR -> Icons.Rounded.Brush
    ProductCategory.CURL -> Icons.Rounded.Waves
    ProductCategory.BEARD -> Icons.Rounded.ContentCut
    ProductCategory.ORAL -> Icons.Rounded.SentimentSatisfied
    ProductCategory.HAND_NAIL -> Icons.Rounded.BackHand
    ProductCategory.BODY -> Icons.Rounded.Shower
    ProductCategory.FRAGRANCE -> Icons.Rounded.Air
    ProductCategory.SPORT -> Icons.Rounded.FitnessCenter
    ProductCategory.SUPPLEMENT -> Icons.Rounded.Medication
}

fun categoryArea(c: ProductCategory): Area = when (c) {
    ProductCategory.CLEANSER, ProductCategory.MOISTURIZER, ProductCategory.SUNSCREEN, ProductCategory.SKIN_TREATMENT -> Area.SKIN
    ProductCategory.HAIR, ProductCategory.CURL -> Area.HAIR
    ProductCategory.BEARD -> Area.BEARD
    ProductCategory.ORAL -> Area.ORAL
    ProductCategory.HAND_NAIL, ProductCategory.BODY, ProductCategory.FRAGRANCE -> Area.BODY
    ProductCategory.SPORT -> Area.WORKOUT
    ProductCategory.SUPPLEMENT -> Area.SUPPLEMENT
}

@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 3)).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
fun TaskRow(
    title: String,
    detail: String,
    area: Area,
    done: Boolean,
    onToggle: () -> Unit,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = done, onCheckedChange = { onToggle() })
        IconBadge(areaIcon(area), areaColor(area), 32.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (detail.isNotBlank()) Muted(detail, maxLines = 2)
        }
        trailing?.invoke(this)
    }
}

@Composable
fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipSelector(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) }) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MultiChipSelector(options: List<T>, selected: Set<T>, label: (T) -> String, onToggle: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o -> FilterChip(selected = o in selected, onClick = { onToggle(o) }, label = { Text(label(o)) }) }
    }
}

@Composable
fun RatingRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        (1..5).forEach { i ->
            IconButton(onClick = { onChange(i) }, modifier = Modifier.size(36.dp)) {
                Icon(if (i <= value) Icons.Rounded.Star else Icons.Rounded.StarOutline, "$i puan", tint = if (i <= value) Color(0xFFF4A300) else MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, suffix: String? = null, decimal: Boolean = true) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = modifier,
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
    )
}

@Composable
fun TextInput(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine, modifier = modifier.fillMaxWidth())
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "Sil", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
fun EmptyState(text: String, icon: ImageVector = Icons.Rounded.Inbox) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
        Muted(text)
    }
}

@Composable
fun InfoBanner(text: String, icon: ImageVector = Icons.Rounded.Info, color: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(color = color, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BulletList(points: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        points.forEach { p ->
            Row {
                Text("•  ", color = MaterialTheme.colorScheme.primary)
                Text(p, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Basit çizgi grafik. [points] x sırasına göre verilmelidir. */
@Composable
fun LineChart(values: List<Float>, modifier: Modifier = Modifier.fillMaxWidth().height(160.dp), labels: Pair<String, String>? = null, unit: String = "") {
    if (values.size < 2) {
        Muted("Grafik için en az 2 kayıt gerekiyor.", Modifier.padding(vertical = 12.dp)); return
    }
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val min = values.min(); val max = values.max()
    val range = (max - min).takeIf { it > 0f } ?: 1f
    Column {
        Row { Muted("${max.fmt1()}$unit", Modifier.weight(1f)); Muted("min ${min.fmt1()}$unit") }
        Canvas(modifier.padding(vertical = 6.dp)) {
            val w = size.width; val h = size.height
            for (i in 0..3) drawLine(grid, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), 1f)
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = w * i / (values.size - 1)
                val y = h - (v - min) / range * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(line, 4.dp.toPx(), Offset(x, y))
            }
            drawPath(path, line, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        if (labels != null) Row { Muted(labels.first, Modifier.weight(1f)); Muted(labels.second) }
    }
}

@Composable
fun BarChart(values: List<Int>, modifier: Modifier = Modifier.fillMaxWidth().height(120.dp), maxValue: Int = 100) {
    val bar = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val gap = 3.dp.toPx()
        val bw = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(2f)
        values.forEachIndexed { i, v ->
            val x = i * (bw + gap)
            drawRoundRect(track, Offset(x, 0f), Size(bw, size.height), CornerRadius(bw / 3))
            val bh = size.height * (v.coerceIn(0, maxValue) / maxValue.toFloat())
            drawRoundRect(bar, Offset(x, size.height - bh), Size(bw, bh), CornerRadius(bw / 3))
        }
    }
}

private fun Float.fmt1(): String = if (this % 1f == 0f) toInt().toString() else String.format("%.1f", this)

/**
 * Hareketin çalıştırdığı kasları gösteren şematik vücut haritası (ön ve arka görünüm).
 */
@Composable
fun BodyMap(muscles: List<Muscle>, modifier: Modifier = Modifier.size(width = 180.dp, height = 150.dp)) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    fun col(m: Muscle) = when {
        muscles.firstOrNull() == m -> primary
        m in muscles -> secondary
        else -> base
    }
    Canvas(modifier.semantics { contentDescription = "Çalışan kaslar: " + muscles.joinToString { it.label } }) {
        val figW = size.width / 2f
        fun figure(ox: Float, back: Boolean) {
            val u = figW / 10f
            val cx = ox + figW / 2f
            fun rect(x: Float, y: Float, w: Float, h: Float, c: Color) =
                drawRoundRect(c, Offset(cx + x * u, y * u), Size(w * u, h * u), CornerRadius(u * 0.6f))
            drawCircle(base, u * 1.1f, Offset(cx, u * 1.3f))
            // gövde
            if (!back) {
                rect(-2.2f, 3f, 4.4f, 1.8f, col(Muscle.UPPER_CHEST))
                rect(-2.2f, 4.9f, 4.4f, 1.6f, col(Muscle.CHEST))
                rect(-1.5f, 6.7f, 3f, 3.6f, col(Muscle.CORE))
            } else {
                rect(-2.2f, 3f, 4.4f, 2.2f, col(Muscle.UPPER_BACK))
                rect(-2.6f, 5.3f, 5.2f, 2.6f, col(Muscle.LATS))
                rect(-1.5f, 8f, 3f, 2.3f, col(Muscle.CORE))
            }
            // omuzlar
            val delt = if (back) col(Muscle.REAR_DELT) else col(Muscle.FRONT_DELT)
            rect(-3.6f, 2.8f, 1.3f, 1.6f, if (Muscle.SIDE_DELT in muscles) col(Muscle.SIDE_DELT) else delt)
            rect(2.3f, 2.8f, 1.3f, 1.6f, if (Muscle.SIDE_DELT in muscles) col(Muscle.SIDE_DELT) else delt)
            // kollar
            val arm = if (back) col(Muscle.TRICEPS) else col(Muscle.BICEPS)
            rect(-3.7f, 4.6f, 1.1f, 2.6f, arm); rect(2.6f, 4.6f, 1.1f, 2.6f, arm)
            rect(-3.8f, 7.4f, 1f, 2.4f, base); rect(2.8f, 7.4f, 1f, 2.4f, base)
            // kalça ve bacaklar
            rect(-2f, 10.5f, 4f, 1.6f, if (back) col(Muscle.GLUTES) else base)
            val thigh = if (back) col(Muscle.HAMSTRINGS) else col(Muscle.QUADS)
            rect(-2f, 12.3f, 1.8f, 3.6f, thigh); rect(0.2f, 12.3f, 1.8f, 3.6f, thigh)
            val calf = if (back) col(Muscle.CALVES) else base
            rect(-1.9f, 16.1f, 1.5f, 2.8f, calf); rect(0.4f, 16.1f, 1.5f, 2.8f, calf)
        }
        figure(0f, back = false)
        figure(figW, back = true)
    }
}

@Composable
fun PhotoThumb(file: File, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    var bmp by remember(file.path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file.path) {
        bmp = withContext(Dispatchers.IO) { Photos.loadThumb(file)?.asImageBitmap() }
    }
    Box(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b, null, contentScale = contentScale, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun KeyValue(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(min = 120.dp).weight(0.45f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.55f))
    }
}

val ContentPadding = PaddingValues(16.dp)
