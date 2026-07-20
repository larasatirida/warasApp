package com.example.warasapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InputJadwalActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var dayChipContainer: LinearLayout
    private lateinit var rvActivities: RecyclerView
    private lateinit var emptyState: View
    private lateinit var tvWeekRange: TextView


    // The 7 days of the current week (Mon-Sun), as Calendar instances
    private val weekDays = mutableListOf<Calendar>()
    private var selectedDayIndex = 0

    private val dayNameFormat = SimpleDateFormat("EEE", Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_jadwal)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        dayChipContainer = findViewById(R.id.dayChipContainer)
        rvActivities = findViewById(R.id.rvActivities)
        emptyState = findViewById(R.id.emptyState)
        tvWeekRange = findViewById(R.id.tvWeekRange)

        rvActivities.layoutManager = LinearLayoutManager(this)

        buildCurrentWeek()
        renderDayChips()
        loadActivitiesForSelectedDay()

        findViewById<View>(R.id.btnAddManual).setOnClickListener {
            showAddActivityDialog()
        }

        findViewById<View>(R.id.btnConnectGoogle).setOnClickListener {
            // TODO: hook up Google Calendar OAuth + import flow here
            Toast.makeText(this, "Integrasi Google Calendar segera hadir", Toast.LENGTH_SHORT).show()
        }

    }

    /** Builds the Mon–Sun range for the current week and updates the header text. */
    private fun buildCurrentWeek() {
        weekDays.clear()
        val cal = Calendar.getInstance()
        // Move back to Monday of this week
        val diffToMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -diffToMonday)

        for (i in 0..6) {
            weekDays.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Default selection = today, if today falls in this week; else Monday
        val today = Calendar.getInstance()
        selectedDayIndex = weekDays.indexOfFirst {
            it.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    it.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        }.let { if (it >= 0) it else 0 }

        val first = weekDays.first()
        val last = weekDays.last()
        val fmt = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        tvWeekRange.text = "Minggu ini · ${first.get(Calendar.DAY_OF_MONTH)} – ${fmt.format(last.time)}"
    }

    /** Inflates the 7 day chips into dayChipContainer and wires selection. */
    private fun renderDayChips() {
        dayChipContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        weekDays.forEachIndexed { index, cal ->
            val chip = inflater.inflate(R.layout.item_day_chip, dayChipContainer, false)
            val tvDayName = chip.findViewById<TextView>(R.id.tvDayName)
            val tvDayNumber = chip.findViewById<TextView>(R.id.tvDayNumber)

            tvDayName.text = dayNameFormat.format(cal.time).replaceFirstChar { it.uppercase() }
            tvDayNumber.text = cal.get(Calendar.DAY_OF_MONTH).toString()

            applyChipStyle(chip, tvDayName, tvDayNumber, selected = index == selectedDayIndex)

            chip.setOnClickListener {
                selectedDayIndex = index
                renderDayChips() // re-render to update highlight state
                loadActivitiesForSelectedDay()
            }

            dayChipContainer.addView(chip)
        }
    }

    private fun applyChipStyle(chip: View, dayName: TextView, dayNumber: TextView, selected: Boolean) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_day_chip_selected)
            dayName.setTextColor(android.graphics.Color.parseColor("#C7CBE0"))
            dayNumber.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        } else {
            chip.setBackgroundResource(R.drawable.bg_day_chip)
            dayName.setTextColor(android.graphics.Color.parseColor("#9B9FA8"))
            dayNumber.setTextColor(android.graphics.Color.parseColor("#1A1C2E"))
        }
    }

    /** Loads activities for the selected day from Firestore and toggles the empty state. */
    private fun loadActivitiesForSelectedDay() {
        val userId = auth.currentUser?.uid ?: return
        val selected = weekDays[selectedDayIndex]

        val startOfDay = (selected.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val endOfDay = (selected.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }

        db.collection("activities")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", Timestamp(startOfDay.time))
            .whereLessThanOrEqualTo("date", Timestamp(endOfDay.time))
            .orderBy("date", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->


            }
            .addOnFailureListener {
                showEmptyState(true)
            }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvActivities.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    /** Shows a dialog to add a manual activity for the currently selected day. */
    private fun showAddActivityDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_activity, null)
        val etName = view.findViewById<EditText>(R.id.etActivityName)
        val etDuration = view.findViewById<EditText>(R.id.etDuration)

        AlertDialog.Builder(this)
            .setTitle("Tambah Aktivitas Manual")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                saveManualActivity(etName.text.toString().trim(), etDuration.text.toString().trim())
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveManualActivity(name: String, durationText: String) {
        if (name.isEmpty() || durationText.isEmpty()) {
            Toast.makeText(this, "Nama aktivitas dan durasi wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val duration = durationText.toIntOrNull()
        if (duration == null || duration <= 0) {
            Toast.makeText(this, "Durasi harus berupa angka valid", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Sesi login habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDate = weekDays[selectedDayIndex].time

        val activityData = hashMapOf(
            "userId" to userId,
            "name" to name,
            "durationMinutes" to duration,
            "date" to Timestamp(selectedDate),
            "source" to "manual"
        )

        db.collection("activities")
            .add(activityData)
            .addOnSuccessListener {
                Toast.makeText(this, "Aktivitas berhasil disimpan", Toast.LENGTH_SHORT).show()
                loadActivitiesForSelectedDay()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}