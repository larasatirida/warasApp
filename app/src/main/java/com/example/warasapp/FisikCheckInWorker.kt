package com.example.warasapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class FisikCheckInWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun scheduleEscalation(type: String) {
        val request = androidx.work.OneTimeWorkRequestBuilder<EscalationWorker>()
            .setInitialDelay(20, TimeUnit.MINUTES)
            .setInputData(androidx.work.workDataOf("type" to type))
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork("escalation_$type", androidx.work.ExistingWorkPolicy.REPLACE, request)
    }

    private fun showNotification() {
        // Intent membuka aplikasi / Halaman Detail Gejala (SymptomDetailActivity harus dibuat dulu/minta Orang D/A)
        val intentYa = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_FRAGMENT", "SYMPTOM_PAGE")
        }
        val pendingIntentYa = PendingIntent.getActivity(
            applicationContext, 0, intentYa,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Intent untuk tombol "Enggak" lewat BroadcastReceiver
        val intentEnggak = Intent(applicationContext, SymptomActionReceiver::class.java)
        val pendingIntentEnggak = PendingIntent.getBroadcast(
            applicationContext, 4, intentEnggak,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_FISIK)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Ada keluhan fisik hari ini?")
            .setContentText("Catat keluhan fisikmu untuk memantau tingkat burnout harian.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Ya, Ada", pendingIntentYa)
            .addAction(android.R.drawable.checkbox_off_background, "Enggak", pendingIntentEnggak)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotifPrefsHelper.setPending(applicationContext, "fisik", true)
        NotifPrefsHelper.setAttempt(applicationContext, "fisik", 0)
        scheduleEscalation("fisik")
        manager.notify(1002, notification) // ID 1002 berbeda dengan mood
    }
}