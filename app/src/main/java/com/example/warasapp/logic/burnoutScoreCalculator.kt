package com.example.warasapp.logic

fun bebanToWeight(beban: String): Float {
    return when (beban.lowercase()) {
        "berat" -> 1.5f
        "sedang" -> 1.0f
        "ringan" -> 0.5f
        else -> 1.0f
    }
}

fun calculateWeeklyBurnoutScore(avgMood: Float, avgEffectiveHours: Float, symptomDaysCount: Int): Int {
    // 1. Kontribusi Jam Kerja (Maks 40 poin)
    // Asumsi: 8 jam kerja = ~25 poin, >12 jam = 40 poin
    val hourScore = (avgEffectiveHours * 4).coerceAtMost(40f)

    // 2. Kontribusi Mood (Maks 30 poin)
    // Mood: 1 (Buruk), 2 (Biasa), 3 (Baik)
    // Jika mood buruk (1), burnout naik. Jika mood baik (3), burnout turun.
    val moodScore = when {
        avgMood <= 1.5f -> 30f // Mood buruk banget
        avgMood <= 2.5f -> 15f // Mood biasa
        else -> 0f            // Mood baik
    }

    // 3. Kontribusi Gejala Fisik (Maks 30 poin)
    val symptomScore = if (symptomDaysCount >= 1) 30f else 0f

    return (hourScore + moodScore + symptomScore).toInt().coerceIn(0, 100)
}

fun burnoutScoreCategory(score: Int): String {
    return when {
        score < 34 -> "Rendah"
        score < 67 -> "Sedang"
        else -> "Tinggi"
    }
}

fun isOverSafeLimit(totalHoursToday: Float): Boolean {
    return totalHoursToday > 9f
}