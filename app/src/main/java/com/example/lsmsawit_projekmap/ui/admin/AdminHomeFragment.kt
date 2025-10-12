package com.example.lsmsawit_projekmap.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.lsmsawit_projekmap.MapsActivity
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.model.Kebun
import com.example.lsmsawit_projekmap.model.KebunAdminViewData
import com.example.lsmsawit_projekmap.model.Notifikasi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch

class AdminHomeFragment : Fragment(), VerifikasiDialogListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: KebunAdminAdapter
    private var fullDataList = listOf<KebunAdminViewData>()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_admin_home, container, false)

        recyclerView = v.findViewById(R.id.recyclerViewAdmin)
        layoutEmpty = v.findViewById(R.id.layoutEmptyAdmin)
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshAdmin)

        swipeRefreshLayout.setOnRefreshListener { loadKebunForAdmin() }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = KebunAdminAdapter(
            mutableListOf(),
            onItemClick = { kebun ->
                val dlg = VerifikasiDialogFragment.newInstance(kebun.idKebun, kebun.namaKebun, kebun.userId)
                dlg.show(childFragmentManager, "verifikasiDialog")
            },
            onLocationClick = { kebun ->
                val intent = Intent(requireContext(), MapsActivity::class.java).apply {
                    putExtra("lokasi", kebun.lokasi)
                    putExtra("namaKebun", kebun.namaKebun)
                }
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter
        loadKebunForAdmin()
        return v
    }

    fun filterList(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullDataList
        } else {
            fullDataList.filter { viewData ->
                val kebunNameMatch = viewData.kebun.namaKebun.contains(query, ignoreCase = true)
                val ownerNameMatch = viewData.namaPemilik.contains(query, ignoreCase = true)
                kebunNameMatch || ownerNameMatch
            }
        }
        adapter.updateList(filteredList)
        if (filteredList.isEmpty()) showEmpty() else showList()
    }

    override fun onVerificationResult(idKebun: String, ownerUserId: String, namaKebun: String, newStatus: String, note: String?) {
        Log.d("AdminHome", "Menerima hasil via Interface: status=$newStatus, idKebun=$idKebun")
        performVerificationUpdate(idKebun, ownerUserId, namaKebun, newStatus, note)
    }

    private fun performVerificationUpdate(idKebun: String, ownerUserId: String, kebunName: String, newStatus: String, note: String?) {
        val adminUid = auth.currentUser?.uid
        if (adminUid == null) {
            Toast.makeText(requireContext(), "Gagal mendapatkan ID Admin", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("AdminHome", "Menjalankan WriteBatch untuk kebun: $idKebun dengan status: $newStatus")
        val batch = db.batch()

        // Operasi 1: Update Dokumen Kebun
        val kebunRef = db.collection("kebun").document(idKebun)
        val kebunUpdates = hashMapOf<String, Any>(
            "status" to newStatus,
            "verifierId" to adminUid,
            "verifiedAt" to FieldValue.serverTimestamp()
        )
        if (!note.isNullOrBlank()) kebunUpdates["verificationNote"] = note
        batch.update(kebunRef, kebunUpdates)

        // Operasi 2: Buat Dokumen Notifikasi Baru
        val notifRef = db.collection("notifications").document()
        val message = when (newStatus) {
            "Verifikasi1" -> "Selamat! Pengajuan kebun '$kebunName' Anda telah diverifikasi tahap 1."
            "Rejected" -> "Mohon maaf, pengajuan kebun '$kebunName' Anda ditolak."
            "Revisi" -> "Pengajuan kebun '$kebunName' Anda memerlukan revisi."
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
            isRead = false
        )
        batch.set(notifRef, newNotification)

        // Eksekusi Batch
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Verifikasi berhasil disimpan", Toast.LENGTH_SHORT).show()
                loadKebunForAdmin()
            }
            .addOnFailureListener { e ->
                Log.e("AdminHome", "Gagal melakukan batch write", e)
                Toast.makeText(requireContext(), "Gagal menyimpan verifikasi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadKebunForAdmin() {
        swipeRefreshLayout.isRefreshing = true
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val adminCity = doc.getString("city")
            if (adminCity.isNullOrEmpty()) {
                showEmpty(); swipeRefreshLayout.isRefreshing = false; return@addOnSuccessListener
            }
            db.collection("users").whereEqualTo("city", adminCity).get().addOnSuccessListener { usersSnap ->
                val userIds = usersSnap.documents.mapNotNull { it.id }
                if (userIds.isEmpty()) {
                    showEmpty(); swipeRefreshLayout.isRefreshing = false; return@addOnSuccessListener
                }
                db.collection("kebun").whereIn("userId", userIds).get().addOnSuccessListener { kebunSnap ->
                    val kebunList = kebunSnap.documents.mapNotNull { d -> d.toObject(Kebun::class.java)?.copy(idKebun = d.id) }
                    if (kebunList.isEmpty()) {
                        showEmpty(); swipeRefreshLayout.isRefreshing = false; return@addOnSuccessListener
                    }
                    fetchUserNamesAndCombine(kebunList)
                }.addOnFailureListener { swipeRefreshLayout.isRefreshing = false }
            }.addOnFailureListener { swipeRefreshLayout.isRefreshing = false }
        }.addOnFailureListener { swipeRefreshLayout.isRefreshing = false }
    }

    private fun fetchUserNamesAndCombine(kebunList: List<Kebun>) {
        val userIds = kebunList.map { it.userId }.distinct().filter { it.isNotEmpty() }
        if (userIds.isEmpty()) {
            val combinedList = kebunList.map { KebunAdminViewData(it, "Nama tidak ditemukan") }
            updateListSorted(combinedList); swipeRefreshLayout.isRefreshing = false; return
        }
        db.collection("users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), userIds).get().addOnSuccessListener { usersSnap ->
            val userNameMap = usersSnap.documents.associate { doc -> doc.id to (doc.getString("name") ?: "Tanpa Nama") }
            val combinedList = kebunList.map { kebun ->
                KebunAdminViewData(kebun, userNameMap[kebun.userId] ?: "Tidak Ditemukan")
            }
            updateListSorted(combinedList)
        }.addOnFailureListener {
            val combinedList = kebunList.map { KebunAdminViewData(it, "Gagal Memuat Nama") }
            updateListSorted(combinedList)
        }.addOnCompleteListener {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun updateListSorted(list: List<KebunAdminViewData>) {
        val order = mapOf("pending" to 0, "verifikasi1" to 1, "revisi" to 2, "rejected" to 4)
        val sortedList = list.sortedWith(compareBy { order[it.kebun.status.lowercase()] ?: 99 })
        fullDataList = sortedList
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