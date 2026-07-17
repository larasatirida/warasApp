package com.example.warasapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class WarasApplication : Application() {

    companion object {
        const val CHANNEL_MOOD = "channel_mood_checkin"
        const val CHANNEL_FISIK = "channel_kendala_fisik"
        const val CHANNEL_MISSION = "channel_mission"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val moodChannel = NotificationChannel(
                CHANNEL_MOOD,
                "Check-in mood",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat check-in mood harian"
            }

            val fisikChannel = NotificationChannel(
                CHANNEL_FISIK,
                "Kendala fisik",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat catat keluhan fisik harian"
            }

            val missionChannel = NotificationChannel(
                CHANNEL_MISSION,
                "Saran & misi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi micro-recovery mission"
            }

            manager.createNotificationChannel(moodChannel)
            manager.createNotificationChannel(fisikChannel)
            manager.createNotificationChannel(missionChannel)
        }
    }
}