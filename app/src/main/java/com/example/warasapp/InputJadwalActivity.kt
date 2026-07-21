package com.example.warasapp

import android.app.AlertDialog
import android.app.TimePickerDialog
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

    private lateinit var adapter: ActivityAdapter
    private val activityList = mutableListOf<ActivityItem>()

    private val weekDays = mutableListOf<Calendar>()
    private var selectedDayIndex = 0

    private val dayNameFormat = SimpleDateFormat("EEE", Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_jadwal)
        BottomNavHelper.setup(this, "jadwal")
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        dayChipContainer = findViewById(R.id.dayChipContainer)
        rvActivities = findViewById(R.id.rvActivities)
        emptyState = findViewById(R.id.emptyState)
        tvWeekRange = findViewById(R.id.tvWeekRange)

        adapter = ActivityAdapter(
            activityList,
            onEditClick = { item -> showAddActivityDialog(item) },
            onDeleteClick = { item -> confirmDeleteActivity(item) }
        )
        rvActivities.layoutManager = LinearLayoutManager(this)
        rvActivities.adapter = adapter

        buildCurrentWeek()
        renderDayChips()
        loadActivitiesForSelectedDay()

        findViewById<View>(R.id.btnAddManual).setOnClickListener {
            showAddActivityDialog()
        }

        findViewById<View>(R.id.btnConnectGoogle).setOnClickListener {
            Toast.makeText(this, "Integrasi Google Calendar segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildCurrentWeek() {
        weekDays.clear()
        val cal = Calendar.getInstance()
        val diffToMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -diffToMonday)

        for (i in 0..6) {
            weekDays.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

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
                renderDayChips()
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
            .get()
            .addOnSuccessListener { snapshot ->
                activityList.clear()
                for (doc in snapshot.documents) {
                    val name = doc.getString("name") ?: "Tanpa nama"
                    val duration = doc.getLong("durationMinutes")?.toInt() ?: 0
                    val source = doc.getString("source") ?: "manual"
                    val start = doc.getString("startTime") ?: "--:--"
                    val end = doc.getString("endTime") ?: "--:--"
                    activityList.add(ActivityItem(doc.id, name, duration, source, start, end))
                }
                
                showEmptyState(activityList.isEmpty())
                adapter.updateData(activityList)
            }
            .addOnFailureListener { e ->
                showEmptyState(true)
                Toast.makeText(this, "Gagal memuat data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvActivities.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showAddActivityDialog(itemToEdit: ActivityItem? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_activity, null)
        val etName = view.findViewById<EditText>(R.id.etActivityName)
        val etStart = view.findViewById<EditText>(R.id.etStartTime)
        val etEnd = view.findViewById<EditText>(R.id.etEndTime)

        var sH = 9; var sM = 0
        var eH = 10; var eM = 0

        if (itemToEdit != null) {
            etName.setText(itemToEdit.name)
            etStart.setText(itemToEdit.startTime)
            etEnd.setText(itemToEdit.endTime)
            val startParts = itemToEdit.startTime.split(":")
            if (startParts.size == 2) {
                sH = startParts[0].toIntOrNull() ?: 9
                sM = startParts[1].toIntOrNull() ?: 0
            }
            val endParts = itemToEdit.endTime.split(":")
            if (endParts.size == 2) {
                eH = endParts[0].toIntOrNull() ?: 10
                eM = endParts[1].toIntOrNull() ?: 0
            }
        }

        etStart.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                sH = h; sM = m
                etStart.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m))
            }, sH, sM, true).show()
        }

        etEnd.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                eH = h; eM = m
                etEnd.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m))
            }, eH, eM, true).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (itemToEdit == null) "Tambah Aktivitas" else "Edit Aktivitas")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty() || etStart.text.isEmpty() || etEnd.text.isEmpty()) {
                    Toast.makeText(this, "Semua data wajib diisi", Toast.LENGTH_SHORT).show()
                } else {
                    saveManualActivity(itemToEdit?.id, name, sH, sM, eH, eM)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveManualActivity(docId: String?, name: String, sH: Int, sM: Int, eH: Int, eM: Int) {
        val startTotalMin = sH * 60 + sM
        val endTotalMin = eH * 60 + eM
        var duration = endTotalMin - startTotalMin
        if (duration < 0) duration += 24 * 60
        if (duration == 0) {
            Toast.makeText(this, "Jam mulai dan selesai tidak boleh sama", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val selectedDate = weekDays[selectedDayIndex].time

        val activityData = hashMapOf(
            "userId" to userId,
            "name" to name,
            "durationMinutes" to duration,
            "startTime" to String.format(Locale.getDefault(), "%02d:%02d", sH, sM),
            "endTime" to String.format(Locale.getDefault(), "%02d:%02d", eH, eM),
            "date" to Timestamp(selectedDate),
            "source" to "manual"
        )

        val collection = db.collection("activities")
        val task = if (docId == null) collection.add(activityData) else collection.document(docId).set(activityData)

        task.addOnSuccessListener {
            Toast.makeText(this, "Berhasil disimpan", Toast.LENGTH_SHORT).show()
            loadActivitiesForSelectedDay()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteActivity(item: ActivityItem) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Aktivitas")
            .setMessage("Yakin ingin menghapus \"${item.name}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("activities").document(item.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadActivitiesForSelectedDay()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
