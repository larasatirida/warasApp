package com.example.warasapp

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.warasapp.logic.getFullMissionContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MissionSuggestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!NotifPrefsHelper.isNotifEnabled(applicationContext, "pengingat_mission")) {
            return Result.success()
        }

        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
            val db = FirebaseFirestore.getInstance()

            // Ambil mood & gejala hari ini (dari dokumen MoodLogRepository, ID = userId_tanggal)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val moodDoc = db.collection("mood_logs").document("${userId}_$today").get().await()
            val todayMood = (moodDoc.get("mood") as? Number)?.toInt() ?: 3
            val symptoms = (moodDoc.get("physicalSymptoms") as? List<*>)?.map { it.toString() } ?: emptyList()

            // Ambil total durasi aktivitas hari ini
            val activitiesSnapshot = db.collection("activities")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val totalMinutesToday = activitiesSnapshot.documents.sumOf {
                (it.getLong("durationMinutes") ?: 0L)
            }
            val durationHours = totalMinutesToday / 60f

            val content = getFullMissionContent(todayMood, durationHours, symptoms)
            showNotification(content)
            NotifPrefsHelper.setAnsweredToday(applicationContext, "mission_shown")

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(content: String) {
        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_MISSION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Ada misi baru buat kamu")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1003, notification)
    }
}