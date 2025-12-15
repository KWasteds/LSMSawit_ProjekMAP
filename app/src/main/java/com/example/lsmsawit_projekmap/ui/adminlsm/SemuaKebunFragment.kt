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
import androidx.fragment.app.viewModels

class SemuaKebunFragment : Fragment() {

    private lateinit var adapter: SemuaKebunAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var recyclerView: RecyclerView

    private val viewModel: SemuaKebunViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_semua_kebun, container, false)

        recyclerView = v.findViewById(R.id.recyclerViewSemuaKebun)
        layoutEmpty = v.findViewById(R.id.layoutEmptySemuaKebun)
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshSemuaKebun)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = SemuaKebunAdapter(mutableListOf()) { kebun ->
            val intent = Intent(requireContext(), MapsActivity::class.java).apply {
                putExtra("lokasi", kebun.lokasi)
                putExtra("namaKebun", kebun.namaKebun)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadKebun()
        }

        observeViewModel()
        viewModel.loadKebun()

        return v
    }

    private fun observeViewModel() {
        viewModel.kebunList.observe(viewLifecycleOwner) {
            adapter.updateList(it)
            if (it.isEmpty()) showEmpty() else showList()
        }

        viewModel.loading.observe(viewLifecycleOwner) {
            swipeRefreshLayout.isRefreshing = it
        }

        viewModel.error.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }

    fun filterList(query: String) {
        viewModel.filter(query)
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
