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

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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
                Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()

                if (email.equals("tangerangsawit@gmail.com", ignoreCase = true)) {
                    // 🔹 Jika email admin, buka halaman admin
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                } else {
                    // 🔹 Jika bukan admin, buka halaman user biasa
                    val intent = Intent(this, com.example.lsmsawit_projekmap.MainActivity::class.java)
                    startActivity(intent)
                }

                finish() // Supaya tidak bisa kembali ke login dengan tombol Back
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
