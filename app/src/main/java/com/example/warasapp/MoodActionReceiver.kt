package com.example.warasapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.util.Date
import java.util.concurrent.TimeUnit

class MoodActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val moodValue = intent.getIntExtra("MOOD_VALUE", -1)
        if (moodValue == -1) return

        NotifPrefsHelper.setPending(context, "mood", false)
        WorkManager.getInstance(context).cancelUniqueWork("escalation_mood")

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_test"

        val moodLog = hashMapOf(
            "userId" to userId,
            "mood" to moodValue,
            "timestamp" to Date()
        )

        FirebaseFirestore.getInstance()
            .collection("mood_logs")
            .add(moodLog)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(1001)

        val fisikRequest = OneTimeWorkRequestBuilder<FisikCheckInWorker>()
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(fisikRequest)
    }
}