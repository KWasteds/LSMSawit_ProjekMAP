package com.example.lsmsawit_projekmap.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lsmsawit_projekmap.MainActivity
import com.example.lsmsawit_projekmap.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()

            // Validasi
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
            if (!binding.cbTerms.isChecked) {
                Toast.makeText(this, "Silakan setuju syarat & ketentuan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(name, email, pass)
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser(name: String, email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener

                val user = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to "petani" // Default role
                )

                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Registrasi berhasil! (Firestore OK)", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }


                // Pindah ke LoginActivity TANPA tergantung Firestore
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Registrasi gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
