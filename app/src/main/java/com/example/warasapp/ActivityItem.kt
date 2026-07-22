package com.example.warasapp

data class ActivityItem(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val source: String,
    val startTime: String,
    val endTime: String,
    val iconName: String = "ic_jadwal", // Menyimpan nama string ikon vektor
    val weight: String = "Ringan"      // Menyimpan tingkat beban (Ringan / Sedang / Berat)
)