package com.example.warasapp.logic

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun getStartAndEndOfToday(): Pair<Timestamp, Timestamp> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val start = Timestamp(cal.time)

    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    val end = Timestamp(cal.time)
    
    return Pair(start, end)
}

suspend fun getTodayMoodAndSymptoms(): Pair<Int, List<String>> {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return Pair(0, emptyList())
    val (start, end) = getStartAndEndOfToday()

    return try {
        val snapshot = firestore.collection("mood_logs")
            .whereEqualTo("userId", currentUserId)
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .get().await()

        if (snapshot.isEmpty) return Pair(0, emptyList())

        val latestDoc = snapshot.documents.maxByOrNull { it.getTimestamp("timestamp")?.seconds ?: 0 }
        val mood = latestDoc?.getLong("mood")?.toInt() ?: 0
        val symptoms = (latestDoc?.get("physicalSymptoms") as? List<*>)?.map { it.toString() } ?: emptyList()
        
        Pair(mood, symptoms)
    } catch (e: Exception) {
        Pair(0, emptyList())
    }
}

suspend fun getTodayTotalHours(): Float {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0f
    val (start, end) = getStartAndEndOfToday()

    return try {
        val snapshot = firestore.collection("activities")
            .whereEqualTo("userId", currentUserId)
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .get().await()

        val totalEffectiveMinutes = snapshot.documents.sumOf {
            val mins = (it.get("durationMinutes") as? Number)?.toFloat() ?: 0f
            val beban = it.getString("tingkatBeban") ?: "Sedang"
            (mins * bebanToWeight(beban)).toDouble()
        }
        (totalEffectiveMinutes / 60f).toFloat()
    } catch (e: Exception) {
        0f
    }
}

// ... (sisanya tetap sama)
suspend fun getWeeklyMoodAverage(): Float {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0f
    val since = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
    val snapshot = firestore.collection("mood_logs").whereEqualTo("userId", currentUserId).whereGreaterThanOrEqualTo("timestamp", since).get().await()
    val moods = snapshot.documents.mapNotNull { (it.get("mood") as? Number)?.toInt() }
    return if (moods.isNotEmpty()) moods.average().toFloat() else 0f
}

suspend fun getWeeklySymptomCount(): Int {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0
    val since = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
    val snapshot = firestore.collection("mood_logs").whereEqualTo("userId", currentUserId).whereGreaterThanOrEqualTo("timestamp", since).get().await()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return snapshot.documents.filter { (it.get("physicalSymptoms") as? List<*>)?.isNotEmpty() == true }
        .mapNotNull { it.getTimestamp("timestamp")?.toDate() }.map { dateFormat.format(it) }.distinct().size
}

suspend fun getWeeklyEffectiveHoursAverage(): Float {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0f
    val since = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
    val snapshot = firestore.collection("activities").whereEqualTo("userId", currentUserId).whereGreaterThanOrEqualTo("date", since).get().await()
    if (snapshot.isEmpty) return 0f
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val hoursPerDay = snapshot.documents.mapNotNull { 
        val mins = (it.get("durationMinutes") as? Number)?.toFloat() ?: 0f
        val ts = it.getTimestamp("date") ?: return@mapNotNull null
        Pair(dateFormat.format(ts.toDate()), mins / 60f)
    }.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
    return if (hoursPerDay.isNotEmpty()) hoursPerDay.values.average().toFloat() else 0f
}
