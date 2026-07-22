package com.example.warasapp.logic

private val symptomTips: Map<String, String> = mapOf(
    "Kelelahan ekstrem" to "Istirahat total dulu ya, jangan dipaksa lanjut aktivitas hari ini",
    "Sulit tidur" to "Coba matikan layar HP 30 menit sebelum tidur malam ini",
    "Sakit kepala" to "Redupkan layar & istirahatkan mata 5 menit di ruangan gelap",
    "Mata lelah" to "Coba teknik 20-20-20: tiap 20 menit, lihat objek sejauh 20 kaki selama 20 detik",
    "Nyeri punggung" to "Coba stretching punggung ringan selama 5 menit",
    "Sulit fokus" to "Coba teknik Pomodoro: kerja 25 menit, istirahat 5 menit",
    "Mudah marah" to "Coba tarik napas dalam 5 kali sebelum lanjut aktivitas",
    "Kurang nafsu makan" to "Coba makan camilan kecil dulu, meski sedikit"
)

fun getSymptomTip(symptom: String): String? = symptomTips[symptom]