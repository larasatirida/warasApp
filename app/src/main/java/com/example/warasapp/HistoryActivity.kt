package com.example.warasapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.warasapp.logic.MoodLogRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var selectedMoodValue: Int? = null

    private data class MoodOption(val label: String, val value: Int)
    private val moodViews = mutableMapOf<MaterialCardView, MoodOption>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Gunakan helper navigasi yang seragam
        BottomNavHelper.setup(this, "checkin")

        val scrollContent = findViewById<ScrollView>(R.id.scrollContent)
        ViewCompat.setOnApplyWindowInsetsListener(scrollContent) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, view.paddingBottom)
            insets
        }

        moodViews[findViewById(R.id.moodBaik)] = MoodOption("Baik", 3)
        moodViews[findViewById(R.id.moodBiasa)] = MoodOption("Biasa", 2)
        moodViews[findViewById(R.id.moodBuruk)] = MoodOption("Buruk", 1)

        moodViews.forEach { (view, option) ->
            view.setOnClickListener {
                selectedMoodValue = option.value
                highlightSelectedMood(view)
            }
        }

        val chipGroupKendala = findViewById<ChipGroup>(R.id.chipGroupKendala)
        val etCatatan = findViewById<EditText>(R.id.etCatatan)
        val btnSimpan = findViewById<Button>(R.id.btnSimpanCheckIn)

        btnSimpan.setOnClickListener {
            if (NotifPrefsHelper.isAnsweredToday(this, "mood")) {
                Toast.makeText(this, "Kamu udah check-in hari ini", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedMoodValue == null) {
                Toast.makeText(this, "Pilih mood dulu ya", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Kamu belum login", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val kendalaTerpilih = chipGroupKendala.checkedChipIds.map { id ->
                chipGroupKendala.findViewById<Chip>(id).text.toString()
            }
            val catatan = etCatatan.text.toString()

            MoodLogRepository.saveEntry(
                userId = userId,
                mood = selectedMoodValue,
                symptoms = kendalaTerpilih,
                notes = catatan,
                onSuccess = {
                    NotifPrefsHelper.setAnsweredToday(this, "mood")
                    Toast.makeText(this, "Check-in berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    SymptomNotifHelper.scheduleTips(this, kendalaTerpilih)
                    loadHistory()
                    etCatatan.setText("")
                    chipGroupKendala.clearCheck()
                    selectedMoodValue = null
                    moodViews.keys.forEach { it.isChecked = false }
                },
                onFailure = { e ->
                    Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        val chips = listOf(
            findViewById<Chip>(R.id.chipSakitKepala),
            findViewById<Chip>(R.id.chipNyeriPunggung),
            findViewById<Chip>(R.id.chipMataLelah),
            findViewById<Chip>(R.id.chipSulitTidur),
            findViewById<Chip>(R.id.chipKurangMakan),
            findViewById<Chip>(R.id.chipMudahMarah),
            findViewById<Chip>(R.id.chipSulitFokus),
            findViewById<Chip>(R.id.chipKelelahanEkstrem)
        )

        chips.forEach { chip ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2D3748")))
                    chip.setTextColor(Color.WHITE)
                } else {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F5F5F0")))
                    chip.setTextColor(Color.BLACK)
                }
            }
        }

        // Tampilkan tanggal hari ini yang sesuai
        val sdf = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID"))
        findViewById<TextView>(R.id.tvDateSubtitle).text = "${sdf.format(Date())} · Bagaimana kondisimu?"

        loadHistory()
    }

    private val moodColors = mapOf(
        "Baik" to Pair("#F0FDF4", "#FAF9F5"),
        "Biasa" to Pair("#EFF6FF", "#FAF9F5"),
        "Buruk" to Pair("#FFF5F5", "#FAF9F5")
    )

    private fun highlightSelectedMood(selectedView: MaterialCardView) {
        val liftPx = -8 * resources.displayMetrics.density

        moodViews.forEach { (view, option) ->
            val isSelected = (view == selectedView)
            view.isChecked = isSelected

            val (checkedColor, defaultColor) = moodColors[option.label] ?: Pair("#FAF9F5", "#FAF9F5")
            view.setCardBackgroundColor(
                Color.parseColor(if (isSelected) checkedColor else defaultColor)
            )

            view.animate()
                .translationY(if (isSelected) liftPx else 0f)
                .setDuration(150)
                .start()
        }
    }

    private fun loadHistory() {
        val userId = auth.currentUser?.uid ?: return
        val container = findViewById<LinearLayout>(R.id.historyContainer)

        db.collection("mood_logs")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                container.removeAllViews()
                if (snapshot.isEmpty) return@addOnSuccessListener

                val inflater = LayoutInflater.from(this)
                for (doc in snapshot.documents) {
                    val mood = doc.getLong("mood")?.toInt() ?: 2
                    val symptoms = (doc.get("physicalSymptoms") as? List<*>)
                        ?.map { it.toString() } ?: emptyList()
                    val note = doc.getString("notes") ?: ""
                    val timestamp = doc.getDate("timestamp") ?: continue

                    val itemView = inflater.inflate(R.layout.item_history_checkin, container, false)
                    bindHistoryItem(itemView, mood, symptoms, note, timestamp)
                    container.addView(itemView)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat riwayat", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bindHistoryItem(
        view: View,
        mood: Int,
        symptoms: List<String>,
        note: String,
        timestamp: Date
    ) {
        val ivIcon = view.findViewById<ImageView>(R.id.ivMoodIcon)
        val tvDate = view.findViewById<TextView>(R.id.tvHistoryDate)
        val tvMoodLabel = view.findViewById<TextView>(R.id.tvMoodLabel)
        val llTags = view.findViewById<LinearLayout>(R.id.llSymptomTags)
        val tvNote = view.findViewById<TextView>(R.id.tvHistoryNote)

        tvDate.text = SimpleDateFormat("EEE, d MMM", Locale("id", "ID")).format(timestamp)

        val (iconRes, label, textColor, bgColor) = when (mood) {
            3 -> arrayOf(R.drawable.ic_baik, "Baik", "#2CD97B", "#E8F9EE")
            2 -> arrayOf(R.drawable.ic_biasa, "Biasa", "#4FA6FF", "#E8F4FF")
            else -> arrayOf(R.drawable.ic_buruk, "Buruk", "#F87171", "#FEF2F2")
        }
        ivIcon.setImageResource(iconRes as Int)
        tvMoodLabel.text = label as String
        tvMoodLabel.setTextColor(Color.parseColor(textColor as String))
        tvMoodLabel.setBackgroundColor(Color.parseColor(bgColor as String))

        llTags.removeAllViews()
        for (symptom in symptoms) {
            val tag = TextView(this).apply {
                text = symptom
                textSize = 11f
                setTextColor(Color.parseColor("#F87171"))
                setBackgroundColor(Color.parseColor("#FEF2F2"))
                setPadding(dp(10), dp(6), dp(10), dp(6))
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
            llTags.addView(tag, params)
        }

        tvNote.text = if (note.isNotBlank()) "\"$note\"" else ""
        tvNote.visibility = if (note.isNotBlank()) View.VISIBLE else View.GONE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}