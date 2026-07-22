package com.example.warasapp

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.warasapp.logic.curatedMentalHealthQuotes
import com.example.warasapp.network.RetrofitClient

class QuoteNotifWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val quote = try {
            if ((0..1).random() == 0) {
                curatedMentalHealthQuotes.random()
            } else {
                RetrofitClient.instance.getRandomQuote().quote
            }
        } catch (e: Exception) {
            curatedMentalHealthQuotes.random()
        }

        showNotification(quote)
        return Result.success()
    }

    private fun showNotification(quote: String) {
        val notification = NotificationCompat.Builder(applicationContext, WarasApplication.CHANNEL_MISSION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Quote hari ini")
            .setContentText(quote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1005, notification)
    }
}