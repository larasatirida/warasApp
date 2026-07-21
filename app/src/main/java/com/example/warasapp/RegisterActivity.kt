package com.example.warasapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etNamaLengkap: EditText = findViewById(R.id.etNamaLengkap)
        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val etConfirmPassword: EditText = findViewById(R.id.etConfirmPassword)
        val btnRegister: Button = findViewById(R.id.btnRegister)
        val tvGoToLogin: TextView = findViewById(R.id.tvGoToLogin)
        val tvBack: TextView = findViewById(R.id.tvBack)

        btnRegister.setOnClickListener {
            val nama = etNamaLengkap.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (nama.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    
                    // 1. Update Display Name di Firebase Auth
                    val profileUpdates = userProfileChangeRequest {
                        displayName = nama
                    }
                    user?.updateProfile(profileUpdates)

                    // 2. Simpan data lengkap ke Firestore
                    val userData = hashMapOf(
                        "uid" to user?.uid,
                        "fullName" to nama,
                        "email" to email,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    
                    FirebaseFirestore.getInstance().collection("users")
                        .document(user?.uid ?: "")
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            // Tetap finish meskipun firestore gagal, yang penting auth berhasil
                            Toast.makeText(this, "Simpan data gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Registrasi gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }

        tvBack.setOnClickListener {
            finish()
        }
    }
}
