package com.example.warasapp.logic

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun sevenDaysAgoTimestamp(): Timestamp {
    return Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
}

private fun startOfTodayTimestamp(): Timestamp {
    val now = System.currentTimeMillis()
    return Timestamp(Date(now - now % (24L * 60 * 60 * 1000)))
}

suspend fun getTodayMoodAndSymptoms(): Pair<Int, List<String>> {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return Pair(2, emptyList())

    val snapshot = firestore.collection("mood_logs")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("timestamp", startOfTodayTimestamp())
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .await()

    if (snapshot.documents.isEmpty()) return Pair(2, emptyList())

    val latestMood = snapshot.documents.first().get("mood")?.let {
        (it as? Number)?.toInt()
    } ?: 2

    @Suppress("UNCHECKED_CAST")
    val allSymptomsToday = snapshot.documents.flatMap {
        (it.get("symptomTypes") as? List<String>) ?: emptyList()
    }.distinct()

    return Pair(latestMood, allSymptomsToday)
}

suspend fun getTodayTotalHours(): Float {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0f

    val snapshot = firestore.collection("activities")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("date", startOfTodayTimestamp())
        .get()
        .await()

    val totalMinutes = snapshot.documents.sumOf {
        (it.get("durationMinutes") as? Number)?.toInt() ?: 0
    }
    return totalMinutes / 60f
}

suspend fun getWeeklyMoodAverage(): Float {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0f

    val snapshot = firestore.collection("mood_logs")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgoTimestamp())
        .get()
        .await()

    val moods = snapshot.documents.mapNotNull { (it.get("mood") as? Number)?.toInt() }
    return if (moods.isNotEmpty()) moods.average().toFloat() else 0f
}

suspend fun getWeeklySymptomCount(): Int {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0

    val snapshot = firestore.collection("mood_logs")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgoTimestamp())
        .get()
        .await()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val daysWithSymptom = snapshot.documents
        .filter { doc ->
            val symptoms = doc.get("symptomTypes") as? List<*>
            symptoms?.isNotEmpty() == true
        }
        .mapNotNull { doc ->
            val ts = doc.getTimestamp("timestamp") ?: return@mapNotNull null
            dateFormat.format(ts.toDate())
        }
        .distinct()

    return daysWithSymptom.size
}

suspend fun getWeeklyEffectiveHoursAverage(): Float {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0f

    val snapshot = firestore.collection("activities")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("date", sevenDaysAgoTimestamp())
        .get()
        .await()

    if (snapshot.documents.isEmpty()) return 0f

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val effectiveHoursPerDay = snapshot.documents
        .mapNotNull { doc ->
            val durationMinutes = (doc.get("durationMinutes") as? Number)?.toFloat() ?: return@mapNotNull null
            val beban = doc.getString("tingkatBeban") ?: "Sedang"
            val ts = doc.getTimestamp("date") ?: return@mapNotNull null
            val dateKey = dateFormat.format(ts.toDate())
            val effectiveHours = (durationMinutes / 60f) * bebanToWeight(beban)
            Pair(dateKey, effectiveHours)
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, hours) -> hours.sum() }

    return if (effectiveHoursPerDay.isNotEmpty()) {
        effectiveHoursPerDay.values.average().toFloat()
    } else 0f
}