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
}