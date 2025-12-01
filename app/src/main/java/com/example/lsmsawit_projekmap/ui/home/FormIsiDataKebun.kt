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
import com.example.lsmsawit_projekmap.ui.home.ViewModel.KebunFormViewModel
import androidx.fragment.app.activityViewModels
import androidx.core.widget.addTextChangedListener
import com.google.firebase.storage.FirebaseStorage
import android.content.DialogInterface // Import untuk onDismiss
import android.content.Context
import android.net.ConnectivityManager
import com.example.lsmsawit_projekmap.model.PendingKebun
import com.example.lsmsawit_projekmap.model.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

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
    private var photoTimestamp: String? = null

    private val formVM: KebunFormViewModel by activityViewModels()

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

        fun newInstance(
            idKebun: String?,
            namaKebun: String?,
            lokasi: String?,
            luas: Double?,
            tahunTanam: Int?,
            status: String?
        ): FormIsiDataKebun {
            val fragment = FormIsiDataKebun()
            val args = Bundle().apply {
                putString("idKebun", idKebun)
                putString("namaKebun", namaKebun)
                putString("lokasi", lokasi)
                luas?.let { putDouble("luas", it) }
                tahunTanam?.let { putInt("tahunTanam", it) }
                putString("status", status)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private fun launchCamera() {
        val newPhotoUri = createImageUri()
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
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.form_isidata_kebun, container, false)

        // 1. Inisialisasi View
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

        // 2. Tentukan Mode Edit dan Inisialisasi/Pemulihan Data ke ViewModel
        arguments?.let { args ->
            val id = args.getString("idKebun")
            if (!id.isNullOrEmpty()) {
                isEditMode = true
                oldIdKebun = id

                // Kunci: Hanya muat data Arguments ke ViewModel JIKA ID di VM masih null.
                // Jika tidak null, berarti data sudah dimuat saat rotasi.
                if (formVM.idKebun.value == null) {
                    formVM.idKebun.value = id
                    formVM.namaKebun.value = args.getString("namaKebun")
                    formVM.lokasi.value = args.getString("lokasi") ?: ""
                    formVM.luas.value = args.getDouble("luas").toString()
                    formVM.tahunTanam.value = args.getInt("tahunTanam").toString()
                    // Image dan Timestamp TIDAK perlu dimuat dari Arguments di sini,
                    // karena Arguments tidak membawa data image/timestamp draft.
                }

                btnHapus.visibility = View.VISIBLE
                btnSimpan.text = "Update"
            }

            // Atur status editable
            val status = args.getString("status")
            val isEditable = when (status?.lowercase()) {
                "pending", "revisi" -> true
                else -> false
            }

            if (isEditMode && !isEditable) {
                disableForm()
            }
        }

        // 3. Isi View dari ViewModel (Data yang dipertahankan saat rotasi)
        // Gunakan operator Elvis (?: "") untuk menangani String? dari LiveData
        etId.setText(formVM.idKebun.value ?: "")
        etNama.setText(formVM.namaKebun.value ?: "")
        etLokasi.setText(formVM.lokasi.value ?: "")
        etLuas.setText(formVM.luas.value ?: "")
        etTahun.setText(formVM.tahunTanam.value ?: "")

        formVM.selectedImageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                // 1. Pulihkan Image Preview
                imagePreview.setImageURI(uri)

                // 2. Sinkronkan variabel Fragment
                selectedImageUri = uri

                // Catatan: Timestamp akan diperbarui oleh observer LiveData Timestamp di bawah,
                // tetapi jika Anda tidak punya observer timestamp, lakukan di sini:
                // tvTimestamp.text = formVM.photoTimestamp.value ?: "..."
            } else {
                // Reset View jika URI null (ketika form baru dibuka)
                imagePreview.setImageDrawable(null)
                // Pastikan timestamp juga direset jika gambar dihapus/disetel null
                tvTimestamp.text = "Tanggal & Waktu Foto: Tidak Tersedia"
                selectedImageUri = null
            }
        }

        // 🔑 LOKASI KODE BARU: Observer untuk Timestamp
        formVM.photoTimestamp.observe(viewLifecycleOwner) { ts ->
            if (ts.isNullOrBlank()) {
                tvTimestamp.text = "Tanggal & Waktu Foto: Tidak Tersedia"
                photoTimestamp = null
            } else {
                tvTimestamp.text = "Waktu foto: $ts"
                // Sinkronkan variabel Fragment (penting untuk kode Fragment lainnya)
                photoTimestamp = ts
            }
        }

        // 4. Binding listener (Simpan perubahan ke VM segera)
        etNama.addTextChangedListener { formVM.namaKebun.value = it.toString() }
        etId.addTextChangedListener { formVM.idKebun.value = it.toString() }
        etLokasi.addTextChangedListener { formVM.lokasi.value = it.toString() }
        etLuas.addTextChangedListener { formVM.luas.value = it.toString() }
        etTahun.addTextChangedListener { formVM.tahunTanam.value = it.toString() }

        // 5. Launcher Image
        imageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Di dalam imageLauncher result handler
                val newUri = result.data!!.data

                // 1. Ambil SEMUA flags izin yang diberikan oleh Content Provider
                val receivedFlags = result.data!!.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                // 2. Tentukan flags yang AKAN dipertahankan (Read dan Persistable)
                val flagsToPersist = Intent.FLAG_GRANT_READ_URI_PERMISSION

                newUri?.let { uri ->
                    imagePreview.setImageURI(uri)
                    getTimestampFromUri(uri)

                    selectedImageUri = uri

                    // 🔑 KUNCI PERBAIKAN: Hanya panggil takePersistableUriPermission jika URI memiliki izin yang valid.
                    // Kita hanya mencoba mempertahankan izin BACA.
                    if (receivedFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION == Intent.FLAG_GRANT_READ_URI_PERMISSION) {
                        try {
                            val contentResolver = requireContext().contentResolver

                            // Panggil takePersistableUriPermission hanya dengan flag READ yang Anda terima
                            contentResolver.takePersistableUriPermission(uri, flagsToPersist)
                            Log.d("URI_PERMISSION", "Persistable READ permission taken successfully.")
                        } catch (e: Exception) {
                            // Tangani pengecualian (termasuk IllegalArgumentException)
                            Log.e("URI_PERMISSION", "Failed to take persistable URI permission: ${e.message}")
                            Toast.makeText(requireContext(), "Error Izin Persisten: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Jika bahkan izin Baca dasar tidak diterima, notifikasi
                        Toast.makeText(requireContext(), "URI tidak memiliki izin baca yang diperlukan.", Toast.LENGTH_LONG).show()
                    }

                    formVM.setImage(uri, photoTimestamp)
                }
            }
        }

        // 6. Launcher Kamera
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                photoUri?.let { uri ->
                    imagePreview.setImageURI(uri)
                    // photoUri adalah URI yang digunakan saat capture

                    // PERBAIKAN: Tetapkan selectedImageUri Fragment
                    selectedImageUri = uri

                    getTimestampFromUri(uri) // Ini akan mengatur photoTimestamp Fragment
                    formVM.setImage(uri, photoTimestamp) // Update VM (menggunakan photoTimestamp Fragment yang baru)
                }
            }
        }

        // 7. Listener Tombol
        btnInsertImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            imageLauncher.launch(intent)
        }
        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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

        return v
    }

    // 🗑️ Tambahkan pembersihan ViewModel saat Fragment ditutup
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Reset ViewModel. Ini memastikan saat form dibuka kembali, ia kosong (tidak ada draft).
        formVM.clearData()
    }


    /**
     * Mengambil timestamp dari metadata gambar (jika ada) atau waktu saat ini.
     * Mengatur `photoTimestamp` dan `tvTimestamp`.
     */
    private fun getTimestampFromUri(uri: Uri) {
        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)
        val cursor: Cursor? = try {
            requireContext().contentResolver.query(uri, projection, null, null, null)
        } catch (e: Exception) {
            Log.e("GetTimestamp", "Error querying MediaStore: ${e.message}")
            null
        }

        var timestamp: String
        val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.getDefault())

        cursor?.use {
            if (it.moveToFirst()) {
                val dateTakenIndex = it.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                if (dateTakenIndex != -1) {
                    val dateTakenMillis = it.getLong(dateTakenIndex)
                    timestamp = sdf.format(Date(dateTakenMillis))
                    photoTimestamp = timestamp
                    tvTimestamp.text = "Waktu foto: $timestamp"
                    return // Selesai jika berhasil dari metadata
                }
            }
        }

        // Jika gagal mendapatkan dari metadata, gunakan waktu saat ini
        timestamp = sdf.format(Date())
        photoTimestamp = timestamp
        tvTimestamp.text = "Waktu foto: $timestamp (waktu capture)"
    }

    private fun attemptSave() {
        // Ambil data dari ViewModel
        val nama = formVM.namaKebun.value?.trim() ?: ""
        val idK = formVM.idKebun.value?.trim() ?: ""
        val lokasiTxt = formVM.lokasi.value?.trim()
        val luas = formVM.luas.value?.toDoubleOrNull()
        val tahun = formVM.tahunTanam.value?.toIntOrNull()
        val connectivity = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isOnline = connectivity.activeNetworkInfo?.isConnectedOrConnecting == true

        if (nama.isEmpty()) { etNama.error = "Nama kebun wajib diisi"; return }
        if (idK.isEmpty() || !ID_REGEX.matches(idK)) { etId.error = "ID kebun tidak valid"; return }
        if (luas == null) { etLuas.error = "Luas wajib diisi (angka)"; return }
        if (tahun == null) { etTahun.error = "Tahun tanam wajib diisi"; return }
        if (!lokasiTxt.isNullOrEmpty() && !LOC_REGEX.matches(lokasiTxt)) { etLokasi.error = "Format lokasi salah (contoh: -6.2341,106.5567)"; return }

        if (!isOnline) {
            saveOfflineDraft()
            return
        }

        setSavingState(true)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Harap login dahulu", Toast.LENGTH_SHORT).show()
            setSavingState(false)
            return
        }

        val currentImageUri = formVM.selectedImageUri.value
        val currentPhotoTimestamp = formVM.photoTimestamp.value

        this.photoTimestamp = currentPhotoTimestamp

        if (currentImageUri != null) {
            uploadToCloudinaryAndSave(uid, idK, nama, lokasiTxt, luas, tahun, currentImageUri)
        } else {
            saveKebunData(uid, idK, nama, lokasiTxt, luas, tahun, "")
        }

        // kalau online → lanjut ke proses normal
        // saveOnline()
    }

    private fun saveOfflineDraft() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Harap login dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil data dari ViewModel, berikan default jika null/empty
        val kebunId = formVM.idKebun.value?.trim().takeIf { !it.isNullOrEmpty() } ?: run {
            Toast.makeText(requireContext(), "ID kebun wajib diisi sebelum menyimpan draft", Toast.LENGTH_SHORT).show()
            return
        }

        val nama = formVM.namaKebun.value?.trim() ?: ""
        val lokasi = formVM.lokasi.value?.trim() ?: ""
        val luas = formVM.luas.value?.toDoubleOrNull() ?: 0.0
        val tahun = formVM.tahunTanam.value?.toIntOrNull() ?: 0
        val imageUriStr = formVM.selectedImageUri.value?.toString()

        val pending = PendingKebun(
            action = if (isEditMode) "update" else "create",
            kebunId = kebunId,
            nama = nama,
            lokasi = lokasi,
            luas = luas,
            tahunTanam = tahun,
            imageUri = imageUriStr,
            userId = uid,
            timestamp = System.currentTimeMillis()
        )

        val dao = AppDatabase.getDatabase(requireContext()).pendingKebunDao()

        // gunakan lifecycleScope supaya coroutine di-cancel saat fragment dihancurkan
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                dao.insertPending(pending)
                // kembali ke main thread untuk toast/dismiss
                launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Disimpan offline. Akan dikirim saat online.", Toast.LENGTH_LONG).show()
                    dismiss()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal menyimpan draft offline: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun deleteKebun(id: String) {
        setSavingState(true)
        db.collection("kebun").document(id).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Kebun dihapus", Toast.LENGTH_SHORT).show()
                setSavingState(false)
                setFragmentResult("kebun_changed", bundleOf("changed" to true))
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal hapus: ${e.message}", Toast.LENGTH_LONG).show()
                setSavingState(false)
            }
    }

    private fun disableForm() {
        etNama.isEnabled = false
        etId.isEnabled = false
        etLokasi.isEnabled = false
        etLuas.isEnabled = false
        etTahun.isEnabled = false
        btnAmbilLokasi.isEnabled = false
        btnInsertImage.isEnabled = false
        btnTakePhoto.isEnabled = false
        btnSimpan.visibility = View.GONE
        btnHapus.visibility = View.GONE
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
                    val lokasiStr = String.format(Locale.US, "%.6f,%.6f", lat, lon)
                    etLokasi.setText(lokasiStr)
                    formVM.lokasi.value = lokasiStr // Simpan ke VM
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

    private fun saveKebunData(
        uid: String,
        idK: String,
        nama: String,
        lokasiTxt: String?,
        luas: Double,
        tahun: Int,
        imageUrl: String
    ) {
        val dataMap = hashMapOf<String, Any?>(
            "userId" to uid,
            "idKebun" to idK,
            "namaKebun" to nama,
            "lokasi" to (lokasiTxt ?: ""),
            "luas" to luas,
            "tahunTanam" to tahun,
            "createdAt" to FieldValue.serverTimestamp(),
            "imageUri" to imageUrl,
            "fotoTimestamp" to photoTimestamp,
            "status" to "pending",
            "verifierId" to "",
            "verifiedAt" to null,
            "verifierLsmId" to "",
            "verifiedLsmAt" to null
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
        val cloudName = "dw5jofoyu"
        val uploadPreset = "fotokebun"
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