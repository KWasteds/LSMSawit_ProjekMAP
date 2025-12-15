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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SemuaKebunFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: SemuaKebunAdapter
    private var fullDataList = listOf<KebunAdminViewData>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_semua_kebun, container, false)

        recyclerView = v.findViewById(R.id.recyclerViewSemuaKebun)
        layoutEmpty = v.findViewById(R.id.layoutEmptySemuaKebun)
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshSemuaKebun)

        swipeRefreshLayout.setOnRefreshListener { loadAllKebun() }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = SemuaKebunAdapter(
            mutableListOf(),
            onLocationClick = { kebun ->
                val intent = Intent(requireContext(), MapsActivity::class.java).apply {
                    putExtra("lokasi", kebun.lokasi)
                    putExtra("namaKebun", kebun.namaKebun)
                }
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter
        loadAllKebun()
        return v
    }

    fun filterList(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullDataList
        } else {
            fullDataList.filter { viewData ->

                val kebunNameMatch = viewData.kebun.namaKebun.contains(query, ignoreCase = true)
                val ownerNameMatch = viewData.namaPemilik.contains(query, ignoreCase = true)
                val statusMatch = viewData.kebun.status.contains(query, ignoreCase = true)
                val cityMatch = viewData.city.contains(query, ignoreCase = true)

                kebunNameMatch || ownerNameMatch || statusMatch || cityMatch
            }
        }

        adapter.updateList(filteredList)
        if (filteredList.isEmpty()) showEmpty() else showList()
    }

    private fun loadAllKebun() {
        swipeRefreshLayout.isRefreshing = true
        db.collection("kebun")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { kebunSnap ->
                val kebunList = kebunSnap.documents.mapNotNull { d ->
                    d.toObject(Kebun::class.java)?.copy(idKebun = d.id)
                }
                if (kebunList.isEmpty()) {
                    showEmpty()
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    fetchUserNamesAndCombine(kebunList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("SemuaKebunFragment", "Gagal memuat semua data kebun", e)
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
                showEmpty()
            }
    }

    private fun fetchUserNamesAndCombine(kebunList: List<Kebun>) {
        val userIds = kebunList.map { it.userId }.distinct().filter { it.isNotEmpty() }

        if (userIds.isEmpty()) {
            val combinedList = kebunList.map {
                KebunAdminViewData(
                    it,
                    "Nama tidak ditemukan",
                    ""
                )
            }
            updateList(combinedList)
            swipeRefreshLayout.isRefreshing = false
            return
        }

        db.collection("users")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), userIds)
            .get()
            .addOnSuccessListener { usersSnap ->

                val userMap = usersSnap.documents.associate { doc ->
                    val name = doc.getString("name") ?: "Tanpa Nama"
                    val city = doc.getString("city") ?: ""
                    doc.id to Pair(name, city)
                }

                val combinedList = kebunList.map { kebun ->
                    val data = userMap[kebun.userId]
                    KebunAdminViewData(
                        kebun,
                        data?.first ?: "Tidak Ditemukan",
                        data?.second ?: ""
                    )
                }

                updateList(combinedList)
            }
            .addOnFailureListener {
                val combinedList = kebunList.map {
                    KebunAdminViewData(it, "Gagal Memuat Nama", "")
                }
                updateList(combinedList)
            }
            .addOnCompleteListener {
                swipeRefreshLayout.isRefreshing = false
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