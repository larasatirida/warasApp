package com.example.warasapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.util.Date

class MoodActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val moodValue = intent.getIntExtra("MOOD_VALUE", -1)
        if (moodValue == -1) return

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
    }
}