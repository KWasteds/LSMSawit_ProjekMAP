// AccountSettingActivity.kt
package com.example.lsmsawit_projekmap

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lsmsawit_projekmap.databinding.ActivityAccountSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AccountSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountSettingBinding
    private var selectedImageUri: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUserUid: String? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    Glide.with(this)
                        .load(uri)
                        .transform(CircleCrop())
                        .into(binding.profileImage)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUserUid = auth.currentUser?.uid

        if (currentUserUid == null) {
            Toast.makeText(this, "Pengguna tidak ditemukan, silakan login ulang.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadUserDataFromFirestore()

        binding.profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserDataFromFirestore() {
        currentUserUid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        binding.editName.setText(document.getString("name"))
                        binding.editEmail.setText(document.getString("email"))
                        binding.editCity.setText(document.getString("city"))
                        binding.editAddress.setText(document.getString("address"))
                        // **FIX: Pastikan menggunakan key "contact"**
                        binding.editPhone.setText(document.getString("contact")) // Asumsi ID di XML adalah editPhone, tapi data dari "contact"

                        val photoUrl = document.getString("photoUrl")
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(photoUrl)
                                .transform(CircleCrop())
                                .placeholder(R.mipmap.ic_launcher_round)
                                .into(binding.profileImage)
                        }
                    } else {
                        Toast.makeText(this, "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Gagal memuat data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveProfile() {
        val name = binding.editName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val city = binding.editCity.text.toString().trim()
        val address = binding.editAddress.text.toString().trim()
        // **FIX: Pastikan ID EditText di XML sesuai (misal: editPhone)**
        val contact = binding.editPhone.text.toString().trim()

        if (name.isBlank() || email.isBlank()) {
            Toast.makeText(this, "Nama dan Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        setSavingState(true)

        if (selectedImageUri != null) {
            uploadToCloudinaryAndSaveProfile(name, email, city, address, contact, selectedImageUri!!)
        } else {
            updateUserDataInFirestore(name, email, city, address, contact, null)
        }
    }

    private fun uploadToCloudinaryAndSaveProfile(name: String, email: String, city: String, address: String, contact: String, uri: Uri) {
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
        val uploadPreset = "fotokebun"
        val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

        val inputStream = contentResolver.openInputStream(uri)
        val imageBytes = inputStream?.readBytes()
        inputStream?.close()

        if (imageBytes == null) {
            Toast.makeText(this, "Gagal membaca file gambar", Toast.LENGTH_SHORT).show()
            setSavingState(false)
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "profile_${currentUserUid}.jpg", imageBytes.toRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder().url(uploadUrl).post(requestBody).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AccountSettingActivity, "Upload gambar gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    setSavingState(false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string() ?: ""
                Log.d("CloudinaryResponse", "Code: ${response.code}, Body: $responseText")

                if (response.isSuccessful) {
                    val imageUrl = JSONObject(responseText).getString("secure_url")
                    runOnUiThread {
                        updateUserDataInFirestore(name, email, city, address, contact, imageUrl)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@AccountSettingActivity, "Upload ke Cloudinary gagal", Toast.LENGTH_LONG).show()
                        setSavingState(false)
                    }
                }
            }
        })
    }

    private fun updateUserDataInFirestore(name: String, email: String, city: String, address: String, contact: String, newPhotoUrl: String?) {
        val userUpdates = mutableMapOf<String, Any?>( // Izinkan nilai null
            "name" to name,
            "email" to email,
            "city" to city,
            "address" to address,
            "contact" to contact // **FIX: Simpan sebagai "contact"**
        )

        // Hanya update photoUrl jika ada URL baru. Jika tidak, field ini tidak akan diubah.
        if (newPhotoUrl != null) {
            userUpdates["photoUrl"] = newPhotoUrl
        }

        currentUserUid?.let { uid ->
            db.collection("users").document(uid).update(userUpdates as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    setSavingState(false)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal memperbarui profil: ${e.message}", Toast.LENGTH_LONG).show()
                    setSavingState(false)
                }
        }
    }

    private fun setSavingState(isSaving: Boolean) {
        binding.btnSave.isEnabled = !isSaving
        binding.btnSave.text = if (isSaving) "Menyimpan..." else "SIMPAN PERUBAHAN"
    }
}