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
    val hourScore = (avgEffectiveHours * 4).coerceAtMost(40f)

    val moodScore = when {
        avgMood == 0f -> 0f    // Jika belum ada data mood, jangan tambah poin
        avgMood <= 1.5f -> 30f // Mood buruk
        avgMood <= 2.5f -> 15f // Mood biasa
        else -> 0f            // Mood baik
    }

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