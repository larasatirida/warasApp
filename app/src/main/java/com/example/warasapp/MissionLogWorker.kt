package com.example.warasapp

import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.warasapp.logic.MissionType
import com.example.warasapp.logic.logMissionCheckin

class MissionLogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val typeKey = inputData.getString("missionTypeKey") ?: return Result.failure()
        val missionType = MissionType.entries.find { it.key == typeKey } ?: return Result.failure()

        logMissionCheckin(missionType)

        val notifId = 2000 + missionType.ordinal
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notifId)

        return Result.success()
    }
}