package com.example.warasapp.logic

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MoodLogRepository {

    private fun todayDocId(userId: String): String {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return "${userId}_$today"
    }

    fun saveEntry(
        userId: String,
        mood: Int? = null,
        symptoms: List<String>? = null,
        notes: String? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val data = hashMapOf<String, Any>(
            "userId" to userId,
            "timestamp" to FieldValue.serverTimestamp()
        )
        if (mood != null) {
            data["mood"] = mood
        }
        if (symptoms != null) {
            data["physicalSymptoms"] = symptoms
            data["hasPhysicalSymptom"] = symptoms.isNotEmpty()
        }
        if (notes != null) {
            data["notes"] = notes
        }

        FirebaseFirestore.getInstance()
            .collection("mood_logs")
            .document(todayDocId(userId))
            .set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Updates an EXISTING mood_logs entry directly by its document id — used when
     * the user edits a past (or today's) entry from the Riwayat list.
     * Unlike saveEntry(), this does NOT touch "timestamp", so the entry stays
     * associated with its original day/order in the history list.
     */
    fun updateEntry(
        docId: String,
        mood: Int,
        symptoms: List<String>,
        notes: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val data = hashMapOf<String, Any>(
            "mood" to mood,
            "physicalSymptoms" to symptoms,
            "hasPhysicalSymptom" to symptoms.isNotEmpty(),
            "notes" to notes
        )

        FirebaseFirestore.getInstance()
            .collection("mood_logs")
            .document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}