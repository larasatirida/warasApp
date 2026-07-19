package com.example.warasapp.logic

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActivityItem(
    val id: String,
    val name: String,
    val startTime: String,
    val endTime: String,
    val kategori: String,
    val tingkatBeban: String,
    val durationMinutes: Int,
    val source: String
)

suspend fun saveManualActivity(
    name: String,
    startTime: String,
    endTime: String,
    kategori: String,
    tingkatBeban: String,
    durationMinutes: Int,
    date: Date
) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    firestore.collection("activities").add(
        hashMapOf(
            "userId" to currentUserId,
            "name" to name,
            "startTime" to startTime,
            "endTime" to endTime,
            "kategori" to kategori,
            "tingkatBeban" to tingkatBeban,
            "durationMinutes" to durationMinutes,
            "date" to Timestamp(date),
            "source" to "manual"
        )
    ).await()
}

suspend fun getActivitiesForDate(date: Date): List<ActivityItem> {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val targetDateKey = dateFormat.format(date)

    val startOfDay = Timestamp(Date(date.time - date.time % (24L * 60 * 60 * 1000)))
    val endOfDay = Timestamp(Date(startOfDay.toDate().time + 24L * 60 * 60 * 1000))

    val snapshot = firestore.collection("activities")
        .whereEqualTo("userId", currentUserId)
        .whereGreaterThanOrEqualTo("date", startOfDay)
        .whereLessThan("date", endOfDay)
        .get()
        .await()

    return snapshot.documents.mapNotNull { doc ->
        ActivityItem(
            id = doc.id,
            name = doc.getString("name") ?: return@mapNotNull null,
            startTime = doc.getString("startTime") ?: "",
            endTime = doc.getString("endTime") ?: "",
            kategori = doc.getString("kategori") ?: "",
            tingkatBeban = doc.getString("tingkatBeban") ?: "Sedang",
            durationMinutes = (doc.get("durationMinutes") as? Number)?.toInt() ?: 0,
            source = doc.getString("source") ?: "manual"
        )
    }
}

suspend fun getTotalHoursForDate(date: Date): Pair<Float, Boolean> {
    val activities = getActivitiesForDate(date)
    val totalMinutes = activities.sumOf { it.durationMinutes }
    val totalHours = totalMinutes / 60f
    return Pair(totalHours, isOverSafeLimit(totalHours))
}

suspend fun getWeeklyActivitySummary(weekStartDate: Date): List<Pair<String, Float>> {
    val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val result = mutableListOf<Pair<String, Float>>()

    for (i in 0..6) {
        val date = Date(weekStartDate.time + i * 24L * 60 * 60 * 1000)
        val (totalHours, _) = getTotalHoursForDate(date)
        result.add(Pair(dayLabels[i], totalHours))
    }
    return result
}

suspend fun importFromGoogleCalendar(): List<ActivityItem> {
    // TODO: implementasi Google Calendar API, target H-2/H-1 kalau waktu cukup
    return emptyList()
}