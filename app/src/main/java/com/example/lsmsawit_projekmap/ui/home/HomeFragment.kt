package com.example.lsmsawit_projekmap.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lsmsawit_projekmap.model.Lahan
import com.example.lsmsawit_projekmap.R

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LahanAdapter
    private val dummyData = mutableListOf<Lahan>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewLahan)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        dummyData.clear() // 🔥 tambahkan ini biar tidak dobel

        // dummy data
        dummyData.add(
            Lahan(
                id = 1,
                namaPetani = "Pak Budi",
                lokasi = "Desa Sukamaju",
                luas = 2.5,
                fotoUrl = "https://picsum.photos/400",
                status = "Menunggu",
                koperasi = "Koperasi Sawit Maju"
            )
        )
        dummyData.add(
            Lahan(
                id = 2,
                namaPetani = "Bu Siti",
                lokasi = "Desa Harapan",
                luas = 1.8,
                fotoUrl = "https://picsum.photos/401",
                status = "Disetujui",
                koperasi = "Koperasi Makmur Jaya"
            )
        )

        adapter = LahanAdapter(dummyData) { lahan ->
            Toast.makeText(requireContext(), "Laporan: ${lahan.koperasi}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = adapter
        return view
    }
}
