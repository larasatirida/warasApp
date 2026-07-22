package com.example.warasapp

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.warasapp.logic.calculateWeeklyBurnoutScore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class BurnoutAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val THRESHOLD = 67
    }

    override suspend fun doWork(): Result {
        if (!NotifPrefsHelper.isNotifEnabled(applicationContext, "burnout_alert")) {
            return Result.success()
        }

        return try {
            checkBurnoutScore()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkBurnoutScore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val sevenDaysAgo = Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)

        // Rata-rata jam aktivitas 7 hari terakhir
        val activitiesSnapshot = db.collection("activities")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(sevenDaysAgo))
            .get()
            .await()

        val totalMinutes = activitiesSnapshot.documents.sumOf {
            (it.getLong("durationMinutes") ?: 0L)
        }
        val avgEffectiveHours = (totalMinutes / 7.0 / 60.0).toFloat()

        // Rata-rata mood & jumlah hari ada gejala, 7 hari terakhir
        val moodSnapshot = db.collection("mood_logs")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", com.google.firebase.Timestamp(sevenDaysAgo))
            .get()
            .await()

        val moods = moodSnapshot.documents.mapNotNull { (it.get("mood") as? Number)?.toFloat() }
        val avgMood = if (moods.isNotEmpty()) moods.average().toFloat() else 2f

        val symptomDaysCount = moodSnapshot.documents.count { doc ->
            (doc.get("symptomTypes") as? List<*>)?.isNotEmpty() == true
        }

        val score = calculateWeeklyBurnoutScore(avgMood, avgEffectiveHours, symptomDaysCount)

        if (score > THRESHOLD) {
            showAlert(score)
        }
    }

    private fun showAlert(score: Int) {
        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_MISSION)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Skor burnout kamu tinggi")
            .setContentText("Skor minggu ini: $score. Coba kurangi beban dan istirahat lebih banyak ya.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1004, notification)
    }
}