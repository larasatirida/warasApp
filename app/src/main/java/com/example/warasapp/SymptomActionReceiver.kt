package com.example.warasapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class SymptomActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_test"

        // Simpan data kosong / no symptom sesuai skema tim (kendala = array kosong)
        val symptomLog = hashMapOf(
            "userId" to userId,
            "kendala" to arrayListOf<String>(), // Kosong karena memilih "Enggak"
            "timestamp" to Date()
        )

        FirebaseFirestore.getInstance()
            .collection("mood_logs") // Sesuaikan nama collection dengan kesepakatan tim
            .add(symptomLog)

        // Hilangkan notifikasi kendala fisik (misal ID-nya 1002)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(1002)
    }
}