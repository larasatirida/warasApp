package com.example.warasapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.os.Build
import android.app.NotificationManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (intent.getStringExtra("OPEN_FRAGMENT") == "SYMPTOM_PAGE") {
            val manager = getSystemService(NotificationManager::class.java)
            manager.cancel(1002)
            NotifPrefsHelper.setPending(this, "fisik", false)
            WorkManager.getInstance(this).cancelUniqueWork("escalation_fisik")
        }

        // Jalankan Penjadwalan Rutin di Jam Spesifik
        scheduleDailyNotifications()
        scheduleMissionCheck()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStop() {
        super.onStop()

        val moodPending = NotifPrefsHelper.isPending(this, "mood")
        val fisikPending = NotifPrefsHelper.isPending(this, "fisik")
        val moodAnswered = NotifPrefsHelper.isAnsweredToday(this, "mood")
        val fisikAnswered = NotifPrefsHelper.isAnsweredToday(this, "fisik")

        if (!moodPending && !fisikPending && !moodAnswered && !fisikAnswered) {
            val exitAppRequest = OneTimeWorkRequestBuilder<MoodCheckInWorker>()
                .setInitialDelay(15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(this).enqueue(exitAppRequest)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
    
    private fun scheduleDailyNotifications() {
        val workManager = WorkManager.getInstance(this)

        // Daftarkan pengingat Jam 2 Siang (14:00)
        val delay14 = calculateDelay(14, 0)
        val request14 = PeriodicWorkRequestBuilder<MoodCheckInWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay14, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork("Mood_14PM", ExistingPeriodicWorkPolicy.KEEP, request14)

        // Daftarkan pengingat Jam 5 Sore (17:00)
        val delay17 = calculateDelay(17, 0)
        val request17 = PeriodicWorkRequestBuilder<MoodCheckInWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay17, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork("Mood_17PM", ExistingPeriodicWorkPolicy.KEEP, request17)

        // Daftarkan pengingat Jam 8 Malam (20:00)
        val delay20 = calculateDelay(20, 0)
        val request20 = PeriodicWorkRequestBuilder<MoodCheckInWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay20, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork("Mood_20PM", ExistingPeriodicWorkPolicy.KEEP, request20)
    }

    private fun scheduleMissionCheck() {
        val request = PeriodicWorkRequestBuilder<MissionCheckWorker>(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("mission_check", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    // Fungsi pembantu untuk menghitung sisa waktu (milidetik) menuju jam target
    private fun calculateDelay(targetHour: Int, targetMinute: Int): Long {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }

        // Jika jam target sudah lewat untuk hari ini, jadwalkan untuk besok
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        return dueDate.timeInMillis - currentDate.timeInMillis
    }
}