package com.example.warasapp

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SymptomTipWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val symptomLabel = inputData.getString("symptomLabel") ?: return Result.failure()
        val tip = inputData.getString("tip") ?: return Result.failure()
        val notifIndex = inputData.getInt("notifIndex", 0)

        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_MISSION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(symptomLabel)
            .setContentText(tip)
            .setStyle(NotificationCompat.BigTextStyle().bigText(tip))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(3000 + notifIndex, notification)

        return Result.success()
    }
}