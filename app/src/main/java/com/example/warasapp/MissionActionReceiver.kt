package com.example.warasapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class MissionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val typeKey = intent.getStringExtra("missionTypeKey") ?: return

        val request = OneTimeWorkRequestBuilder<MissionLogWorker>()
            .setInputData(workDataOf("missionTypeKey" to typeKey))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}