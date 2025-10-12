package com.example.lsmsawit_projekmap.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.model.Kebun
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KebunAdapter
    private val dataList = mutableListOf<Kebun>()

    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnTambah: Button
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Listener ketika BottomSheet mengirim result (setFragmentResult)
        setFragmentResultListener("kebun_changed") { _, _ ->
            reloadKebun()
        }
    }

    // In HomeFragment.kt

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize your views using the class-level variables
        recyclerView = view.findViewById(R.id.recyclerViewLahan)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        btnTambah = view.findViewById(R.id.btnTambahKebun)

        // Correctly initialize swipeRefreshLayout using the view
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh) // Or R.id.swipeRefresh if that's the ID in your XML

        // Set up the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        btnTambah.setOnClickListener {
            val bottomSheet = FormIsiDataKebun()
            bottomSheet.show(parentFragmentManager, "FormKebun")
        }

        adapter = KebunAdapter(dataList,
            onItemClick = { kebun ->
                val bs = FormIsiDataKebun.newInstance(
                    kebun.idKebun,
                    kebun.namaKebun,
                    kebun.lokasi,
                    kebun.luas,
                    kebun.tahunTanam,
                    kebun.status
                )
                bs.show(parentFragmentManager, "FormKebun")
            },
            onEditClick = { kebun ->
                val bs = FormIsiDataKebun.newInstance(
                    kebun.idKebun, kebun.namaKebun, kebun.lokasi,
                    kebun.luas, kebun.tahunTanam, kebun.status
                )
                bs.show(parentFragmentManager, "FormKebun")
            }
        )
        recyclerView.adapter = adapter

        // Set the refresh listener on the class-level variable
        swipeRefreshLayout.setOnRefreshListener {
            reloadKebun()
        }

        reloadKebun() // Initial data load
        return view
    }

    // Fungsi terpisah untuk setup RecyclerView agar onCreateView lebih rapi
    private fun setupRecyclerView() {
        adapter = KebunAdapter(
            dataList,
            onItemClick = { /* no-op */ },
            onEditClick = { kebun ->
                val bs = FormIsiDataKebun.newInstance(
                    idKebun = kebun.idKebun,
                    namaKebun = kebun.namaKebun,
                    lokasi = kebun.lokasi,
                    luas = kebun.luas,
                    tahunTanam = kebun.tahunTanam,
                    status = kebun.status
                )
                bs.show(parentFragmentManager, "FormKebun")
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    // Public function to reload data (dipanggil setelah simpan/hapus)
    private fun reloadKebun() {
        swipeRefreshLayout.isRefreshing = true
        val userId = auth.currentUser?.uid ?: return

        db.collection("kebun")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                // Gunakan toObjects agar lebih bersih dan aman
                val kebunList = result.toObjects(Kebun::class.java)

                if (kebunList.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    // Kirim list yang sudah lengkap ke adapter
                    adapter.updateList(kebunList)
                    Log.d("HomeFragment", "RecyclerView updated: ${kebunList.size} items ✅")
                }
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                swipeRefreshLayout.isRefreshing = false
                layoutEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                Log.e("HomeFragment", "Error: ${e.message}", e)
            }
    }
}
