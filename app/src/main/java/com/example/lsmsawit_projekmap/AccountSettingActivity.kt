package com.example.lsmsawit_projekmap

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop

class AccountSettingActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPhone: EditText
    private lateinit var editAddress: EditText
    private lateinit var btnSave: Button

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_setting)

        // 🔧 Inisialisasi View
        profileImage = findViewById(R.id.profileImage)
        editName = findViewById(R.id.editName)
        editEmail = findViewById(R.id.editEmail)
        editPhone = findViewById(R.id.editPhone)
        editAddress = findViewById(R.id.editAddress)
        btnSave = findViewById(R.id.btnSave)

        // 🔹 Ambil data dari SharedPreferences
        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
        editName.setText(sharedPref.getString("name", "Android Studio"))
        editEmail.setText(sharedPref.getString("email", "android.studio@android.com"))

        val photoUri = sharedPref.getString("photoUri", null)
        if (photoUri != null) {
            val uri = Uri.parse(photoUri)
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(profileImage) // ✅ sudah benar sekarang
        }

        // 📷 Klik gambar untuk memilih foto
        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // 💾 Tombol Simpan
        btnSave.setOnClickListener {
            val name = editName.text.toString()
            val email = editEmail.text.toString()
            val phone = editPhone.text.toString()
            val address = editAddress.text.toString()

            // Simpan ke SharedPreferences
            val editor = sharedPref.edit()
            editor.putString("name", name)
            editor.putString("email", email)
            editor.putString("phone", phone)
            editor.putString("address", address)
            selectedImageUri?.let { uri ->
                editor.putString("photoUri", uri.toString())
            }
            editor.apply()

            // Kirim hasil ke MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("name", name)
            resultIntent.putExtra("email", email)
            resultIntent.putExtra("phone", phone)
            resultIntent.putExtra("address", address)
            setResult(Activity.RESULT_OK, resultIntent)

            Toast.makeText(this, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // 🔄 Hasil pemilihan gambar dari galeri
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            selectedImageUri = uri
            Glide.with(this)
                .load(uri)
                .transform(CircleCrop()) // 🔵 Gambar dibulatkan
                .into(profileImage)
        }
    }
}
