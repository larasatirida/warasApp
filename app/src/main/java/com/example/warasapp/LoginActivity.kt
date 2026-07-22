package com.example.warasapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Menangkap hasil dari layar pilih akun Google
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                Toast.makeText(this, "Gagal mendapatkan token Google", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        requestNotificationPermission()
        if (!hasAskedBatteryOptimizationBefore()) {
            requestIgnoreBatteryOptimization()
        }


        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val tvForgotPassword: TextView = findViewById(R.id.tvForgotPassword)
        val tvGoToRegister: TextView = findViewById(R.id.tvGoToRegister)
        val btnGoogleLogin: Button = findViewById(R.id.btnGoogleLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Login gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Masukkan email dulu di kolom email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Link reset password sudah dikirim ke $email, cek inbox/spam ya", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal kirim reset password: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                
                // Simpan/Update data user ke Firestore agar nama muncul di Dashboard
                val userData = hashMapOf(
                    "uid" to user?.uid,
                    "fullName" to user?.displayName,
                    "email" to user?.email,
                    "lastLogin" to com.google.firebase.Timestamp.now()
                )

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user?.uid ?: "")
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnCompleteListener {
                        Toast.makeText(this, "Login dengan Google berhasil", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Login dengan Google gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestIgnoreBatteryOptimization() {
        if (isIgnoringBatteryOptimizations()) return

        AlertDialog.Builder(this)
            .setTitle("Izinkan pengingat tepat waktu")
            .setMessage("Supaya notifikasi check-in muncul tepat waktu meskipun app tidak dibuka, aktifkan opsi 'Unrestricted' battery di halaman berikutnya.")
            .setCancelable(false)
            .setPositiveButton("Aktifkan") { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                markAskedBatteryOptimization()
            }
            .setNegativeButton("Nanti saja") { _, _ ->
                markAskedBatteryOptimization()
            }
            .show()
    }

    private fun hasAskedBatteryOptimizationBefore(): Boolean {
        val prefs = getSharedPreferences("app_onetime_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("has_asked_battery_optimization", false)
    }

    private fun markAskedBatteryOptimization() {
        val prefs = getSharedPreferences("app_onetime_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_asked_battery_optimization", true).apply()
    }

}