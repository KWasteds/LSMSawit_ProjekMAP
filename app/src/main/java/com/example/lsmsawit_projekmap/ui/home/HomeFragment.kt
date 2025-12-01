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
import android.content.Context
import androidx.work.*
import com.example.lsmsawit_projekmap.sync.KebunSyncWorker

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
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh)

        // Set up the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        btnTambah.setOnClickListener {
            val bottomSheet = FormIsiDataKebun()
            bottomSheet.show(parentFragmentManager, "FormKebun")
        }

        // **PERUBAHAN: Tambahkan onStatusClick saat inisialisasi Adapter**
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
            },
            // **IMPLEMENTASI KLIK STATUS: Memanggil BottomSheet**
            onStatusClick = { kebun ->
                val bottomSheet = TimelineBottomSheet.newInstance(kebun.status)
                bottomSheet.show(parentFragmentManager, "TimelineStatus")
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
        // Biarkan seperti semula
    }

    // Public function to reload data (dipanggil setelah simpan/hapus)
    private fun reloadKebun() {
        swipeRefreshLayout.isRefreshing = true
        val userId = auth.currentUser?.uid ?: return
        triggerKebunSync(requireContext())

        db.collection("kebun")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val kebunList = result.toObjects(Kebun::class.java)

                // Logika visibilitas dikembalikan ke asli (hilang jika kosong)
                if (kebunList.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
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

    fun triggerKebunSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<KebunSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueue(request)

        Log.d("KebunSync", "One-time sync triggered")
    }
}