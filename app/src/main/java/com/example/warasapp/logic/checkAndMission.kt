package com.example.warasapp.logic

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CheckinHistoryItem(
    val dateLabel: String,
    val mood: Int,
    val symptomTypes: List<String>,
    val catatan: String
)

suspend fun saveCheckin(mood: Int, symptomTypes: List<String>, catatan: String, sleepHours: Float?) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val data = hashMapOf<String, Any>(
        "userId" to currentUserId,
        "mood" to mood,
        "symptomTypes" to symptomTypes,
        "catatan" to catatan,
        "timestamp" to Timestamp.now()
    )
    if (sleepHours != null) data["sleepHours"] = sleepHours

    firestore.collection("mood_logs").add(data).await()
}

suspend fun getCheckinHistory(limit: Int = 10): List<CheckinHistoryItem> {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()

    val dateLabelFormat = SimpleDateFormat("EEE, d MMM", Locale("id"))

    val snapshot = firestore.collection("mood_logs")
        .whereEqualTo("userId", currentUserId)
        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        .limit(limit.toLong())
        .get()
        .await()

    return snapshot.documents.mapNotNull { doc ->
        val ts = doc.getTimestamp("timestamp") ?: return@mapNotNull null
        @Suppress("UNCHECKED_CAST")
        CheckinHistoryItem(
            dateLabel = dateLabelFormat.format(ts.toDate()),
            mood = (doc.get("mood") as? Number)?.toInt() ?: 2,
            symptomTypes = (doc.get("symptomTypes") as? List<String>) ?: emptyList(),
            catatan = doc.getString("catatan") ?: ""
        )
    }
}