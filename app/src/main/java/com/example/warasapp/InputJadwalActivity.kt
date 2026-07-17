package com.example.warasapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.Calendar

class InputJadwalActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedDateMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_jadwal)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etActivityName: EditText = findViewById(R.id.etActivityName)
        val etDuration: EditText = findViewById(R.id.etDuration)
        val btnPickDate: Button = findViewById(R.id.btnPickDate)
        val tvSelectedDate: TextView = findViewById(R.id.tvSelectedDate)
        val btnSaveActivity: Button = findViewById(R.id.btnSaveActivity)
        val tvLogout: TextView = findViewById(R.id.tvLogout)

        // Pilih tanggal
        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDateMillis = selectedCalendar.timeInMillis

                val dateText = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                tvSelectedDate.text = "Tanggal: $dateText"
            }, year, month, day).show()
        }

        // Simpan aktivitas ke Firestore
        btnSaveActivity.setOnClickListener {
            val activityName = etActivityName.text.toString().trim()
            val durationText = etDuration.text.toString().trim()

            if (activityName.isEmpty() || durationText.isEmpty()) {
                Toast.makeText(this, "Nama aktivitas dan durasi wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDateMillis == 0L) {
                Toast.makeText(this, "Pilih tanggal dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = durationText.toIntOrNull()
            if (duration == null || duration <= 0) {
                Toast.makeText(this, "Durasi harus berupa angka valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Sesi login habis, silakan login ulang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val activityData = hashMapOf(
                "userId" to userId,
                "name" to activityName,
                "durationMinutes" to duration,
                "date" to Timestamp(java.util.Date(selectedDateMillis)),
                "source" to "manual"
            )

            db.collection("activities")
                .add(activityData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Aktivitas berhasil disimpan", Toast.LENGTH_SHORT).show()
                    etActivityName.text.clear()
                    etDuration.text.clear()
                    tvSelectedDate.text = "Tanggal belum dipilih"
                    selectedDateMillis = 0L
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Logout
        tvLogout.setOnClickListener {
            auth.signOut()
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}