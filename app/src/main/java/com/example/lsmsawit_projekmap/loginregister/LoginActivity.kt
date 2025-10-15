package com.example.lsmsawit_projekmap.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lsmsawit_projekmap.MainActivity
import com.example.lsmsawit_projekmap.databinding.ActivityLoginBinding
import com.example.lsmsawit_projekmap.ui.admin.AdminActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Tombol Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            // Validasi
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etUsername.error = "Email tidak valid"
                return@setOnClickListener
            }
            if (pass.isEmpty() || pass.length < 6) {
                binding.etPassword.error = "Password minimal 6 karakter"
                return@setOnClickListener
            }

            loginUser(email, pass)
        }

        // ✅ Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etUsername.error = "Masukkan email yang valid terlebih dahulu"
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Link reset password dikirim ke email.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal mengirim email: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Pindah ke Register
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val uid = currentUser.uid

                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {

                                val status = document.getString("status") ?: "aktif"
                                if (status.equals("nonaktif", ignoreCase = true)) {
                                    // ✅ Logout lagi & tampilkan pesan
                                    auth.signOut()
                                    Toast.makeText(
                                        this,
                                        "Akun Anda nonaktif. Silakan hubungi admin.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@addOnSuccessListener
                                }

                                // ✅ Kalau status aktif → lanjutkan ke role masing-masing
                                val role = document.getString("role") ?: ""
                                when (role.lowercase()) {
                                    "adminwilayah" -> {
                                        startActivity(Intent(this, AdminActivity::class.java))
                                    }
                                    "adminpusat" -> {
                                        startActivity(
                                            Intent(
                                                this,
                                                com.example.lsmsawit_projekmap.ui.adminlsm.AdminLSMActivity::class.java
                                            )
                                        )
                                    }
                                    else -> {
                                        startActivity(Intent(this, MainActivity::class.java))
                                    }
                                }
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Data user tidak ditemukan di Firestore",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Gagal ambil data user: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
