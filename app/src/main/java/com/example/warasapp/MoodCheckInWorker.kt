package com.example.warasapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MoodCheckInWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun buildMoodAction(label: String, moodValue: Int, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(applicationContext, MoodActionReceiver::class.java).apply {
            putExtra("MOOD_VALUE", moodValue)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(0, label, pendingIntent)
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_MOOD)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Gimana harimu?")
            .setContentText("Yuk check-in mood sebelum tidur")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(buildMoodAction("Baik", 5, 1))
            .addAction(buildMoodAction("Biasa", 3, 2))
            .addAction(buildMoodAction("Buruk", 1, 3))
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }
}