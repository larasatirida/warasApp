package com.example.warasapp

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
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

    // Struktur Data untuk Kategori & Icon Vektor
    private data class KategoriItem(val nama: String, val iconResId: Int, val iconName: String)

    private val daftarIkonTersedia = listOf(
        Pair(R.drawable.ic_jadwal, "ic_jadwal"),
        Pair(R.drawable.ic_mission, "ic_mission"),
        Pair(R.drawable.ic_profile, "ic_profile"),
        Pair(R.drawable.ic_check, "ic_check"),
        Pair(R.drawable.ic_warning, "ic_warning"),
        Pair(R.drawable.ic_sleepy, "ic_sleepy")
    )

    private val daftarKategori = mutableListOf(
        KategoriItem("Pekerjaan", R.drawable.ic_jadwal, "ic_jadwal"),
        KategoriItem("Istirahat", R.drawable.ic_sleepy, "ic_sleepy"),
        KategoriItem("Olahraga", R.drawable.ic_mission, "ic_mission")
    )

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

        // Reset jam kalender minggu ke 00:00:00 agar bersih
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        for (i in 0..6) {
            weekDays.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Ambil tanggal hari ini tanpa mempedulikan jam
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        selectedDayIndex = weekDays.indexOfFirst {
            it.timeInMillis == today.timeInMillis
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
            dayName.setTextColor(Color.parseColor("#C7CBE0"))
            dayNumber.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            chip.setBackgroundResource(R.drawable.bg_day_chip)
            dayName.setTextColor(Color.parseColor("#9B9FA8"))
            dayNumber.setTextColor(Color.parseColor("#1A1C2E"))
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
                    val start = doc.getString("startTime") ?: doc.getString("start") ?: "--:--"
                    val end = doc.getString("endTime") ?: doc.getString("end") ?: "--:--"
                    val icon = doc.getString("icon") ?: "ic_jadwal"
                    val weight = doc.getString("weight") ?: "Ringan"
                    activityList.add(
                        ActivityItem(
                            id = doc.id,
                            name = name,
                            durationMinutes = duration,
                            source = source,
                            startTime = start,
                            endTime = end,
                            iconName = icon,
                            weight = weight
                        )
                    )
                }

                showEmptyState(activityList.isEmpty())
                adapter.updateData(activityList.toList())
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
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnClose = view.findViewById<ImageView>(R.id.btnCloseDialog)
        val etName = view.findViewById<EditText>(R.id.etNamaAktivitas)
        val tvStart = view.findViewById<TextView>(R.id.tvWaktuMulai)
        val tvEnd = view.findViewById<TextView>(R.id.tvWaktuSelesai)
        val tvKategori = view.findViewById<TextView>(R.id.tvKategori)
        val btnRingan = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnBebanRingan)
        val btnSedang = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnBebanSedang)
        val btnBerat = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnBebanBerat)
        val btnSimpan = view.findViewById<Button>(R.id.btnSimpanAktivitas)

        var sH = 9; var sM = 0
        var eH = 10; var eM = 0
        var tingkatBeban = "Ringan"
        var kategoriAktif = "Pekerjaan"
        var iconAktif = "ic_jadwal"
        var selectedResId = R.drawable.ic_jadwal

        tvKategori.text = kategoriAktif
        tvKategori.setCompoundDrawablesWithIntrinsicBounds(selectedResId, 0, 0, 0)
        tvKategori.compoundDrawablePadding = 16

        if (itemToEdit != null) {
            etName.setText(itemToEdit.name)
            tvStart.text = itemToEdit.startTime
            tvEnd.text = itemToEdit.endTime
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
        } else {
            tvStart.text = String.format(Locale.getDefault(), "%02d:%02d", sH, sM)
            tvEnd.text = String.format(Locale.getDefault(), "%02d:%02d", eH, eM)
        }

        btnClose?.setOnClickListener { dialog.dismiss() }

        tvStart.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                sH = h; sM = m
                tvStart.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }, sH, sM, true).show()
        }

        tvEnd.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                eH = h; eM = m
                tvEnd.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }, eH, eM, true).show()
        }

        tvKategori.setOnClickListener {
            showPilihKategoriDialog { nama, resId, namaRes ->
                kategoriAktif = nama
                iconAktif = namaRes
                selectedResId = resId
                tvKategori.text = nama
                tvKategori.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
            }
        }

        fun resetBeban() {
            btnRingan.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
            btnSedang.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
            btnBerat.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        btnRingan.setOnClickListener {
            resetBeban(); btnRingan.setCardBackgroundColor(Color.parseColor("#E8F9EE")); tingkatBeban = "Ringan"
        }
        btnSedang.setOnClickListener {
            resetBeban(); btnSedang.setCardBackgroundColor(Color.parseColor("#FFF9E6")); tingkatBeban = "Sedang"
        }
        btnBerat.setOnClickListener {
            resetBeban(); btnBerat.setCardBackgroundColor(Color.parseColor("#FEF2F2")); tingkatBeban = "Berat"
        }

        btnRingan.performClick()

        btnSimpan.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty() || tvStart.text.toString().isEmpty() || tvEnd.text.toString().isEmpty()) {
                Toast.makeText(this, "Semua data wajib diisi", Toast.LENGTH_SHORT).show()
            } else {
                saveManualActivity(itemToEdit?.id, name, sH, sM, eH, eM, tingkatBeban, kategoriAktif, iconAktif)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showPilihKategoriDialog(onSelected: (String, Int, String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pilih Kategori")

        val adapter = object : ArrayAdapter<KategoriItem>(this, android.R.layout.select_dialog_item, android.R.id.text1, daftarKategori) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val tv = view.findViewById<TextView>(android.R.id.text1)
                val item = getItem(position)
                tv.text = item?.nama
                tv.setCompoundDrawablesWithIntrinsicBounds(item?.iconResId ?: 0, 0, 0, 0)
                tv.compoundDrawablePadding = 24
                return view
            }
        }

        builder.setAdapter(adapter) { dialog, which ->
            val terpilih = daftarKategori[which]
            onSelected(terpilih.nama, terpilih.iconResId, terpilih.iconName)
        }

        builder.setPositiveButton("+ Kategori Baru") { _, _ ->
            showTambahKategoriBaruDialog(onSelected)
        }

        builder.show()
    }

    private fun showTambahKategoriBaruDialog(onSelected: (String, Int, String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Buat Kategori Baru")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        val etNama = EditText(this).apply { hint = "Nama kategori..." }
        layout.addView(etNama)

        val tvLabel = TextView(this).apply {
            text = "Pilih Ikon Vektor:"
            setPadding(0, 32, 0, 16)
        }
        layout.addView(tvLabel)

        val scrollView = HorizontalScrollView(this)
        val iconContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scrollView.addView(iconContainer)

        var selectedIcon = daftarIkonTersedia[0]
        val iconViews = mutableListOf<ImageView>()

        for (ikon in daftarIkonTersedia) {
            val iv = ImageView(this).apply {
                setImageResource(ikon.first)
                setPadding(24, 24, 24, 24)
                setBackgroundResource(android.R.drawable.list_selector_background)
                layoutParams = LinearLayout.LayoutParams(160, 160).apply {
                    setMargins(0, 0, 16, 0)
                }
            }
            iv.setOnClickListener {
                selectedIcon = ikon
                iconViews.forEach { it.alpha = 0.3f }
                iv.alpha = 1.0f
            }
            iconViews.add(iv)
            iconContainer.addView(iv)
        }
        iconViews.forEach { it.alpha = 0.3f }
        iconViews[0].alpha = 1.0f

        layout.addView(scrollView)
        builder.setView(layout)

        builder.setPositiveButton("Simpan") { _, _ ->
            val namaBaru = etNama.text.toString().trim()
            if (namaBaru.isNotEmpty()) {
                daftarKategori.add(KategoriItem(namaBaru, selectedIcon.first, selectedIcon.second))
                onSelected(namaBaru, selectedIcon.first, selectedIcon.second)
            } else {
                Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun saveManualActivity(docId: String?, name: String, sH: Int, sM: Int, eH: Int, eM: Int, weight: String, cat: String, icon: String) {
        val startTotalMin = sH * 60 + sM
        val endTotalMin = eH * 60 + eM
        var duration = endTotalMin - startTotalMin
        if (duration < 0) duration += 24 * 60
        if (duration == 0) {
            Toast.makeText(this, "Jam mulai dan selesai tidak boleh sama", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        // AMBIL TANGGAL DARI CHIP YANG AKTIF DAN RESET JAMNYA KE 00:00:00
        val selectedCal = (weekDays[selectedDayIndex].clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val selectedDate = selectedCal.time

        val activityData = hashMapOf(
            "userId" to userId,
            "name" to name,
            "durationMinutes" to duration,
            "startTime" to String.format(Locale.getDefault(), "%02d:%02d", sH, sM),
            "endTime" to String.format(Locale.getDefault(), "%02d:%02d", eH, eM),
            "date" to Timestamp(selectedDate),
            "source" to "manual",
            "cat" to cat,
            "weight" to weight,
            "icon" to icon
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