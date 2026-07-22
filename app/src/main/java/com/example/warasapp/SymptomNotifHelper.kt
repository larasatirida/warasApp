package com.example.warasapp

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.warasapp.logic.getSymptomTip
import java.util.concurrent.TimeUnit

object SymptomNotifHelper {
    fun scheduleTips(context: Context, symptoms: List<String>) {
        var index = 0
        symptoms.forEach { symptom ->
            val tip = getSymptomTip(symptom) ?: return@forEach
            val delaySeconds = 30L + (index * 60L)

            val data = workDataOf(
                "symptomLabel" to symptom,
                "tip" to tip,
                "notifIndex" to index
            )
            val request = OneTimeWorkRequestBuilder<SymptomTipWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            index++
        }
    }
}