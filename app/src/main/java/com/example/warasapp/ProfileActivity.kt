package com.example.warasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.warasapp.logic.getProfileStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        BottomNavHelper.setup(this, "profil")

        setupUserInfo()
        loadStats()
        setupLogout()
    }

    private fun showEditNameDialog(tvProfileName: TextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ubah Nama Akun")

        // Membuat kontainer untuk EditText agar marginnya rapi
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(60, 20, 60, 0)

        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT
        input.setText(tvProfileName.text) // Mengisi otomatis dengan nama saat ini
        input.setSelection(input.text.length) // Taruh kursor di akhir teks

        container.addView(input)
        builder.setView(container)

        // Tombol Simpan
        builder.setPositiveButton("Simpan") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                // Panggil fungsi untuk menyimpan ke database
                updateNameToDatabase(newName, tvProfileName)
            } else {
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol Batal
        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun updateNameToDatabase(newName: String, tvProfileName: TextView) {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Update "fullName" dan "name" sekaligus agar konsisten saat dibaca ulang
            val updates = hashMapOf<String, Any>(
                "fullName" to newName,
                "name" to newName
            )

            db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener {
                    // Jika berhasil di database, update juga UI di layar
                    tvProfileName.text = newName
                    Toast.makeText(this, "Nama berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal mengupdate nama: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Anda belum login", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUserInfo() {
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val btnEditNama = findViewById<LinearLayout>(R.id.btnEditNama)

        val user = auth.currentUser
        tvEmail.text = user?.email ?: "email@kamu.com"

        // Ambil nama dari Firestore
        user?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("fullName") ?: doc.getString("name") ?: user.displayName ?: "Pengguna"
                    tvName.text = name
                }
                .addOnFailureListener {
                    tvName.text = user.displayName ?: "Pengguna"
                }
        }

        // Aksi saat area nama atau ikon pensil diklik
        btnEditNama.setOnClickListener {
            showEditNameDialog(tvName)
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val stats = getProfileStats()
                findViewById<TextView>(R.id.tvStatHariAktif).text = stats.hariAktif.toString()
                findViewById<TextView>(R.id.tvStatMissionOk).text = stats.missionOkCount.toString()
                findViewById<TextView>(R.id.tvStatAvgBurnout).text = stats.avgBurnout.toString()
                findViewById<TextView>(R.id.tvStatXpTotal).text = stats.totalXp.toString()
                findViewById<TextView>(R.id.tvProfileLevelInfo).text = "Level ${stats.level} • ${stats.levelTitle}"
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Gagal memuat statistik", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLogout() {
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Keluar Akun")
                .setMessage("Apakah kamu yakin ingin keluar?")
                .setPositiveButton("Keluar") { _, _ ->
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}