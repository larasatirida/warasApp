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

class DashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Data class buat nampung skor + tanggal per hari, dipakai chart & (nanti) skor utama
    data class DailyBurnout(val date: Date, val score: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

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

        val navBeranda = findViewById<LinearLayout>(R.id.navBeranda)
        val navJadwal = findViewById<LinearLayout>(R.id.navJadwal)
        val navCheckIn = findViewById<LinearLayout>(R.id.navCheckIn)
        val navMisi = findViewById<LinearLayout>(R.id.navMisi)
        val navProfil = findViewById<LinearLayout>(R.id.navProfil)

        tvUserName.text = auth.currentUser?.displayName ?: "Pengguna"

        loadBurnoutData(tvBurnoutScore, tvBurnoutLevel)

        btnCheckIn.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        btnMission.setOnClickListener {
            // TODO: pindah ke halaman Mission
        }

        navBeranda.setOnClickListener { /* sudah di halaman ini */ }
        navJadwal.setOnClickListener { /* TODO: buka JadwalActivity */ }
        navCheckIn.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        navMisi.setOnClickListener { /* TODO: buka MisiActivity */ }
        navProfil.setOnClickListener { /* TODO: buka ProfilActivity */ }
    }

    private fun loadBurnoutData(tvScore: TextView, tvLevel: TextView) {
        val userId = auth.currentUser?.uid ?: return

        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.time

        db.collection("mood_logs")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgo)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    tvScore.text = "-"
                    tvLevel.text = "Belum ada data"
                    renderTrendChart(emptyList())
                    return@addOnSuccessListener
                }

                val dailyData = mutableListOf<DailyBurnout>()

                for (doc in snapshot.documents) {
                    val mood = doc.getLong("mood")?.toInt() ?: 2
                    val hasSymptom = doc.getBoolean("hasPhysicalSymptom") ?: false
                    val timestamp = doc.getDate("timestamp") ?: continue

                    val moodContribution = (4 - mood) * 20
                    val symptomPenalty = if (hasSymptom) 10 else 0
                    val dayScore = (moodContribution + symptomPenalty).coerceIn(0, 100)

                    dailyData.add(DailyBurnout(timestamp, dayScore))
                }

                val avgScore = dailyData.map { it.score }.average().toInt()
                tvScore.text = avgScore.toString()
                tvLevel.text = when {
                    avgScore < 34 -> "Rendah"
                    avgScore < 67 -> "Sedang"
                    else -> "Tinggi"
                }

                renderTrendChart(dailyData)
            }
            .addOnFailureListener {
                tvScore.text = "-"
                tvLevel.text = "Gagal memuat"
                renderTrendChart(emptyList())
            }
    }

    private fun renderTrendChart(dailyData: List<DailyBurnout>) {
        val barsContainer = findViewById<LinearLayout>(R.id.chartBarsContainer)
        val labelsContainer = findViewById<LinearLayout>(R.id.chartLabelsContainer)
        barsContainer.removeAllViews()
        labelsContainer.removeAllViews()

        if (dailyData.isEmpty()) return

        val today = Calendar.getInstance()
        // Calendar.DAY_OF_WEEK: 1=Minggu, 2=Senin, ... 7=Sabtu
        val dayInitials = arrayOf("M", "S", "S", "R", "K", "J", "S")

        for (day in dailyData) {
            val cal = Calendar.getInstance().apply { time = day.date }
            val isToday = cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)

            // --- Bar (+ angka skor kalau hari ini) ---
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
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpToPx(2) }
                }
                barWrapper.addView(scoreLabel)
            }

            val barHeightDp = 20 + (day.score * 50 / 100) // rentang 20dp - 70dp
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

            // --- Label hari (S/S/R/K/J/S/M) ---
            val label = TextView(this).apply {
                text = dayInitials[cal.get(Calendar.DAY_OF_WEEK) - 1]
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(if (isToday) "#1A1A2E" else "#CCCCCC"))
                if (isToday) setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }
            labelsContainer.addView(label)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}