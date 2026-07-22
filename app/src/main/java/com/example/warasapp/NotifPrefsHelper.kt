package com.example.warasapp

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object NotifPrefsHelper {
    private const val PREFS_NAME = "notif_status_prefs"

    private fun uid(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    }

    fun setPending(context: Context, type: String, pending: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${uid()}_${type}_pending", pending).apply()
    }

    fun isPending(context: Context, type: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("${uid()}_${type}_pending", false)
    }

    fun getAttempt(context: Context, type: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("${uid()}_${type}_attempt", 0)
    }

    fun setAttempt(context: Context, type: String, attempt: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("${uid()}_${type}_attempt", attempt).apply()
    }

    fun setAnsweredToday(context: Context, type: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        prefs.edit().putString("${uid()}_${type}_answered_date", today).apply()
    }

    fun isAnsweredToday(context: Context, type: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val savedDate = prefs.getString("${uid()}_${type}_answered_date", null)
        return savedDate == today
    }

    fun setLastNotifiedMissionId(context: Context, missionId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("${uid()}_last_mission_notified", missionId).apply()
    }

    fun getLastNotifiedMissionId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("${uid()}_last_mission_notified", null)
    }

    fun setNotifEnabled(context: Context, type: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${uid()}_${type}_enabled", enabled).apply()
    }

    fun isNotifEnabled(context: Context, type: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("${uid()}_${type}_enabled", true)
    }
}