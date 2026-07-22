package com.example.warasapp.logic

import com.example.warasapp.network.RetrofitClient
import kotlinx.coroutines.tasks.await

fun getDailyMission(userName: String, todayMood: Int, todayDurationHours: Float, symptomTypes: List<String>): String {
    val isOverworked = todayDurationHours > 8
    val name = if (userName.isBlank()) "Babe" else userName
    
    // Ubah semua gejala ke huruf kecil agar pencarian lebih akurat
    val symptoms = symptomTypes.map { it.lowercase() }
    
    return when {
        symptoms.any { it.contains("lelah") && it.contains("ekstrem") } ->
            "$name, istirahat total dulu yaah, jangan dipaksa lanjut aktivitas hari ini okay"

        symptoms.any { it.contains("tidur") } ->
            "Biar gampang tidur, $name coba deh matiin layar HP 30 menit sebelum tidur malam ini"

        symptoms.any { it.contains("kepala") } ->
            "Kalo sakit kepala, $name redupin layar & istirahatin mata 5 menit di ruangan gelap dulu deh"

        symptoms.any { it.contains("mata") } ->
            "$name, kalau matamu lelah, coba teknik 20-20-20: tiap 20 menit, lihat objek sejauh 20 kaki selama 20 detik"

        symptoms.any { it.contains("punggung") || it.contains("jompo") } ->
            "Biar ga jompo, $name coba deh stretching punggung ringan selama 5 menit"

        symptoms.any { it.contains("fokus") } ->
            "Nih tips biar fokus, $name coba teknik Pomodoro: kerja 25 menit, istirahat 5 menit"

        symptoms.any { it.contains("marah") } ->
            "Jangan marah-marah plis $name, coba tarik napas dalam 5 kali sebelum lanjut aktivitas"

        symptoms.any { it.contains("makan") || it.contains("nafsu") } ->
            "$name ga harus dipaksa makan kalau ga nafsu, coba makan camilan kecil dulu, meski sedikit"

        todayMood == 1 && isOverworked ->
            "$name Capek ya? coba jalan-jalan keluar 10 menit, aktivitas fisik ringan bantu perbaiki mood kamu"

        todayMood == 1 ->
            "Mood $name lagi ga banget ya? coba tulis 3 kalimat tentang perasaanmu hari ini, menulis bantu meredakan emosi"

        todayMood == 2 && isOverworked ->
            "Ambil jeda 5 menit $name, tarik napas dalam beberapa kali sebelum lanjut aktivitas"

        todayMood == 2 ->
            "Walau mood $name b aja, coba minum air putih dan regangkan badan sebentar"

        todayMood == 3 && isOverworked ->
            "Mood $name bagus, tapi aktivitas hari ini padat—tetap sisihkan waktu istirahat yah"

        else ->
            "OMG $name, Kamu baik-baik aja hari ini! Terus jaga ritme ini ya"
    }
}
val curatedMentalHealthQuotes = listOf(
    "Istirahat bukan tanda kemalasan, itu kebutuhan.",
    "Kamu boleh berhenti sejenak, dunia nggak akan runtuh.",
    "Merawat diri sendiri itu bukan egois.",
    "Kelelahan itu sinyal, bukan kegagalan.",
    "Nggak apa-apa kalau hari ini belum produktif banget."
)

suspend fun getFullMissionContent(todayMood: Int, todayDurationHours: Float, symptomTypes: List<String>): String {
    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var userName = ""
    if (userId != null) {
        val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
        userName = doc.getString("fullName") ?: doc.getString("name") ?: ""
    }
    
    val mainMission = getDailyMission(userName, todayMood, todayDurationHours, symptomTypes)

    val quotes = try {
        if ((0..1).random() == 0) {
            curatedMentalHealthQuotes.random()
        } else {
            RetrofitClient.instance.getRandomQuote().quote
        }
    } catch (e: Exception) {
        curatedMentalHealthQuotes.random()
    }

    return "$mainMission\n\nquote: $quotes"
}