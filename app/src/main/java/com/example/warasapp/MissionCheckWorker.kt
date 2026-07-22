package com.example.warasapp

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class MissionCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            checkForActiveMission()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkForActiveMission() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val snapshot = FirebaseFirestore.getInstance()
            .collection("mission_logs")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "aktif")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: return
        val missionId = doc.id
        val title = doc.getString("title") ?: "Ada misi baru buat kamu"
        val desc = doc.getString("desc") ?: "Coba luangin waktu sebentar yuk"

        val lastNotified = NotifPrefsHelper.getLastNotifiedMissionId(applicationContext)
        if (missionId == lastNotified) return // udah pernah dinotif, jangan ulang

        showMissionNotification(title, desc)
        NotifPrefsHelper.setLastNotifiedMissionId(applicationContext, missionId)
        NotifPrefsHelper.setAnsweredToday(applicationContext, "mission_shown")
    }

    private fun showMissionNotification(title: String, desc: String) {
        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_MISSION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1003, notification)
    }
}