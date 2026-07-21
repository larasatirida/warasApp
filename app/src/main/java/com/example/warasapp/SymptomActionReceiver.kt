package com.example.warasapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.example.warasapp.logic.MoodLogRepository

class SymptomActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotifPrefsHelper.setPending(context, "fisik", false)
        WorkManager.getInstance(context).cancelUniqueWork("escalation_fisik")
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_test"

        // User pilih "Enggak" -> gejala fisik kosong, tapi tetap ditulis ke field
        // yang sama (physicalSymptoms) dan dokumen hari yang sama supaya tidak
        // menimpa mood yang sudah dijawab lewat notifikasi pertama.
        MoodLogRepository.saveEntry(userId = userId, symptoms = emptyList())

        // Hilangkan notifikasi kendala fisik (misal ID-nya 1002)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotifPrefsHelper.setAnsweredToday(context, "fisik")
        manager.cancel(1002)
    }
}