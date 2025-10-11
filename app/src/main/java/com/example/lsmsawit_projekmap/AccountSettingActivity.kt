package com.example.lsmsawit_projekmap

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lsmsawit_projekmap.databinding.ActivityAccountSettingBinding
import java.io.File
import java.io.FileOutputStream

class AccountSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountSettingBinding
    private var selectedImageUri: Uri? = null

    // Launcher untuk memilih gambar dari galeri
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    selectedImageUri = uri
                    // Tampilkan gambar yang baru dipilih
                    Glide.with(this)
                        .load(uri)
                        .transform(CircleCrop())
                        .into(binding.profileImage) // ID disesuaikan dengan XML
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Gunakan ViewBinding yang dihasilkan dari activity_account_setting.xml
        binding = ActivityAccountSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Muat data yang sudah ada dari SharedPreferences saat activity dibuka
        loadUserData()

        // Listener untuk memilih gambar baru
        binding.profileImage.setOnClickListener { // ID disesuaikan dengan XML
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Listener untuk tombol simpan
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    /**
     * Memuat semua data pengguna dari SharedPreferences dan menampilkannya di EditText/ImageView
     */
    private fun loadUserData() {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")
        val email = sharedPref.getString("email", "")
        val city = sharedPref.getString("city", "")
        val address = sharedPref.getString("address", "")
        val phone = sharedPref.getString("phone", "")
        val photoUriString = sharedPref.getString("photoUri", null)

        binding.editName.setText(name) // ID disesuaikan dengan XML
        binding.editEmail.setText(email) // ID disesuaikan dengan XML
        binding.editCity.setText(city)
        binding.editAddress.setText(address)
        binding.editPhone.setText(phone)

        if (photoUriString != null) {
            val photoUri = Uri.parse(photoUriString)
            Glide.with(this)
                .load(photoUri)
                .transform(CircleCrop())
                .placeholder(R.mipmap.ic_launcher_round) // Gambar default
                .into(binding.profileImage) // ID disesuaikan dengan XML
        }
    }

    /**
     * Menyimpan semua data dari EditText ke SharedPreferences dan
     * mengirim kembali data yang diperlukan (nama, email, foto) ke MainActivity.
     */
    private fun saveProfile() {
        val name = binding.editName.text.toString()
        val email = binding.editEmail.text.toString()
        val city = binding.editCity.text.toString()
        val address = binding.editAddress.text.toString()
        val phone = binding.editPhone.text.toString()

        if (name.isBlank() || email.isBlank()) {
            Toast.makeText(this, "Nama dan Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Siapkan SharedPreferences untuk menyimpan data
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()

        sharedPref.putString("name", name)
        sharedPref.putString("email", email)
        sharedPref.putString("city", city)
        sharedPref.putString("address", address)
        sharedPref.putString("phone", phone)

        val resultIntent = Intent()
        resultIntent.putExtra("name", name)
        resultIntent.putExtra("email", email)

        // Jika ada gambar baru yang dipilih, simpan secara lokal dan dapatkan URI barunya
        if (selectedImageUri != null) {
            val localImageUri = saveImageToInternalStorage(selectedImageUri!!)
            if (localImageUri != null) {
                val uriString = localImageUri.toString()
                resultIntent.putExtra("photoUri", uriString)
                sharedPref.putString("photoUri", uriString)
            }
        } else {
            // Jika tidak ada gambar baru, kirim kembali URI yang lama
            val oldPhotoUri = getSharedPreferences("UserData", Context.MODE_PRIVATE).getString("photoUri", null)
            resultIntent.putExtra("photoUri", oldPhotoUri)
        }

        sharedPref.apply() // Terapkan semua perubahan ke SharedPreferences

        setResult(Activity.RESULT_OK, resultIntent)
        Toast.makeText(this, "Profil berhasil disimpan", Toast.LENGTH_SHORT).show()
        finish() // Kembali ke MainActivity
    }

    /**
     * Menyalin file gambar dari URI galeri ke penyimpanan internal aplikasi
     * dan mengembalikan URI file lokal yang baru.
     */
    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "profile_image.jpg") // Nama file yang konsisten
            val outputStream = FileOutputStream(file)

            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            return Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
            return null
        }
    }
}