package com.example.lsmsawit_projekmap.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lsmsawit_projekmap.MainActivity
import com.example.lsmsawit_projekmap.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            // Validasi sederhana
            if (name.isEmpty()) {
                binding.etName.error = "Nama wajib diisi"
                return@setOnClickListener
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Email tidak valid"
                return@setOnClickListener
            }
            if (pass.length < 6) {
                binding.etPassword.error = "Password minimal 6 karakter"
                return@setOnClickListener
            }
            if (pass != confirm) {
                binding.etConfirmPassword.error = "Password konfirmasi tidak sama"
                return@setOnClickListener
            }
            if (!binding.cbTerms.isChecked) {
                Toast.makeText(this, "Silakan setuju syarat & ketentuan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Contoh penyimpanan ringan: SharedPreferences (JANGAN simpan password plaintext di aplikasi produksi)
            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("user_name", name)
                .putString("user_email", email)
                .putBoolean("is_logged_in", true)
                .apply()

            // Lanjut ke MainActivity (atau ke halaman lain)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
