package com.example.warasapp.logic

import com.example.warasapp.network.RetrofitClient

fun getDailyMission(todayMood: Int, todayDurationHours: Float, symptomTypes: List<String>): String {
    val isOverworked = todayDurationHours > 8
    return when {
        symptomTypes.contains("kelelahan_ekstrem") ->
            "Istirahat total dulu ya, jangan dipaksa lanjut aktivitas hari ini"

        symptomTypes.contains("sulit_tidur") ->
            "Coba matikan layar HP 30 menit sebelum tidur malam ini"

        symptomTypes.contains("sakit_kepala") ->
            "Redupkan layar & istirahatkan mata 5 menit di ruangan gelap"

        symptomTypes.contains("mata_lelah") ->
            "Coba teknik 20-20-20: tiap 20 menit, lihat objek sejauh 20 kaki selama 20 detik"

        symptomTypes.contains("nyeri_punggung") ->
            "Coba stretching punggung ringan selama 5 menit"

        symptomTypes.contains("sulit_fokus") ->
            "Coba teknik Pomodoro: kerja 25 menit, istirahat 5 menit"

        symptomTypes.contains("mudah_marah") ->
            "Coba tarik napas dalam 5 kali sebelum lanjut aktivitas"

        symptomTypes.contains("kurang_nafsu_makan") ->
            "Coba makan camilan kecil dulu, meski sedikit"

        todayMood == 1 && isOverworked ->
            "Coba jalan-jalan keluar 10 menit, aktivitas fisik ringan bantu perbaiki mood"

        todayMood == 1 ->
            "Coba tulis 3 kalimat tentang perasaanmu hari ini, menulis bantu meredakan emosi"

        todayMood == 2 && isOverworked ->
            "Ambil jeda 5 menit, tarik napas dalam beberapa kali sebelum lanjut aktivitas"

        todayMood == 2 ->
            "Coba minum air putih dan regangkan badan sebentar"

        todayMood == 3 && isOverworked ->
            "Mood kamu bagus, tapi aktivitas hari ini padat—tetap sisihkan waktu istirahat ya"

        else ->
            "Kamu baik-baik aja hari ini! Terus jaga ritme ini ya"
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
    val mainMission = getDailyMission(todayMood, todayDurationHours, symptomTypes)

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