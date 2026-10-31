package com.ninetymax.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

val TR: Locale = Locale.forLanguageTag("tr-TR")

fun newId(): String = UUID.randomUUID().toString().take(12)

fun LocalDate.iso(): String = toString()
fun String.toDate(): LocalDate = LocalDate.parse(this)
fun String.toDateOrNull(): LocalDate? = try { LocalDate.parse(this) } catch (e: Exception) { null }
fun String.toTimeOrNull(): LocalTime? = try { LocalTime.parse(this) } catch (e: Exception) { null }

private val longFmt = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TR)
private val shortFmt = DateTimeFormatter.ofPattern("d MMM", TR)
fun LocalDate.longTr(): String = format(longFmt)
fun LocalDate.shortTr(): String = format(shortFmt)
fun DayOfWeek.shortTr(): String = getDisplayName(TextStyle.SHORT, TR)
fun isoDayShort(d: Int): String = DayOfWeek.of(d).shortTr()

fun Double.fmt(decimals: Int = 1): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else String.format(TR, "%.${decimals}f", this)

fun String.toDoubleTr(): Double? = trim().replace(',', '.').toDoubleOrNull()

fun formatTl(v: Double): String = String.format(TR, "%,.0f ₺", v)
