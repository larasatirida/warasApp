package com.example.warasapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.warasapp.logic.MissionType

class MissionNotifWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val typeKey = inputData.getString("missionTypeKey") ?: return Result.failure()
        val missionType = MissionType.entries.find { it.key == typeKey } ?: return Result.failure()

        if (!NotifPrefsHelper.isNotifEnabled(applicationContext, "pengingat_mission")) {
            return Result.success()
        }

        showNotification(missionType)
        return Result.success()
    }

    private fun showNotification(missionType: MissionType) {
        val notifId = 2000 + missionType.ordinal

        val intentDone = Intent(applicationContext, MissionActionReceiver::class.java).apply {
            putExtra("missionTypeKey", missionType.key)
        }
        val pendingDone = PendingIntent.getBroadcast(
            applicationContext, notifId, intentDone,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = when (missionType) {
            MissionType.SLEEP -> "Semalam tidurnya 7 jam lebih nggak?"
            MissionType.NO_OVERTIME -> "Hari ini pulang tepat waktu, nggak lembur?"
            MissionType.EXERCISE -> "Udah sempet olahraga hari ini?"
        }

        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_MISSION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(missionType.title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "Sudah ✓", pendingDone)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }
}