package com.example.lsmsawit_projekmap.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.example.lsmsawit_projekmap.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import android.net.Uri
import java.io.IOException
import android.util.Log
import java.io.File
import android.os.Environment
import androidx.core.content.FileProvider
import android.database.Cursor
import java.text.SimpleDateFormat
import java.util.Date
import android.provider.MediaStore

class FormIsiDataKebun : BottomSheetDialogFragment() {

    // View
    private lateinit var etNama: EditText
    private lateinit var etId: EditText
    private lateinit var etLokasi: EditText
    private lateinit var btnAmbilLokasi: Button
    private lateinit var etLuas: EditText
    private lateinit var etTahun: EditText
    private lateinit var btnSimpan: Button
    private lateinit var progressSaving: ProgressBar
    private lateinit var btnHapus: Button
    private lateinit var btnInsertImage: Button
    private lateinit var imagePreview: ImageView
    private lateinit var btnTakePhoto: Button
    private lateinit var tvTimestamp: TextView

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Edit mode
    private var isEditMode = false
    private var oldIdKebun: String? = null

    // Image
    private var photoUri: Uri? = null
    private lateinit var imageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: android.net.Uri? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private val cameraPermission = arrayOf(Manifest.permission.CAMERA)
    private var photoTimestamp: String? = null



    private fun createImageUri(): Uri? {
        val context = requireContext()
        val image = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "kebun_photo_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            image
        )
    }

    companion object {
        private const val REQ_LOCATION = 1001
        private val ID_REGEX = Regex("^[A-Z0-9-]{5,30}$")
        private val LOC_REGEX = Regex("^\\s*-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?\\s*$")

        // Fungsi instance baru dengan parameter
        fun newInstance(
            idKebun: String?,
            namaKebun: String?,
            lokasi: String?,
            luas: Double?,
            tahunTanam: Int?
        ): FormIsiDataKebun {
            val fragment = FormIsiDataKebun()
            val args = Bundle()
            args.putString("idKebun", idKebun)
            args.putString("namaKebun", namaKebun)
            args.putString("lokasi", lokasi)
            if (luas != null) args.putDouble("luas", luas)
            if (tahunTanam != null) args.putInt("tahunTanam", tahunTanam)
            fragment.arguments = args
            return fragment
        }
    }

    private fun launchCamera() {
        val newPhotoUri = createImageUri()
        // Lakukan pengecekan null di sini
        if (newPhotoUri != null) {
            photoUri = newPhotoUri
            cameraLauncher.launch(newPhotoUri)
        } else {
            Toast.makeText(requireContext(), "Gagal membuat file gambar", Toast.LENGTH_SHORT).show()
        }
    }
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Jika user memberi izin, baru buka kamera
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.form_isidata_kebun, container, false)

        // Inisialisasi view
        etNama = v.findViewById(R.id.etNamaKebun)
        etId = v.findViewById(R.id.etIdKebun)
        etLokasi = v.findViewById(R.id.etLokasi)
        btnAmbilLokasi = v.findViewById(R.id.btnAmbilLokasi)
        etLuas = v.findViewById(R.id.etLuas)
        etTahun = v.findViewById(R.id.etTahunTanam)
        btnSimpan = v.findViewById(R.id.btnSimpan)
        progressSaving = v.findViewById(R.id.progressSaving)
        btnHapus = v.findViewById(R.id.btnHapus)
        btnInsertImage = v.findViewById(R.id.btnInsertImage)
        imagePreview = v.findViewById(R.id.imgPreview)
        btnTakePhoto = v.findViewById(R.id.btnTakePhoto)
        tvTimestamp = v.findViewById(R.id.tvTimestamp)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Launcher untuk pilih gambar dari galeri
        imageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedImageUri = result.data!!.data
                imagePreview.setImageURI(selectedImageUri)

                selectedImageUri?.let { uri ->
                    getTimestampFromUri(uri)
                }
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                photoUri?.let { uri ->
                    imagePreview.setImageURI(uri)
                    selectedImageUri = uri

                    val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.getDefault())
                    val timestamp = sdf.format(Date())
                    photoTimestamp = timestamp
                    tvTimestamp.text = "Waktu foto: $timestamp"
                }
            }
        }

        btnInsertImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imageLauncher.launch(intent)
        }

        // Mode edit jika ada argumen
        arguments?.let { args ->
            val id = args.getString("idKebun")
            if (!id.isNullOrEmpty()) {
                isEditMode = true
                oldIdKebun = id
                etId.setText(id)
                etNama.setText(args.getString("namaKebun"))
                etLokasi.setText(args.getString("lokasi") ?: "")
                etLuas.setText(args.getDouble("luas").toString())
                etTahun.setText(args.getInt("tahunTanam").toString())
                btnHapus.visibility = View.VISIBLE
                btnSimpan.text = "Update"
            }
        }

        btnAmbilLokasi.setOnClickListener { ambilLokasi() }
        btnSimpan.setOnClickListener { attemptSave() }

        btnHapus.setOnClickListener {
            if (isEditMode && !oldIdKebun.isNullOrBlank()) {
                val dlg = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Kebun")
                    .setMessage("Yakin ingin menghapus kebun ini?")
                    .setNegativeButton("Batal", null)
                    .setPositiveButton("Hapus") { _, _ -> deleteKebun(oldIdKebun!!) }
                    .create()
                dlg.show()
            }
        }

        btnTakePhoto = v.findViewById(R.id.btnTakePhoto)

        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Sudah diizinkan → langsung buka kamera
                launchCamera()
            } else {
                // Minta izin dulu
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }


        return v
    }

    private fun getTimestampFromUri(uri: Uri) {
        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)
        val cursor: Cursor? = requireContext().contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val dateTakenIndex = it.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                if (dateTakenIndex != -1) {
                    val dateTakenMillis = it.getLong(dateTakenIndex)
                    val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.getDefault())
                    val timestamp = sdf.format(Date(dateTakenMillis))
                    photoTimestamp = timestamp
                    tvTimestamp.text = "Waktu foto: $timestamp"
                    return
                }
            }
        }
        // Jika gagal mendapatkan dari metadata, gunakan waktu saat ini
        val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())
        photoTimestamp = timestamp
        tvTimestamp.text = "Waktu foto: $timestamp"
    }

    private fun attemptSave() {
        val nama = etNama.text.toString().trim()
        val idK = etId.text.toString().trim()
        val lokasiTxt = etLokasi.text.toString().trim()
        val luas = etLuas.text.toString().toDoubleOrNull()
        val tahun = etTahun.text.toString().toIntOrNull()

        if (nama.isEmpty()) {
            etNama.error = "Nama kebun wajib diisi"; return
        }
        if (idK.isEmpty() || !ID_REGEX.matches(idK)) {
            etId.error = "ID kebun tidak valid"; return
        }
        if (luas == null) {
            etLuas.error = "Luas wajib diisi (angka)"; return
        }
        if (tahun == null) {
            etTahun.error = "Tahun tanam wajib diisi"; return
        }
        if (lokasiTxt.isNotEmpty() && !LOC_REGEX.matches(lokasiTxt)) {
            etLokasi.error = "Format lokasi salah (contoh: -6.2341,106.5567)"; return
        }

        setSavingState(true)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Harap login dahulu", Toast.LENGTH_SHORT).show()
            setSavingState(false)
            return
        }

        // 🔹 Jika user memilih gambar, upload ke Cloudinary dulu
        if (selectedImageUri != null) {
            uploadToCloudinaryAndSave(uid, idK, nama, lokasiTxt, luas, tahun, selectedImageUri!!)
        } else {
            // 🔹 Jika tidak ada gambar, langsung simpan data Firestore dengan imageUrl kosong
            saveKebunData(uid, idK, nama, lokasiTxt, luas, tahun, "")
        }
    }

    private fun deleteKebun(id: String) {
        setSavingState(true)
        db.collection("kebun").document(id).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Kebun dihapus", Toast.LENGTH_SHORT).show()
                setSavingState(false)

                // Tambahan: kirim sinyal penghapusan juga
                setFragmentResult("kebun_changed", bundleOf("changed" to true))

                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal hapus: ${e.message}", Toast.LENGTH_LONG).show()
                setSavingState(false)
            }
    }

    private fun setSavingState(isSaving: Boolean) {
        btnSimpan.isEnabled = !isSaving
        btnHapus.isEnabled = !isSaving
        btnInsertImage.isEnabled = !isSaving
        progressSaving.visibility = if (isSaving) View.VISIBLE else View.GONE
        btnSimpan.text = if (isSaving) "Menyimpan..." else if (isEditMode) "Update" else "Simpan"
    }

    private fun ambilLokasi() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQ_LOCATION)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    etLokasi.setText(String.format(Locale.US, "%.6f,%.6f", lat, lon))
                    Toast.makeText(requireContext(), "Lokasi berhasil diambil", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Tidak dapat menemukan lokasi", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal ambil lokasi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQ_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ambilLokasi()
            } else {
                Toast.makeText(requireContext(), "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun uploadImageAndSave(
        uid: String,
        idK: String,
        nama: String,
        lokasiTxt: String?,
        luas: Double,
        tahun: Int,
        uri: android.net.Uri
    ) {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("kebun_images/$idK.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Setelah upload sukses, simpan data Firestore dengan URL download
                    saveKebunData(uid, idK, nama, lokasiTxt, luas, tahun, downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload gambar gagal: ${e.message}", Toast.LENGTH_LONG).show()
                setSavingState(false)
            }
    }

    private fun saveKebunData(
        uid: String,
        idK: String,
        nama: String,
        lokasiTxt: String?,
        luas: Double,
        tahun: Int,
        imageUrl: String
    ) {
        val dataMap = hashMapOf(
            "userId" to uid,
            "idKebun" to idK,
            "namaKebun" to nama,
            "lokasi" to (lokasiTxt ?: ""),
            "luas" to luas,
            "tahunTanam" to tahun,
            "createdAt" to FieldValue.serverTimestamp(),
            "imageUri" to imageUrl,
            "fotoTimestamp" to photoTimestamp
        )

        db.collection("kebun").document(idK)
            .set(dataMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), if (isEditMode) "Kebun diperbarui" else "Kebun tersimpan", Toast.LENGTH_SHORT).show()
                setSavingState(false)
                setFragmentResult("kebun_changed", bundleOf("changed" to true))
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                setSavingState(false)
            }
    }

    private fun uploadToCloudinaryAndSave(
        uid: String,
        idK: String,
        nama: String,
        lokasiTxt: String?,
        luas: Double,
        tahun: Int,
        uri: Uri
    ) {
        val cloudName = "dw5jofoyu"        // ganti sesuai Cloudinary kamu
        val uploadPreset = "fotokebun"     // ganti sesuai Cloudinary kamu
        val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val imageBytes = inputStream?.readBytes()
        inputStream?.close()

        if (imageBytes == null) {
            Toast.makeText(requireContext(), "Gagal membaca gambar", Toast.LENGTH_SHORT).show()
            setSavingState(false)
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "kebun_$idK.jpg",
                RequestBody.create("image/*".toMediaTypeOrNull(), imageBytes))
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Upload gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    setSavingState(false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string() ?: ""
                Log.e("CloudinaryUpload", "Response code: ${response.code}, body: $responseText")

                if (response.isSuccessful) {
                    val json = JSONObject(responseText)
                    val imageUrl = json.getString("secure_url")

                    requireActivity().runOnUiThread {
                        saveKebunData(uid, idK, nama, lokasiTxt, luas, tahun, imageUrl)
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(),
                            "Upload ke Cloudinary gagal (${response.code})",
                            Toast.LENGTH_LONG
                        ).show()
                        setSavingState(false)
                    }
                }
            }
        })
    }


}
