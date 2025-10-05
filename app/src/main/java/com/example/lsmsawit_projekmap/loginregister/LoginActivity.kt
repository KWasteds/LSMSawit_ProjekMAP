package com.example.lsmsawit_projekmap.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lsmsawit_projekmap.MainActivity
import com.example.lsmsawit_projekmap.databinding.ActivityLoginBinding
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

        // Pindah ke Register
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
