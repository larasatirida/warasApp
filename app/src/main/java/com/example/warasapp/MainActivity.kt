package com.example.warasapp

import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.example.warasapp.logic.MoodLogRepository
import com.example.warasapp.logic.MissionType
import androidx.work.workDataOf

class MainActivity : AppCompatActivity() {

    private val symptomLabels = arrayOf(
        "Sakit kepala", "Nyeri punggung", "Mata lelah", "Sulit tidur",
        "Kurang nafsu makan", "Mudah marah", "Sulit fokus", "Kelelahan ekstrem"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // penjadwalan rutin di jam spesifik
        scheduleDailyNotifications()
        triggerMoodNotificationNow()
        triggerMissionNotificationNow()
        scheduleMissionCheck()
        scheduleBurnoutAlertCheck()
        scheduleMissionReminders()
        scheduleQuoteNotification()
        scheduleDailyAdviceNotification()
        scheduleGeneralReminder()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (intent.getStringExtra("OPEN_FRAGMENT") == "SYMPTOM_PAGE") {
            // isi gejala dulu, ga langsung ke dashboard
            handleSymptomNotificationTap()
        } else {
            goToDashboard()
        }
    }

    private fun handleSymptomNotificationTap() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(1002)
        NotifPrefsHelper.setPending(this, "fisik", false)
        WorkManager.getInstance(this).cancelUniqueWork("escalation_fisik")

        val checkedItems = BooleanArray(symptomLabels.size)

        AlertDialog.Builder(this)
            .setTitle("Keluhan fisik apa yang kamu rasakan?")
            .setMultiChoiceItems(symptomLabels, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setCancelable(false)
            .setPositiveButton("Simpan") { _, _ ->
                val selected = symptomLabels.filterIndexed { index, _ -> checkedItems[index] }
                saveSymptomsAndContinue(selected)
            }
            .setNegativeButton("Batal") { _, _ -> goToDashboard() }
            .show()
    }

    private fun saveSymptomsAndContinue(selected: List<String>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            goToDashboard()
            return
        }
        MoodLogRepository.saveEntry(
            userId = userId,
            symptoms = selected,
            onSuccess = {
                NotifPrefsHelper.setAnsweredToday(this, "fisik")
                SymptomNotifHelper.scheduleTips(this, selected)
                goToDashboard()
            },
            onFailure = {
                // tetep lanjut ke Dashboard walau gagal simpan, biar user nggak nyangkut.
                goToDashboard()
            }
        )
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
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

        if (!NotifPrefsHelper.isAnsweredToday(this, "mission_shown")) {
            val missionTestRequest = OneTimeWorkRequestBuilder<MissionSuggestWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(this).enqueue(missionTestRequest)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                // no-op untuk sekarang
            }
        }
    }

    private fun triggerMoodNotificationNow() {
        if (NotifPrefsHelper.isPending(this, "mood") || NotifPrefsHelper.isAnsweredToday(this, "mood")) {
            return
        }
        val request = OneTimeWorkRequestBuilder<MoodCheckInWorker>()
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(request)
    }

    private fun triggerMissionNotificationNow() {
        if (NotifPrefsHelper.isAnsweredToday(this, "mission_shown")) {
            return
        }
        val request = OneTimeWorkRequestBuilder<MissionSuggestWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(request)
    }

    private fun scheduleDailyNotifications() {
        val workManager = WorkManager.getInstance(this)

        // pengingat Jam 2 Siang (14:00)
        val delay14 = calculateDelay(14, 0)
        val request14 = PeriodicWorkRequestBuilder<MoodCheckInWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay14, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork("Mood_14PM", ExistingPeriodicWorkPolicy.KEEP, request14)

        // pengingat Jam 5 Sore (17:00)
        val delay17 = calculateDelay(17, 0)
        val request17 = PeriodicWorkRequestBuilder<MoodCheckInWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay17, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork("Mood_17PM", ExistingPeriodicWorkPolicy.KEEP, request17)

        // pengingat Jam 8 Malam (20:00)
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

    private fun scheduleBurnoutAlertCheck() {
        val request = PeriodicWorkRequestBuilder<BurnoutAlertWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("burnout_alert_check", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun scheduleMissionReminders() {
        val workManager = WorkManager.getInstance(this)

        val schedule = listOf(
            Triple("SleepMorning6", MissionType.SLEEP.key, Pair(6, 0)),
            Triple("SleepMorning9", MissionType.SLEEP.key, Pair(9, 0)),
            Triple("Overtime6PM", MissionType.NO_OVERTIME.key, Pair(18, 0)),
            Triple("Overtime9PM", MissionType.NO_OVERTIME.key, Pair(21, 0)),
            Triple("Exercise4PM", MissionType.EXERCISE.key, Pair(16, 0)),
            Triple("Exercise7PM", MissionType.EXERCISE.key, Pair(19, 0))
        )

        schedule.forEach { (workName, typeKey, time) ->
            val delay = calculateDelay(time.first, time.second)
            val request = PeriodicWorkRequestBuilder<MissionNotifWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("missionTypeKey" to typeKey))
                .build()
            workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    private fun scheduleQuoteNotification() {
        val delay = calculateDelay(12, 0)
        val request = PeriodicWorkRequestBuilder<QuoteNotifWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("QuoteNoon", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun scheduleDailyAdviceNotification() {
        // Misal dijadwalkan jam 10 pagi agar user bisa siap-siap
        val delay = calculateDelay(10, 0) 
        val request = PeriodicWorkRequestBuilder<DailyAdviceWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("DailyAdvice", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun scheduleGeneralReminder() {
        // Jadwalkan jam 9 malam (21:00) untuk mengecek apakah data sudah diisi
        val delay = calculateDelay(21, 0)
        val request = PeriodicWorkRequestBuilder<GeneralReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("GeneralReminder", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}