package com.example.warasapp.logic

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyTrendPoint(val dayLabel: String, val score: Int)

suspend fun getTodayBurnoutScore(): Int {
    val (todayMood, symptomTypes) = getTodayMoodAndSymptoms()
    val todayHours = getTodayTotalHours()
    return calculateWeeklyBurnoutScore(todayMood.toFloat(), todayHours, if (symptomTypes.isNotEmpty()) 1 else 0)
}

suspend fun getWeeklyTrend(): List<DailyTrendPoint> {
    return try {
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
                val mood = (doc.get("mood") as? Number)?.toInt() ?: 2
                val hasSymptom = (doc.get("physicalSymptoms") as? List<*>)?.isNotEmpty() ?: false
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
            val mood = moodEntry?.second ?: 2
            val hasSymptom = moodEntry?.third ?: false
            val hours = hoursByDay[key] ?: 0f

            val score = calculateWeeklyBurnoutScore(mood.toFloat(), hours, if (hasSymptom) 1 else 0)
            result.add(DailyTrendPoint(label, score))
        }
        result
    } catch (e: Exception) {
        emptyList() // Jika butuh index, grafik kosong dulu saja, jangan bikin crash
    }
}
