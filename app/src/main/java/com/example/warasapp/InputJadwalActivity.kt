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

    // ================== KATEGORI (persisten ke Firestore) ==================

    private data class KategoriItem(
        val id: String = "",       // docId Firestore. Kosong = belum tersimpan.
        val nama: String,
        val iconResId: Int,
        val iconName: String
    )

    private val daftarIkonTersedia = listOf(
        Pair(R.drawable.ic_jadwal, "ic_jadwal"),
        Pair(R.drawable.ic_mission, "ic_mission"),
        Pair(R.drawable.ic_profile, "ic_profile"),
        Pair(R.drawable.ic_check, "ic_check"),
        Pair(R.drawable.ic_warning, "ic_warning"),
        Pair(R.drawable.ic_sleepy, "ic_sleepy")
    )

    // Mulai kosong -> diisi dari Firestore lewat loadKategoriFromFirestore()
    private val daftarKategori = mutableListOf<KategoriItem>()

    private val categoriesCollection get() = db.collection("categories")
    private var kategoriSudahDimuat = false

    // ==========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_jadwal)
        BottomNavHelper.setup(this, "jadwal")

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Muat kategori lebih dulu supaya siap dipakai saat dialog dibuka
        loadKategoriFromFirestore()

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

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        for (i in 0..6) {
            weekDays.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

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
                // Kirim salinan list, bukan referensi langsung -> mencegah adapter.updateData()
                // ikut mengosongkan activityList kalau di dalamnya ada items.clear()
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

            // Isi form dengan kategori & beban yang tersimpan sebelumnya,
            // bukan selalu reset ke default "Pekerjaan" / "Ringan"
            iconAktif = itemToEdit.iconName
            selectedResId = daftarIkonTersedia.firstOrNull { it.second == itemToEdit.iconName }?.first
                ?: R.drawable.ic_jadwal
            kategoriAktif = daftarKategori.firstOrNull { it.iconName == itemToEdit.iconName }?.nama
                ?: kategoriAktif
            tingkatBeban = itemToEdit.weight

            tvKategori.text = kategoriAktif
            tvKategori.setCompoundDrawablesWithIntrinsicBounds(selectedResId, 0, 0, 0)
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

        // Highlight kartu beban sesuai data (default "Ringan" kalau tambah baru)
        resetBeban()
        when (tingkatBeban) {
            "Sedang" -> btnSedang.setCardBackgroundColor(Color.parseColor("#FFF9E6"))
            "Berat" -> btnBerat.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
            else -> btnRingan.setCardBackgroundColor(Color.parseColor("#E8F9EE"))
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

    // ================== KATEGORI: dialog pilih, tambah, edit, hapus ==================

    private fun showPilihKategoriDialog(onSelected: (String, Int, String) -> Unit) {
        if (!kategoriSudahDimuat && daftarKategori.isEmpty()) {
            Toast.makeText(this, "Kategori masih dimuat, coba lagi sebentar", Toast.LENGTH_SHORT).show()
            loadKategoriFromFirestore()
            return
        }

        val dialog = AlertDialog.Builder(this).create()

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 32, 40, 24)
        }

        val tvTitle = TextView(this).apply {
            text = "Pilih Kategori"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(8, 0, 0, 24)
        }
        rootLayout.addView(tvTitle)

        val rowContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootLayout.addView(rowContainer)

        fun renderRows() {
            rowContainer.removeAllViews()

            daftarKategori.forEachIndexed { index, kategori ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(8, 24, 8, 24)
                    isClickable = true
                    isFocusable = true
                    setBackgroundResource(android.R.drawable.list_selector_background)
                }

                val icon = ImageView(this).apply {
                    setImageResource(kategori.iconResId)
                    layoutParams = LinearLayout.LayoutParams(72, 72).apply {
                        marginEnd = 28
                    }
                }
                row.addView(icon)

                val tvNama = TextView(this).apply {
                    text = kategori.nama
                    textSize = 15f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(tvNama)

                val btnEdit = TextView(this).apply {
                    text = "✏️"
                    textSize = 16f
                    setPadding(20, 12, 20, 12)
                    setOnClickListener {
                        showKategoriFormDialog(existing = kategori) { nama, resId, iconName ->
                            updateKategoriInFirestore(kategori.id, nama, resId, iconName) { updated ->
                                daftarKategori[index] = updated
                                renderRows()
                            }
                        }
                    }
                }
                row.addView(btnEdit)

                val btnDelete = TextView(this).apply {
                    text = "🗑️"
                    textSize = 16f
                    setPadding(20, 12, 20, 12)
                    setOnClickListener {
                        if (daftarKategori.size <= 1) {
                            Toast.makeText(this@InputJadwalActivity, "Minimal harus ada 1 kategori", Toast.LENGTH_SHORT).show()
                        } else {
                            AlertDialog.Builder(this@InputJadwalActivity)
                                .setTitle("Hapus Kategori")
                                .setMessage("Yakin ingin menghapus kategori \"${kategori.nama}\"?")
                                .setPositiveButton("Hapus") { _, _ ->
                                    deleteKategoriFromFirestore(kategori.id) {
                                        daftarKategori.removeAt(index)
                                        renderRows()
                                    }
                                }
                                .setNegativeButton("Batal", null)
                                .show()
                        }
                    }
                }
                row.addView(btnDelete)

                row.setOnClickListener {
                    onSelected(kategori.nama, kategori.iconResId, kategori.iconName)
                    dialog.dismiss()
                }

                rowContainer.addView(row)

                rowContainer.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                    setBackgroundColor(Color.parseColor("#F0F0F0"))
                })
            }
        }

        renderRows()

        val btnTambah = TextView(this).apply {
            text = "+ Kategori Baru"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#5B8DEF"))
            setPadding(8, 32, 8, 8)
            setOnClickListener {
                showKategoriFormDialog(existing = null) { nama, resId, iconName ->
                    saveNewKategoriToFirestore(nama, resId, iconName) { baru ->
                        daftarKategori.add(baru)
                        renderRows()
                    }
                }
            }
        }
        rootLayout.addView(btnTambah)

        dialog.setView(rootLayout)
        dialog.show()
    }

    /**
     * Form tambah ATAU edit kategori, tergantung parameter `existing`.
     * existing == null -> mode tambah baru
     * existing != null -> mode edit (form terisi otomatis dari data lama)
     *
     * Callback mengirim data mentah (nama, resId, iconName), BUKAN KategoriItem final,
     * karena pemanggil (add/edit) yang menentukan kapan Firestore call dijalankan dan
     * baru membuat KategoriItem setelah docId dari server didapat.
     */
    private fun showKategoriFormDialog(existing: KategoriItem?, onSaved: (nama: String, resId: Int, iconName: String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (existing == null) "Buat Kategori Baru" else "Edit Kategori")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        val etNama = EditText(this).apply {
            hint = "Nama kategori..."
            setText(existing?.nama ?: "")
        }
        layout.addView(etNama)

        val tvLabel = TextView(this).apply {
            text = "Pilih Ikon Vektor:"
            setPadding(0, 32, 0, 16)
        }
        layout.addView(tvLabel)

        val scrollView = HorizontalScrollView(this)
        val iconContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scrollView.addView(iconContainer)

        var selectedIcon = existing?.let { ex ->
            daftarIkonTersedia.firstOrNull { it.second == ex.iconName }
        } ?: daftarIkonTersedia[0]

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
        val activeIndex = daftarIkonTersedia.indexOfFirst { it.second == selectedIcon.second }
        iconViews.getOrNull(if (activeIndex >= 0) activeIndex else 0)?.alpha = 1.0f

        layout.addView(scrollView)
        builder.setView(layout)

        builder.setPositiveButton("Simpan") { _, _ ->
            val nama = etNama.text.toString().trim()
            if (nama.isNotEmpty()) {
                onSaved(nama, selectedIcon.first, selectedIcon.second)
            } else {
                Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    // ================== KATEGORI: fungsi Firestore (CRUD) ==================

    private fun loadKategoriFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        categoriesCollection
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                daftarKategori.clear()

                if (snapshot.isEmpty) {
                    // User baru -> seed kategori default ke Firestore satu kali
                    seedDefaultCategories(userId)
                } else {
                    for (doc in snapshot.documents) {
                        val nama = doc.getString("nama") ?: continue
                        val iconName = doc.getString("iconName") ?: "ic_jadwal"
                        val iconResId = daftarIkonTersedia.firstOrNull { it.second == iconName }?.first
                            ?: R.drawable.ic_jadwal
                        daftarKategori.add(KategoriItem(doc.id, nama, iconResId, iconName))
                    }
                    kategoriSudahDimuat = true
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat kategori: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun seedDefaultCategories(userId: String) {
        val defaults = listOf(
            Triple("Pekerjaan", R.drawable.ic_jadwal, "ic_jadwal"),
            Triple("Istirahat", R.drawable.ic_sleepy, "ic_sleepy"),
            Triple("Olahraga", R.drawable.ic_mission, "ic_mission")
        )

        defaults.forEach { (nama, resId, iconName) ->
            val data = hashMapOf(
                "userId" to userId,
                "nama" to nama,
                "iconName" to iconName,
                "timestamp" to Timestamp.now()
            )
            categoriesCollection.add(data)
                .addOnSuccessListener { docRef ->
                    daftarKategori.add(KategoriItem(docRef.id, nama, resId, iconName))
                    kategoriSudahDimuat = true
                }
        }
    }

    private fun saveNewKategoriToFirestore(nama: String, iconResId: Int, iconName: String, onSaved: (KategoriItem) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "userId" to userId,
            "nama" to nama,
            "iconName" to iconName,
            "timestamp" to Timestamp.now()
        )
        categoriesCollection.add(data)
            .addOnSuccessListener { docRef ->
                onSaved(KategoriItem(docRef.id, nama, iconResId, iconName))
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan kategori: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateKategoriInFirestore(docId: String, nama: String, iconResId: Int, iconName: String, onUpdated: (KategoriItem) -> Unit) {
        if (docId.isEmpty()) return
        categoriesCollection.document(docId)
            .update(mapOf("nama" to nama, "iconName" to iconName))
            .addOnSuccessListener {
                onUpdated(KategoriItem(docId, nama, iconResId, iconName))
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengubah kategori: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteKategoriFromFirestore(docId: String, onDeleted: () -> Unit) {
        if (docId.isEmpty()) return
        categoriesCollection.document(docId).delete()
            .addOnSuccessListener { onDeleted() }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus kategori: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ================== AKTIVITAS: simpan & hapus ==================

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