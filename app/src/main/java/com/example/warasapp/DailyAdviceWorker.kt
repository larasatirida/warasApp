package com.example.warasapp

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.warasapp.logic.getTodayMoodAndSymptoms
import com.example.warasapp.logic.getTodayTotalHours
import com.example.warasapp.logic.getDailyMission

class DailyAdviceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val (mood, symptoms) = getTodayMoodAndSymptoms()
        val totalHours = getTodayTotalHours()
        val userName = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName ?: ""

        val hasData = symptoms.isNotEmpty() || totalHours > 0

        val message = if (hasData) {
            getDailyMission(userName, mood, totalHours, symptoms)
        } else {
            "Selamat pagi! Yuk, mulai hari dengan check-in kondisimu agar kami bisa memberikan saran terbaik hari ini."
        }

        showNotification(message)
        return Result.success()
    }

    private fun showNotification(message: String) {
        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_MISSION)
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentTitle("Saran untukmu hari ini")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1006, notification)
    }
}
