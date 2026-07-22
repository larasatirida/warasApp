package com.example.warasapp

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.warasapp.logic.getTodayMoodAndSymptoms
import com.example.warasapp.logic.getTodayTotalHours

class GeneralReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1. Cek apakah notifikasi diaktifkan
        if (!NotifPrefsHelper.isNotifEnabled(applicationContext, "checkin_harian")) {
            return Result.success()
        }

        // 2. Ambil data kondisi user hari ini
        val (mood, symptoms) = getTodayMoodAndSymptoms()
        val totalHours = getTodayTotalHours()

        // 3. Cek apakah data masih kosong (mood 0 berarti belum check-in)
        val moodMissing = (mood == 0)
        val activitiesMissing = (totalHours == 0f)

        if (moodMissing || activitiesMissing) {
            val message = when {
                moodMissing && activitiesMissing -> 
                    "Kamu belum mengisi jadwal dan mood hari ini. Yuk, luangkan waktu sebentar!"
                moodMissing -> 
                    "Jadwal sudah aman, tapi mood kamu belum dicatat. Bagaimana perasaanmu?"
                else -> 
                    "Mood sudah dicatat, tapi jadwal aktivitasmu masih kosong. Yuk lengkapi!"
            }
            showNotification(message)
        }

        return Result.success()
    }

    private fun showNotification(message: String) {
        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_MOOD)
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentTitle("Pengingat Harian")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1007, notification)
    }
}
