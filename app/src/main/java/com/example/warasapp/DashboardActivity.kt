package com.example.warasapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Date

import androidx.lifecycle.lifecycleScope
import com.example.warasapp.logic.getTodayBurnoutScore
import com.example.warasapp.logic.getWeeklyTrend
import com.example.warasapp.logic.burnoutScoreCategory
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Data class buat nampung skor + tanggal per hari
    data class DailyBurnoutData(val label: String, val score: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Menggunakan helper untuk navigasi agar kode lebih ringkas
        BottomNavHelper.setup(this, "beranda")

        val scrollContent = findViewById<ScrollView>(R.id.scrollContent)
        ViewCompat.setOnApplyWindowInsetsListener(scrollContent) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, view.paddingBottom)
            insets
        }

        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvBurnoutScore = findViewById<TextView>(R.id.tvBurnoutScore)
        val tvBurnoutLevel = findViewById<TextView>(R.id.tvBurnoutLevel)

        val btnCheckIn = findViewById<Button>(R.id.btnCheckIn)
        val btnMission = findViewById<Button>(R.id.btnMission)

        // Ambil nama dari Firestore agar lebih akurat
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val nameFromFirestore = document.getString("fullName") ?: document.getString("name")
                    if (nameFromFirestore != null) {
                        tvUserName.text = nameFromFirestore
                    } else {
                        tvUserName.text = auth.currentUser?.displayName ?: "Pengguna"
                    }
                }
                .addOnFailureListener {
                    tvUserName.text = auth.currentUser?.displayName ?: "Pengguna"
                }
        } else {
            tvUserName.text = "Pengguna"
        }

        loadDashboardData(tvBurnoutScore, tvBurnoutLevel)

        btnCheckIn.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnMission.setOnClickListener {
            startActivity(Intent(this, MissionActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }

    private fun loadDashboardData(tvScore: TextView, tvLevel: TextView) {
        lifecycleScope.launch {
            try {
                // 1. Ambil skor hari ini (sudah menggabungkan mood & jam kerja)
                val todayScore = getTodayBurnoutScore()
                tvScore.text = todayScore.toString()
                tvLevel.text = burnoutScoreCategory(todayScore)

                // Update Burnout Progress Bar (Horizontal line)
                val progressView = findViewById<android.view.View>(R.id.viewBurnoutProgress)
                val params = progressView.layoutParams as android.widget.LinearLayout.LayoutParams
                params.weight = todayScore.toFloat().coerceIn(0f, 100f)
                progressView.layoutParams = params

                // 2. Ambil tren mingguan
                val trend = getWeeklyTrend()
                renderTrendChart(trend.map { DailyBurnoutData(it.dayLabel, it.score) })
                
                updateInsights()
            } catch (e: Exception) {
                tvScore.text = "0"
                tvLevel.text = "Gagal memuat"
            }
        }
    }

    private fun updateInsights() {
        val container = findViewById<LinearLayout>(R.id.insightContainer)
        container.removeAllViews()
        
        val tvNoData = TextView(this).apply {
            text = "Belum ada insight hari ini. Yuk isi jadwal dan mood kamu!"
            textSize = 13f
            setTextColor(Color.parseColor("#999999"))
            setPadding(0, dpToPx(10), 0, dpToPx(20))
        }
        container.addView(tvNoData)
    }

    private fun renderTrendChart(dailyData: List<DailyBurnoutData>) {
        val barsContainer = findViewById<LinearLayout>(R.id.chartBarsContainer)
        val labelsContainer = findViewById<LinearLayout>(R.id.chartLabelsContainer)
        barsContainer.removeAllViews()
        labelsContainer.removeAllViews()

        if (dailyData.isEmpty()) return

        // Ambil hari ini untuk highlight (misal: "Sel" atau "Tue")
        val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale("id"))
        val todayLabel = sdf.format(java.util.Date())

        for (day in dailyData) {
            val isToday = day.label.equals(todayLabel, ignoreCase = true)

            val barWrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
                ).apply {
                    marginStart = dpToPx(4)
                    marginEnd = dpToPx(4)
                }
            }

            if (isToday) {
                val scoreLabel = TextView(this).apply {
                    text = day.score.toString()
                    setTextColor(Color.parseColor("#F5C518"))
                    textSize = 10f
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpToPx(2) }
                }
                barWrapper.addView(scoreLabel)
            }

            val barHeightDp = 20 + (day.score * 50 / 100)
            val bar = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(barHeightDp)
                )
                radius = dpToPx(6).toFloat()
                setCardBackgroundColor(
                    Color.parseColor(if (isToday) "#F5C518" else "#55F5C518")
                )
                cardElevation = 0f
            }
            barWrapper.addView(bar)
            barsContainer.addView(barWrapper)

            val label = TextView(this).apply {
                text = day.label
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(if (isToday) "#1A1A2E" else "#CCCCCC"))
                if (isToday) setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }
            labelsContainer.addView(label)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}