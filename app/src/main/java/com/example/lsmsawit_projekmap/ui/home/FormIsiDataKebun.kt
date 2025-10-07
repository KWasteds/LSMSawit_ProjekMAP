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

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Edit mode
    private var isEditMode = false
    private var oldIdKebun: String? = null

    // Image
    private lateinit var imageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: android.net.Uri? = null

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Launcher untuk pilih gambar dari galeri
        imageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedImageUri = result.data!!.data
                imagePreview.setImageURI(selectedImageUri)
                Toast.makeText(requireContext(), "Gambar berhasil dimuat", Toast.LENGTH_SHORT).show()
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

        return v
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

        val dataMap = hashMapOf<String, Any?>(
            "userId" to uid,
            "idKebun" to idK,
            "namaKebun" to nama,
            "lokasi" to if (lokasiTxt.isNotEmpty()) lokasiTxt else null,
            "luas" to luas,
            "tahunTanam" to tahun,
            "createdAt" to FieldValue.serverTimestamp(),
            "imageUri" to (selectedImageUri?.toString() ?: "")
        )

        db.collection("kebun").document(idK)
            .set(dataMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), if (isEditMode) "Kebun diperbarui" else "Kebun tersimpan", Toast.LENGTH_SHORT).show()
                setSavingState(false)

                // Tambahan: kirim sinyal bahwa data kebun berubah
                setFragmentResult("kebun_changed", bundleOf("changed" to true))

                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                setSavingState(false)
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
}
