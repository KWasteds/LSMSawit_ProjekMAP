package com.example.lsmsawit_projekmap.ui.adminlsm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.lsmsawit_projekmap.MapsActivity
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.model.Kebun
import com.example.lsmsawit_projekmap.model.KebunAdminViewData
import com.example.lsmsawit_projekmap.model.Notifikasi
import com.example.lsmsawit_projekmap.ui.admin.VerifikasiDialogListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.google.firebase.firestore.Query

class AdminLSMHomeFragment : Fragment(), VerifikasiDialogListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: KebunLSMAdapter
    private var fullDataList = listOf<KebunAdminViewData>()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_admin_lsm_home, container, false)

        recyclerView = v.findViewById(R.id.recyclerViewLSM)
        layoutEmpty = v.findViewById(R.id.layoutEmptyLSM)
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshLSM)

        swipeRefreshLayout.setOnRefreshListener { loadKebunForVerification() }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = KebunLSMAdapter(
            mutableListOf(),
            onItemClick = { kebun ->
                // 🎯 **PANGGIL DIALOG BARU DI SINI** 🎯
                val dlg = VerifikasiDialogFragmentLSM.newInstance(kebun.idKebun, kebun.namaKebun, kebun.userId)
                dlg.setTargetFragment(this, 0) // Pastikan dialog mengirim hasil kembali ke fragment ini
                dlg.show(parentFragmentManager, "verifikasiDialogLSM")
            },
            onLocationClick = { kebun ->
                val intent = Intent(requireContext(), MapsActivity::class.java).apply {
                    putExtra("lokasi", kebun.lokasi)
                    putExtra("namaKebun", kebun.namaKebun)
                }
                startActivity(intent)
            },
            onDownloadClick = { kebun ->
                if (kebun.status != "Diterima") {
                    Toast.makeText(requireContext(), "Hanya kebun dengan status 'Diterima' yang bisa diunduh.", Toast.LENGTH_SHORT).show()
                    return@KebunLSMAdapter
                }
                generateKebunPdf(kebun)
            }
        )
        recyclerView.adapter = adapter
        loadKebunForVerification()
        return v
    }

    fun filterList(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullDataList
        } else {
            fullDataList.filter { viewData ->
                val kebunNameMatch = viewData.kebun.namaKebun.contains(query, ignoreCase = true)
                val ownerNameMatch = viewData.namaPemilik.contains(query, ignoreCase = true)

                val displayedStatus = if (viewData.kebun.status.equals("Verifikasi1", ignoreCase = true)) "Pending" else viewData.kebun.status
                val statusMatch = displayedStatus.contains(query, ignoreCase = true) || viewData.kebun.status.contains(query, ignoreCase = true)

                kebunNameMatch || ownerNameMatch || statusMatch // Tambahkan pencarian status
            }
        }
        adapter.updateList(filteredList)
        if (filteredList.isEmpty()) showEmpty() else showList()
    }

    override fun onVerificationResult(idKebun: String, ownerUserId: String, namaKebun: String, newStatus: String, note: String?) {
        Log.d("AdminLSMHome", "Menerima hasil: status=$newStatus, idKebun=$idKebun")
        performVerificationUpdate(idKebun, ownerUserId, namaKebun, newStatus, note)
    }

    private fun performVerificationUpdate(
        idKebun: String,
        ownerUserId: String,
        kebunName: String,
        newStatus: String,
        note: String?
    ) {
        val adminUid = auth.currentUser?.uid ?: return

        val batch = db.batch()

        // Update kebun
        val kebunRef = db.collection("kebun").document(idKebun)
        val kebunUpdates = hashMapOf<String, Any>(
            "status" to newStatus,
            "verifierLsmId" to adminUid,
            "verifiedLsmAt" to FieldValue.serverTimestamp()
        )
        if (!note.isNullOrBlank()) kebunUpdates["verificationNote"] = note
        batch.update(kebunRef, kebunUpdates)

        // 🔔 Buat Notifikasi untuk Petani
        val notifRef = db.collection("notifications").document()
        val message = when (newStatus) {
            "Diterima" -> "Selamat! Pengajuan kebun '$kebunName' Anda telah diterima oleh admin pusat."
            "Revisi" -> "Pengajuan kebun '$kebunName' Anda memerlukan revisi dari admin pusat."
            else -> "Status kebun '$kebunName' Anda telah diperbarui."
        }
        val newNotification = Notifikasi(
            id = notifRef.id,
            userId = ownerUserId,
            kebunId = idKebun,
            kebunName = kebunName,
            message = message,
            note = note,
            adminId = adminUid,
            read = false
        )
        batch.set(notifRef, newNotification)

        // Commit
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Verifikasi berhasil disimpan", Toast.LENGTH_SHORT).show()
                loadKebunForVerification()
            }
            .addOnFailureListener { e ->
                Log.e("AdminLSMHome", "Gagal melakukan batch write", e)
                Toast.makeText(requireContext(), "Gagal menyimpan verifikasi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun loadKebunForVerification() {
        swipeRefreshLayout.isRefreshing = true
        db.collection("kebun")
            .whereIn("status", listOf("Verifikasi1", "Diterima"))
            .get()
            .addOnSuccessListener { kebunSnap ->
                var kebunList = kebunSnap.documents.mapNotNull { d ->
                    d.toObject(Kebun::class.java)?.copy(idKebun = d.id)
                }

                // Urutkan agar Verifikasi1 di atas
                kebunList = kebunList.sortedWith(
                    compareBy<Kebun> {
                        when (it.status) {
                            "Verifikasi1" -> 0
                            "Diterima" -> 1
                            else -> 2   // jaga-jaga kalau ada status lain
                        }
                    }
                )

                if (kebunList.isEmpty()) {
                    showEmpty()
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    fetchUserNamesAndCombine(kebunList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminLSMHome", "Gagal memuat data kebun", e)
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
                showEmpty()
            }
    }


    private fun fetchUserNamesAndCombine(kebunList: List<Kebun>) {
        val userIds = kebunList.map { it.userId }.distinct().filter { it.isNotEmpty() }
        if (userIds.isEmpty()) {
            val combinedList = kebunList.map { KebunAdminViewData(it, "Nama tidak ditemukan") }
            updateList(combinedList)
            swipeRefreshLayout.isRefreshing = false
            return
        }
        db.collection("users")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), userIds).get()
            .addOnSuccessListener { usersSnap ->
                val userNameMap = usersSnap.documents.associate { doc ->
                    doc.id to (doc.getString("name") ?: "Tanpa Nama")
                }
                val combinedList = kebunList.map { kebun ->
                    KebunAdminViewData(kebun, userNameMap[kebun.userId] ?: "Tidak Ditemukan")
                }
                updateList(combinedList)
            }
            .addOnFailureListener {
                val combinedList = kebunList.map { KebunAdminViewData(it, "Gagal Memuat Nama") }
                updateList(combinedList)
            }
            .addOnCompleteListener {
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun generateKebunPdf(kebun: Kebun) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "Laporan_Kebun_${kebun.idKebun}.pdf"
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val filePath = java.io.File(downloadsDir, fileName).absolutePath

                val document = com.itextpdf.text.Document()
                val writer = com.itextpdf.text.pdf.PdfWriter.getInstance(
                    document,
                    java.io.FileOutputStream(filePath)
                )
                document.open()

                val titleFont = com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA,
                    20f,
                    com.itextpdf.text.Font.BOLD
                )
                val fieldFont = com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA,
                    12f
                )

                document.add(com.itextpdf.text.Paragraph("Laporan Kebun ${kebun.namaKebun}", titleFont))
                document.add(com.itextpdf.text.Paragraph(" "))
                document.add(
                    com.itextpdf.text.Paragraph(
                        "Tanggal Cetak: ${
                            java.text.SimpleDateFormat(
                                "dd MMM yyyy, HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date())
                        }", fieldFont
                    )
                )

                fun addField(label: String, value: String?) {
                    document.add(com.itextpdf.text.Paragraph("$label: ${value ?: "-"}", fieldFont))
                }

                // 🔹 Muat gambar di background (IO Thread)
                try {
                    val imageUrl = kebun.imageUri
                    if (!imageUrl.isNullOrEmpty()) {
                        val bitmap = com.bumptech.glide.Glide.with(requireContext())
                            .asBitmap()
                            .load(imageUrl)
                            .submit()
                            .get() // sekarang ini berjalan di IO thread

                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                        val image = com.itextpdf.text.Image.getInstance(stream.toByteArray())

                        image.scaleToFit(450f, 450f)
                        image.alignment = com.itextpdf.text.Element.ALIGN_CENTER
                        document.add(image)
                        document.add(
                            com.itextpdf.text.Paragraph(
                                "Gambar Kebun",
                                com.itextpdf.text.Font(
                                    com.itextpdf.text.Font.FontFamily.HELVETICA,
                                    14f,
                                    com.itextpdf.text.Font.BOLD
                                )
                            )
                        )
                        document.add(com.itextpdf.text.Paragraph(" "))
                    }
                } catch (e: Exception) {
                    Log.e("PDF", "Gagal menambah gambar: ${e.message}")
                    document.add(
                        com.itextpdf.text.Paragraph("Gagal memuat gambar: ${e.message}", fieldFont)
                    )
                }

                // 🔹 Tambahkan data kebun
                addField("Link Gambar", kebun.imageUri)
                document.add(com.itextpdf.text.Paragraph(" "))
                addField("ID Kebun", kebun.idKebun)
                addField("Nama Kebun", kebun.namaKebun)
                addField("Luas", "${kebun.luas ?: "-"} ha")
                addField("Lokasi", kebun.lokasi)
                addField("Tahun Tanam", kebun.tahunTanam?.toString())
                addField("Status", kebun.status)
                addField("User ID (Pemilik)", kebun.userId)
                document.add(com.itextpdf.text.Paragraph(" "))
                document.add(com.itextpdf.text.Paragraph(" "))

                document.close()
                writer.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "PDF berhasil disimpan di folder Download:\n$fileName",
                        Toast.LENGTH_LONG
                    ).show()

                    // 🔹 Buka PDF langsung
                    val pdfFile = java.io.File(filePath)
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().packageName + ".provider",
                        pdfFile
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Tidak ada aplikasi PDF viewer.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateList(list: List<KebunAdminViewData>) {
        fullDataList = list
        adapter.updateList(fullDataList)
        if (fullDataList.isEmpty()) showEmpty() else showList()
    }

    private fun showEmpty() {
        layoutEmpty.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showList() {
        layoutEmpty.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}