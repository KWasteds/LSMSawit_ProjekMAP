package com.example.lsmsawit_projekmap.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.model.Kebun
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class FormIsiDataKebun : BottomSheetDialogFragment() {

    private lateinit var etNama: EditText
    private lateinit var etId: EditText
    private lateinit var etLokasi: EditText
    private lateinit var btnAmbilLokasi: Button
    private lateinit var etLuas: EditText
    private lateinit var etTahun: EditText
    private lateinit var btnSimpan: Button
    private lateinit var progressSaving: ProgressBar
    private lateinit var btnHapus: Button

    private var isEditMode = false
    private var oldIdKebun: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        fun newInstance(idKebun: String, nama: String, lokasi: String?, luas: Double, tahun: Int): FormIsiDataKebun {
            val f = FormIsiDataKebun()
            val args = Bundle()
            args.putString("idKebun", idKebun)
            args.putString("nama", nama)
            args.putString("lokasi", lokasi)
            args.putDouble("luas", luas)
            args.putInt("tahun", tahun)
            f.arguments = args
            return f
        }

        private const val REQ_LOCATION = 1001
        private val ID_REGEX = Regex("^[A-Z0-9-]{5,30}$")
        private val LOC_REGEX = Regex("^\\s*-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?\\s*$")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.form_isidata_kebun, container, false)

        etNama = v.findViewById(R.id.etNamaKebun)
        etId = v.findViewById(R.id.etIdKebun)
        etLokasi = v.findViewById(R.id.etLokasi)
        btnAmbilLokasi = v.findViewById(R.id.btnAmbilLokasi)
        etLuas = v.findViewById(R.id.etLuas)
        etTahun = v.findViewById(R.id.etTahunTanam)
        btnSimpan = v.findViewById(R.id.btnSimpan)
        progressSaving = v.findViewById(R.id.progressSaving)
        btnHapus = v.findViewById(R.id.btnHapus)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Check if arguments exist -> edit mode
        arguments?.let { args ->
            val id = args.getString("idKebun")
            if (!id.isNullOrEmpty()) {
                isEditMode = true
                oldIdKebun = id
                etId.setText(id)
                etNama.setText(args.getString("nama"))
                etLokasi.setText(args.getString("lokasi") ?: "")
                etLuas.setText(args.getDouble("luas").toString())
                etTahun.setText(args.getInt("tahun").toString())
                btnHapus.visibility = View.VISIBLE
                btnSimpan.text = "Update"
            }
        }

        btnAmbilLokasi.setOnClickListener {
            ambilLokasi()
        }

        btnSimpan.setOnClickListener {
            attemptSave()
        }

        btnHapus.setOnClickListener {
            if (isEditMode && !oldIdKebun.isNullOrBlank()) {
                // konfirmasi hapus
                val dlg = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Kebun")
                    .setMessage("Yakin ingin menghapus kebun ini?")
                    .setNegativeButton("Batal", null)
                    .setPositiveButton("Hapus") { _, _ ->
                        deleteKebun(oldIdKebun!!)
                    }
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

        // Validasi
        if (nama.isEmpty()) {
            etNama.error = "Nama kebun wajib diisi"
            return
        }
        if (idK.isEmpty() || !ID_REGEX.matches(idK)) {
            etId.error = "ID kebun tidak valid (gunakan huruf besar, angka, '-' minimal 5 karakter)"
            return
        }
        if (luas == null) {
            etLuas.error = "Luas wajib diisi (angka)"
            return
        }
        if (tahun == null) {
            etTahun.error = "Tahun tanam wajib diisi"
            return
        }
        // lokasi longgar: jika ada harus sesuai pattern lat,lng (longgar kita terima - tanpa spasi ok)
        if (lokasiTxt.isNotEmpty() && !LOC_REGEX.matches(lokasiTxt)) {
            etLokasi.error = "Format lokasi harus lat,lng (contoh -6.2341,106.5567) atau kosong"
            return
        }

        // show progress
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
            "createdAt" to FieldValue.serverTimestamp()
        )

        // If edit and id changed -> delete old then set new
        if (isEditMode && !oldIdKebun.isNullOrBlank() && oldIdKebun != idK) {
            // delete old first, then create new
            db.collection("kebun").document(oldIdKebun!!).delete()
                .addOnSuccessListener {
                    db.collection("kebun").document(idK).set(dataMap)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Kebun diperbarui", Toast.LENGTH_SHORT).show()
                            setSavingState(false)
                            setFragmentResult("kebun_changed", bundleOf("changed" to true))
                            dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Gagal simpan: ${e.message}", Toast.LENGTH_LONG).show()
                            setSavingState(false)
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal hapus data lama: ${e.message}", Toast.LENGTH_LONG).show()
                    setSavingState(false)
                }
        } else {
            // create or update same id
            db.collection("kebun").document(idK).set(dataMap)
                .addOnSuccessListener {
                    val msg = if (isEditMode) "Kebun diperbarui" else "Kebun tersimpan"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    setSavingState(false)
                    setFragmentResult("kebun_changed", bundleOf("changed" to true))
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                    setSavingState(false)
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

    private fun setSavingState(isSaving: Boolean) {
        btnSimpan.isEnabled = !isSaving
        btnHapus.isEnabled = !isSaving
        progressSaving.visibility = if (isSaving) View.VISIBLE else View.GONE
        btnSimpan.text = if (isSaving) "Menyimpan..." else if (isEditMode) "Update" else "Simpan"
    }

    private fun ambilLokasi() {
        // check permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQ_LOCATION)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude

                    // PERBAIKAN: Gunakan Locale.US agar pemisah desimal selalu titik (.)
                    etLokasi.setText(String.format(Locale.US, "%.6f,%.6f", lat, lon))

                    Toast.makeText(requireContext(), "Lokasi berhasil diambil", Toast.LENGTH_SHORT).show()
                } else {
                    // ... kode lainnya ...
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal ambil lokasi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // hasil request permission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQ_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ambilLokasi()
            } else {
                Toast.makeText(requireContext(), "Izin lokasi ditolak. Anda bisa isi lokasi manual.", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
