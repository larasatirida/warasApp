package com.example.warasapp.logic

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyTrendPoint(val dayLabel: String, val score: Int)

data class DashboardInsights(
    val workHoursMessage: String,
    val sleepMissionMessage: String,
    val exerciseMissionMessage: String
)

suspend fun getTodayBurnoutScore(): Int {
    val (todayMood, symptomTypes) = getTodayMoodAndSymptoms()
    val todayHours = getTodayTotalHours()
    return calculateWeeklyBurnoutScore(todayMood.toFloat(), todayHours, if (symptomTypes.isNotEmpty()) 1 else 0)
}

suspend fun getWeeklyTrend(): List<DailyTrendPoint> {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()

    val sevenDaysAgo = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayLabelFormat = SimpleDateFormat("EEE", Locale("id"))

    val moodSnapshot = firestore.collection("mood_logs")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgo)
        .get().await()

    val activitySnapshot = firestore.collection("activities")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("date", sevenDaysAgo)
        .get().await()

    val moodByDay = moodSnapshot.documents
        .mapNotNull { doc ->
            val ts = doc.getTimestamp("timestamp") ?: return@mapNotNull null
            val mood = (doc.get("mood") as? Number)?.toInt() ?: return@mapNotNull null
            val hasSymptom = !((doc.get("symptomTypes") as? List<*>).isNullOrEmpty())
            Triple(dateFormat.format(ts.toDate()), mood, hasSymptom)
        }
        .groupBy { it.first }
        .mapValues { (_, entries) -> entries.last() }

    val hoursByDay = activitySnapshot.documents
        .mapNotNull { doc ->
            val ts = doc.getTimestamp("date") ?: return@mapNotNull null
            val minutes = (doc.get("durationMinutes") as? Number)?.toFloat() ?: 0f
            val beban = doc.getString("tingkatBeban") ?: "Sedang"
            Pair(dateFormat.format(ts.toDate()), (minutes / 60f) * bebanToWeight(beban))
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, hours) -> hours.sum() }

    val result = mutableListOf<DailyTrendPoint>()
    for (i in 6 downTo 0) {
        val date = Date(System.currentTimeMillis() - i * 24L * 60 * 60 * 1000)
        val key = dateFormat.format(date)
        val label = dayLabelFormat.format(date)

        val moodEntry = moodByDay[key]
        val mood = moodEntry?.second ?: 2       // default "Biasa" kalau hari itu tidak ada data
        val hasSymptom = moodEntry?.third ?: false
        val hours = hoursByDay[key] ?: 0f

        val score = calculateWeeklyBurnoutScore(mood.toFloat(), hours, if (hasSymptom) 1 else 0)
        result.add(DailyTrendPoint(label, score))
    }
    return result
}

private suspend fun getWorkHoursMessage(): String {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return "Belum ada data aktivitas."

    val sevenDaysAgo = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
    val fourteenDaysAgo = Timestamp(Date(System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000))

    val thisWeek = firestore.collection("activities")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("date", sevenDaysAgo)
        .get().await()

    val lastWeek = firestore.collection("activities")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("date", fourteenDaysAgo)
        .whereLessThan("date", sevenDaysAgo)
        .get().await()

    val thisWeekMinutes = thisWeek.documents.sumOf { (it.get("durationMinutes") as? Number)?.toInt() ?: 0 }
    val lastWeekMinutes = lastWeek.documents.sumOf { (it.get("durationMinutes") as? Number)?.toInt() ?: 0 }

    if (lastWeekMinutes == 0) return "Belum cukup data minggu lalu untuk dibandingkan."

    val changePercent = (((thisWeekMinutes - lastWeekMinutes).toFloat() / lastWeekMinutes) * 100).toInt()

    return when {
        changePercent > 5 -> "Jam kerja kamu naik $changePercent% minggu ini."
        changePercent < -5 -> "Jam kerja kamu turun ${-changePercent}% minggu ini, kerja bagus!"
        else -> "Jam kerja kamu stabil minggu ini."
    }
}

private suspend fun getSleepMissionMessage(): String {
    val progress = getSleepStreakMission()
    return when {
        progress.currentProgress == 0 ->
            "Kamu belum tidur 7+ jam minggu ini."
        progress.currentProgress < progress.targetProgress ->
            "Kamu sudah tidur 7+ jam ${progress.currentProgress}x minggu ini."
        else ->
            "Selamat! Kamu sudah menyelesaikan misi tidur 7+ jam minggu ini!"
    }
}

private suspend fun getExerciseMissionMessage(): String {
    val progress = getExerciseMission()
    return when {
        progress.currentProgress == 0 ->
            "Kamu belum olahraga minggu ini."
        progress.currentProgress < progress.targetProgress ->
            "Kamu sudah olahraga ${progress.currentProgress}x minggu ini."
        else ->
            "Selamat! Kamu sudah menyelesaikan misi olahraga minggu ini!"
    }
}

suspend fun getDashboardInsights(): DashboardInsights {
    return DashboardInsights(
        workHoursMessage = getWorkHoursMessage(),
        sleepMissionMessage = getSleepMissionMessage(),
        exerciseMissionMessage = getExerciseMissionMessage()
    )
}