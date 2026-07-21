package com.example.warasapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

object BottomNavHelper {

    private val ACTIVE_COLOR = Color.parseColor("#1A1A2E")
    private val INACTIVE_COLOR = Color.parseColor("#AAAAAA")

    fun setup(activity: AppCompatActivity, currentTag: String) {

        // 1. Definisikan mapping antara ID layout dan Class tujuan
        val navItems: Map<Int, Class<*>> = mapOf(
            R.id.navBeranda to DashboardActivity::class.java,
            R.id.navJadwal to InputJadwalActivity::class.java,
            R.id.navCheckIn to HistoryActivity::class.java,
            R.id.navMisi to MissionActivity::class.java,
            R.id.navProfil to ProfileActivity::class.java
        )

        // 2. Loop untuk styling SEMUA tab (termasuk yang belum ada activity-nya)
        val allTabs = listOf(
            R.id.navBeranda, R.id.navJadwal, R.id.navCheckIn, R.id.navMisi, R.id.navProfil
        )

        allTabs.forEach { viewId ->
            val navView = activity.findViewById<LinearLayout>(viewId)
            if (navView == null) {
                Log.e("NavError", "Tombol dengan ID $viewId tidak ditemukan!")
                return@forEach
            }

            // Set warna aktif/nonaktif berdasarkan currentTag
            val isActive = getTagFromId(viewId) == currentTag
            val icon = navView.getChildAt(0) as? ImageView
            val label = navView.getChildAt(1) as? TextView

            label?.setTextColor(if (isActive) ACTIVE_COLOR else INACTIVE_COLOR)
            label?.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
            icon?.alpha = if (isActive) 1f else 0.6f
        }

        // 3. Loop untuk klik/navigasi (hanya untuk tab yang activity-nya sudah ada)
        navItems.forEach { (viewId, targetActivity) ->
            val navView = activity.findViewById<LinearLayout>(viewId)

            navView?.setOnClickListener {
                if (currentTag != getTagFromId(viewId)) {
                    activity.startActivity(Intent(activity, targetActivity))
                    activity.overridePendingTransition(0, 0)
                    activity.finish()
                }
            }
        }
    }

    private fun getTagFromId(viewId: Int): String {
        return when (viewId) {
            R.id.navBeranda -> "beranda"
            R.id.navJadwal -> "jadwal"
            R.id.navCheckIn -> "checkin"
            R.id.navMisi -> "misi"
            R.id.navProfil -> "profil"
            else -> ""
        }
    }
}