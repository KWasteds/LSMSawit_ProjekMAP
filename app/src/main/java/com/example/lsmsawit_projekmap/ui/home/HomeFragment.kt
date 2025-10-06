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


class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KebunAdapter
    // Jadikan dataList sebagai val karena list-nya sendiri tidak akan di-assign ulang, hanya isinya yang diubah
    private val dataList = mutableListOf<Kebun>()

    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnTambah: Button

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
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewLahan)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        btnTambah = view.findViewById(R.id.btnTambahKebun)

        setupRecyclerView() // Panggil fungsi untuk setup RecyclerView

        btnTambah.setOnClickListener {
            val bottomSheet = FormIsiDataKebun()
            bottomSheet.show(parentFragmentManager, "FormKebun")
        }

        reloadKebun() // Panggil data saat fragment pertama kali dibuat

        return view
    }

    // Fungsi terpisah untuk setup RecyclerView agar onCreateView lebih rapi
    private fun setupRecyclerView() {
        adapter = KebunAdapter(dataList,
            onItemClick = { kebun ->
                // Klik card -> buka form edit (pakai arguments)
                val bs = FormIsiDataKebun.newInstance(
                    kebun.idKebun,
                    kebun.namaKebun,
                    kebun.lokasi,
                    kebun.luas,
                    kebun.tahunTanam
                )
                bs.show(parentFragmentManager, "FormKebun")
            },
            onEditClick = { kebun ->
                val bs = FormIsiDataKebun.newInstance(
                    kebun.idKebun,
                    kebun.namaKebun,
                    kebun.lokasi,
                    kebun.luas,
                    kebun.tahunTanam
                )
                bs.show(parentFragmentManager, "FormKebun")
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    // Public function to reload data (dipanggil setelah simpan/hapus)
    fun reloadKebun() {
        val uid = auth.currentUser?.uid
        Log.d("HomeFragment", "Mencari kebun untuk UID = $uid")

        if (uid == null) {
            Toast.makeText(requireContext(), "Harap login terlebih dahulu", Toast.LENGTH_SHORT).show()
            layoutEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return
        }

        db.collection("kebun")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                dataList.clear() // Kosongkan list yang lama
                for (doc in snapshot.documents) {
                    val k = Kebun(
                        userId = doc.getString("userId") ?: "",
                        idKebun = doc.getString("idKebun") ?: doc.id,
                        namaKebun = doc.getString("namaKebun") ?: "",
                        lokasi = doc.getString("lokasi") ?: "",
                        luas = doc.getDouble("luas") ?: 0.0,
                        tahunTanam = doc.getLong("tahunTanam")?.toInt() ?: 0
                    )
                    dataList.add(k)
                }

                // Cek apakah list kosong untuk menampilkan notifikasi
                if (dataList.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }

                // PERBAIKAN UTAMA: Beri tahu adapter bahwa data telah berubah
                adapter.notifyDataSetChanged()
                Log.d("HomeFragment", "Data berhasil dimuat, jumlah item: ${dataList.size}")
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mengambil data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("HomeFragment", "Error Firestore", e)
            }
    }
}