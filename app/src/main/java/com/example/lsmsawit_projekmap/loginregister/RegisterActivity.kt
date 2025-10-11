// RegisterActivity.kt
package com.example.lsmsawit_projekmap.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lsmsawit_projekmap.R
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
            val address = binding.etAddress.text.toString().trim()
            val contact = binding.etContact.text.toString().trim() // Menggunakan `etContact` dari XML Anda
            val city = binding.etCity.text.toString().trim()

            if (name.isEmpty()) { binding.etName.error = "Nama wajib diisi"; return@setOnClickListener }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEmail.error = "Email tidak valid"; return@setOnClickListener }
            if (address.isEmpty()) { binding.etAddress.error = "Alamat wajib diisi"; return@setOnClickListener }
            if (contact.isEmpty()) { binding.etContact.error = "Nomor kontak wajib diisi"; return@setOnClickListener }
            if (pass.length < 6) { binding.etPassword.error = "Password minimal 6 karakter"; return@setOnClickListener }
            if (!binding.cbTerms.isChecked) { Toast.makeText(this, "Silakan setuju syarat & ketentuan", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            registerUser(name, email, pass, address, contact, city)
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val cities = resources.getStringArray(R.array.indonesia_cities)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
        binding.etCity.setAdapter(adapter)
        binding.etCity.threshold = 1
    }

    private fun registerUser(name: String, email: String, pass: String, address: String, contact: String, city: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener

                val user = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "address" to address,
                    "contact" to contact, // Sesuaikan key dengan model data
                    "city" to city,
                    "role" to "petani",
                    "photoUrl" to null // Tambahkan field photoUrl dengan nilai awal null
                )

                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Registrasi gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}