package com.example.warasapp

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import android.app.PendingIntent
import android.content.Intent

class EscalationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val MAX_ATTEMPT = 3
    }

    override suspend fun doWork(): Result {
        val type = inputData.getString("type") ?: return Result.failure()

        // Kalau user udah jawab duluan, nggak usah ngejar lagi
        if (!NotifPrefsHelper.isPending(applicationContext, type)) {
            return Result.success()
        }

        val attempt = NotifPrefsHelper.getAttempt(applicationContext, type) + 1
        if (attempt > MAX_ATTEMPT) {
            return Result.success() // udah nyoba maksimal, berhenti ngejar
        }
        NotifPrefsHelper.setAttempt(applicationContext, type, attempt)

        showEscalatedNotification(type, attempt)
        scheduleNextEscalation(type, attempt)

        return Result.success()
    }

    private fun showEscalatedNotification(type: String, attempt: Int) {
        val channel = if (type == "mood") WarasApplication.CHANNEL_MOOD else WarasApplication.CHANNEL_FISIK
        val notifId = if (type == "mood") 1001 else 1002

        val title = when (attempt) {
            1 -> "Masih nunggu jawabanmu nih"
            2 -> "Yuk luangin waktu bentar aja"
            else -> "Terakhir nih, jangan lupa check-in ya"
        }

        val builder = NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (type == "mood") {
            builder.setContentText("Gimana harimu?")
            builder.addAction(buildMoodAction("Baik", 5, 10))
            builder.addAction(buildMoodAction("Biasa", 3, 11))
            builder.addAction(buildMoodAction("Buruk", 1, 12))
        } else {
            builder.setContentText("Ada keluhan fisik hari ini?")
            val intentYa = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntentYa = PendingIntent.getActivity(
                applicationContext, 20, intentYa,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val intentEnggak = Intent(applicationContext, SymptomActionReceiver::class.java)
            val pendingIntentEnggak = PendingIntent.getBroadcast(
                applicationContext, 21, intentEnggak,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Ya, Ada", pendingIntentYa)
            builder.addAction(0, "Enggak", pendingIntentEnggak)
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, builder.build())
    }

    private fun buildMoodAction(label: String, moodValue: Int, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(applicationContext, MoodActionReceiver::class.java).apply {
            putExtra("MOOD_VALUE", moodValue)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(0, label, pendingIntent)
    }

    private fun scheduleNextEscalation(type: String, attempt: Int) {
        if (attempt >= MAX_ATTEMPT) return

        // Interval makin rapat tiap attempt (contoh: 20 menit -> 10 menit)
        val delayMinutes = when (attempt) {
            1 -> 20L
            else -> 10L
        }

        val request = OneTimeWorkRequestBuilder<EscalationWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf("type" to type))
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork("escalation_$type", androidx.work.ExistingWorkPolicy.REPLACE, request)
    }
}