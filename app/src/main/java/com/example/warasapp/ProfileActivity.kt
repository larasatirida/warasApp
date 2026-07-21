package com.example.warasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

    private fun setupUserInfo() {
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        
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
