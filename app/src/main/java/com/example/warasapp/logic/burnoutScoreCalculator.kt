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
    var score = 0
    if (avgEffectiveHours > 9) score += 40
    if (avgMood > 2f) score += 30
    if (symptomDaysCount >= 1) score += 30
    return score.coerceIn(0, 100)
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