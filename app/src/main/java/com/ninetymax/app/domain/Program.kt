package com.ninetymax.app.domain

import com.ninetymax.app.data.AppData
import com.ninetymax.app.data.toDateOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val PROGRAM_DAYS = 90
val CHECKPOINT_DAYS = listOf(1, 30, 60, 90)

data class Phase(val index: Int, val name: String, val description: String, val rir: String)

object Program {
    fun startDate(data: AppData, fallback: LocalDate): LocalDate =
        data.profile.startDate.toDateOrNull() ?: fallback

    /** 1'den başlayan program günü. Başlangıçtan önceki tarihler için 0 veya negatif döner. */
    fun dayOf(data: AppData, date: LocalDate): Int =
        ChronoUnit.DAYS.between(startDate(data, date), date).toInt() + 1

    fun dateOfDay(data: AppData, day: Int, fallback: LocalDate): LocalDate =
        startDate(data, fallback).plusDays((day - 1).toLong())

    fun phaseOf(day: Int): Phase = when {
        day <= 28 -> Phase(1, "Faz 1 · Temel", "Formu öğren, alışkanlıkları otur. Hareketleri 3 tekrar yedekte bitir.", "RIR 3")
        day <= 56 -> Phase(2, "Faz 2 · Hacim", "Öncelikli kaslarda set sayısı artar. Her hafta tekrar veya ağırlık ekle.", "RIR 2")
        day <= 63 -> Phase(3, "Toparlanma haftası", "Setler azalır, eklemler ve sinir sistemi dinlenir. Teknik çalış.", "RIR 4")
        else -> Phase(4, "Faz 3 · Yoğunluk", "Ağırlık artar, tekrarlar biraz düşer. Kalıcı kazanımları pekiştir.", "RIR 1-2")
    }

    fun weekOf(day: Int): Int = ((day - 1).coerceAtLeast(0) / 7) + 1
}
