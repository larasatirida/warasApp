package com.example.warasapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.warasapp.logic.MissionProgress
import com.example.warasapp.logic.MissionType
import com.example.warasapp.logic.getAllActiveMissions
import com.example.warasapp.logic.getTotalXP
import com.example.warasapp.logic.logMissionCheckin
import com.example.warasapp.logic.xpToLevel
import kotlinx.coroutines.launch

class MissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mission)

        BottomNavHelper.setup(this, "misi")

        loadMissionData()

        findViewById<Button>(R.id.btnAddMission).setOnClickListener {
            Toast.makeText(this, "Belum ada misi baru", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMissionData() {
        lifecycleScope.launch {
            val missions = getAllActiveMissions()
            val totalXp = getTotalXP()
            val (level, currentXp) = xpToLevel(totalXp)
            val nextLevelXp = level * 100

            updateHeader(level, totalXp, currentXp, nextLevelXp)
            renderMissionList(missions)
        }
    }

    private fun updateHeader(level: Int, totalXp: Int, currentXp: Int, nextLevelXp: Int) {
        findViewById<TextView>(R.id.tvLevelInfo).text = "Level $level • $totalXp XP total"
        findViewById<TextView>(R.id.tvLevelBadge).text = "Lv. $level"
        findViewById<TextView>(R.id.tvXpProgress).text = "$currentXp / $nextLevelXp XP"
        findViewById<TextView>(R.id.tvNextLevel).text = "Level ${level + 1} →"

        val progressView = findViewById<View>(R.id.viewXpProgress)
        val params = progressView.layoutParams as LinearLayout.LayoutParams
        params.weight = (currentXp.toFloat() / nextLevelXp).coerceIn(0f, 1f)
        progressView.layoutParams = params
    }

    private fun renderMissionList(missions: List<MissionProgress>) {
        val container = findViewById<LinearLayout>(R.id.missionListContainer)
        container.removeAllViews()

        val aktifCount = missions.size
        findViewById<TextView>(R.id.tvTabAktif).text = "Aktif ($aktifCount)"
        findViewById<TextView>(R.id.tvTabSelesai).text = "Selesai (0)"
        findViewById<TextView>(R.id.tvTabGagal).text = "Gagal (0)"

        val inflater = LayoutInflater.from(this)

        missions.forEach { mission ->
            val itemView = inflater.inflate(R.layout.item_mission, container, false)
            
            val titleText = itemView.findViewById<TextView>(R.id.tvMissionTitle)
            val subtitleText = itemView.findViewById<TextView>(R.id.tvMissionSubtitle)
            val xpRewardText = itemView.findViewById<TextView>(R.id.tvXpReward)
            val progressText = itemView.findViewById<TextView>(R.id.tvProgressText)
            val progressPercentText = itemView.findViewById<TextView>(R.id.tvProgressPercent)
            val icon = itemView.findViewById<ImageView>(R.id.ivMissionIcon)
            val progressFill = itemView.findViewById<View>(R.id.viewProgressFill)
            val btnCheckIn = itemView.findViewById<Button>(R.id.btnCheckInMission)

            titleText.text = mission.title
            xpRewardText.text = "+${mission.xpReward}"
            progressText.text = "${mission.currentProgress} / ${mission.targetProgress} selesai"
            progressPercentText.text = "${mission.percentComplete}%"
            
            val type = when (mission.title) {
                "Tidur 7+ Jam" -> {
                    subtitleText.text = "Tidur cukup setiap hari"
                    icon.setImageResource(R.drawable.ic_sleepy)
                    progressFill.setBackgroundColor(Color.parseColor("#4A90E2"))
                    MissionType.SLEEP
                }
                "Tanpa Lembur" -> {
                    subtitleText.text = "Pulang tepat waktu"
                    icon.setImageResource(R.drawable.ic_check)
                    progressFill.setBackgroundColor(Color.parseColor("#9F7AEA"))
                    MissionType.NO_OVERTIME
                }
                "Olahraga Rutin" -> {
                    subtitleText.text = "Biar badan tetap bugar"
                    icon.setImageResource(R.drawable.ic_bubble)
                    progressFill.setBackgroundColor(Color.parseColor("#48BB78"))
                    MissionType.EXERCISE
                }
                else -> null
            }

            val fillParams = progressFill.layoutParams as LinearLayout.LayoutParams
            fillParams.weight = (mission.percentComplete.toFloat() / 100).coerceIn(0f, 1f)
            progressFill.layoutParams = fillParams

            btnCheckIn.setOnClickListener {
                type?.let { missionType ->
                    showConfirmationDialog(missionType)
                }
            }

            container.addView(itemView)
        }
    }

    private fun showConfirmationDialog(missionType: MissionType) {
        AlertDialog.Builder(this)
            .setTitle("Check-in Misi")
            .setMessage("Apakah kamu sudah menyelesaikan misi \"${missionType.title}\" hari ini?")
            .setPositiveButton("Sudah") { _, _ ->
                performCheckIn(missionType)
            }
            .setNegativeButton("Belum", null)
            .show()
    }

    private fun performCheckIn(missionType: MissionType) {
        lifecycleScope.launch {
            val success = logMissionCheckin(missionType)
            if (success) {
                com.example.warasapp.logic.checkAndAwardMissionCompletion(missionType)
                Toast.makeText(this@MissionActivity, "Berhasil!", Toast.LENGTH_SHORT).show()
                loadMissionData()
            } else {
                Toast.makeText(this@MissionActivity, "Sudah check-in hari ini.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
