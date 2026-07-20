package com.example.warasapp

import android.content.Context

object NotifPrefsHelper {
    private const val PREFS_NAME = "notif_status_prefs"

    fun setPending(context: Context, type: String, pending: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${type}_pending", pending).apply()
    }

    fun isPending(context: Context, type: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("${type}_pending", false)
    }

    fun getAttempt(context: Context, type: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("${type}_attempt", 0)
    }

    fun setAttempt(context: Context, type: String, attempt: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("${type}_attempt", attempt).apply()
    }

    fun setLastNotifiedMissionId(context: Context, missionId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("last_mission_notified", missionId).apply()
    }

    fun getLastNotifiedMissionId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("last_mission_notified", null)
    }

    fun setAnsweredToday(context: Context, type: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        prefs.edit().putString("${type}_answered_date", today).apply()
    }

    fun isAnsweredToday(context: Context, type: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val savedDate = prefs.getString("${type}_answered_date", null)
        return savedDate == today
    }

    fun setNotifEnabled(context: Context, type: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${type}_enabled", enabled).apply()
    }

    fun isNotifEnabled(context: Context, type: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("${type}_enabled", true) // default ON
    }
}