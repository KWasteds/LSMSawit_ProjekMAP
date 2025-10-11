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

        profileImage = findViewById(R.id.profileImage)
        editName = findViewById(R.id.editName)
        editEmail = findViewById(R.id.editEmail)
        editPhone = findViewById(R.id.editPhone)
        editAddress = findViewById(R.id.editAddress)
        btnSave = findViewById(R.id.btnSave)

        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
        editName.setText(sharedPref.getString("name", "Android Studio"))
        editEmail.setText(sharedPref.getString("email", "android.studio@android.com"))

        val photoUri = sharedPref.getString("photoUri", null)
        if (photoUri != null) {
            Glide.with(this)
                .load(Uri.parse(photoUri))
                .circleCrop()
                .into(profileImage)
        }

        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString()
            val email = editEmail.text.toString()
            val phone = editPhone.text.toString()
            val address = editAddress.text.toString()

            val editor = sharedPref.edit()
            editor.putString("name", name)
            editor.putString("email", email)
            editor.putString("phone", phone)
            editor.putString("address", address)
            selectedImageUri?.let { uri ->
                editor.putString("photoUri", uri.toString())
            }
            editor.apply()

            val resultIntent = Intent()
            resultIntent.putExtra("name", name)
            resultIntent.putExtra("email", email)
            resultIntent.putExtra("phone", phone)
            resultIntent.putExtra("address", address)
            if (selectedImageUri != null) {
                resultIntent.putExtra("photoUri", selectedImageUri.toString())
            } else if (photoUri != null) {
                resultIntent.putExtra("photoUri", photoUri)
            }
            setResult(Activity.RESULT_OK, resultIntent)

            Toast.makeText(this, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            selectedImageUri = uri
            Glide.with(this)
                .load(uri)
                .transform(CircleCrop())
                .into(profileImage)
        }
    }
}
