package com.example.lsmsawit_projekmap.ui.riceprice

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.lsmsawit_projekmap.ApiClient
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.databinding.FragmentRicePriceBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter

class RicePriceFragment : Fragment(R.layout.fragment_rice_price) {

    private var _binding: FragmentRicePriceBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RicePriceViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRicePriceBinding.bind(view)

        val repository = RicePriceRepository(ApiClient.apiService)
        val factory = RicePriceViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[RicePriceViewModel::class.java]

        observeState()

        binding.btnPredict.setOnClickListener {
            val harga = binding.etNational.text.toString().toDoubleOrNull()
            if (harga == null) {
                Toast.makeText(requireContext(), "Input tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.predict(harga)
        }

        // Inisialisasi Chart Awal
        setupChart()
    }

    private fun observeState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RicePriceUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvResult.text = "Loading..."
                    binding.ricePriceChart.visibility = View.GONE // Sembunyikan saat loading
                }
                is RicePriceUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvResult.text = "Tommorows Kalteng Rice Price: ${state.price}"

                    // 🟢 Panggil fungsi untuk mengisi data grafik
                    updateChart(state.nationalPrices, state.kaltengPrices)
                    binding.ricePriceChart.visibility = View.VISIBLE
                }
                is RicePriceUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvResult.text = "Prediction Error"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🟢 Fungsi untuk mengatur tampilan dasar Chart
    private fun setupChart() {
        binding.ricePriceChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setDrawGridBackground(false)
            setPinchZoom(true)

            // X-Axis (Label Hari)
            val xAxis = xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            // Label hari 1-7
            val days = listOf("-6 Day", "-5 Day", "-4 Day", "-3 Day", "-2 Day", "-1 Day", "Today")
            xAxis.valueFormatter = IndexAxisValueFormatter(days)
            xAxis.granularity = 1f
            xAxis.labelCount = 7

            // Y-Axis
            axisLeft.apply {
                setDrawGridLines(true)
                valueFormatter = DefaultAxisValueFormatter(0)
            }
            axisRight.isEnabled = false // Nonaktifkan Y-Axis kanan

            // Legend
            legend.apply {
                form = Legend.LegendForm.LINE
                textSize = 12f
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }

            invalidate()
        }
    }

    // 🟢 Fungsi untuk mengisi dan memperbarui data grafik
    private fun updateChart(nationalPrices: List<Double>, kaltengPrices: List<Double>) {

        // 1. Data Harga Beras Nasional
        val nationalEntries = nationalPrices.mapIndexed { index, price ->
            Entry(index.toFloat(), price.toFloat())
        }
        val nationalDataSet = LineDataSet(nationalEntries, "Harga Nasional").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
        }

        // 2. Data Harga Beras Kalimantan Tengah (Kalteng)
        val kaltengEntries = kaltengPrices.mapIndexed { index, price ->
            Entry(index.toFloat(), price.toFloat())
        }
        val kaltengDataSet = LineDataSet(kaltengEntries, "Harga Kalteng").apply {
            color = Color.RED
            setCircleColor(Color.RED)
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
        }

        // Gabungkan DataSet
        val dataSets = arrayListOf<ILineDataSet>()
        dataSets.add(nationalDataSet)
        dataSets.add(kaltengDataSet)

        val lineData = LineData(dataSets)

        // Terapkan data ke Chart
        binding.ricePriceChart.data = lineData
        binding.ricePriceChart.notifyDataSetChanged()

        // Batasi rentang Y-Axis berdasarkan data (agar tidak terlalu lebar)
        val minY = listOf(nationalPrices.minOrNull() ?: 17000.0, kaltengPrices.minOrNull() ?: 17000.0).minOrNull()!!
        val maxY = listOf(nationalPrices.maxOrNull() ?: 18000.0, kaltengPrices.maxOrNull() ?: 18000.0).maxOrNull()!!

        binding.ricePriceChart.axisLeft.axisMinimum = (minY - 50).toFloat() // Beri sedikit margin
        binding.ricePriceChart.axisLeft.axisMaximum = (maxY + 50).toFloat() // Beri sedikit margin

        // Animasikan dan refresh Chart
        binding.ricePriceChart.animateX(1000)
        binding.ricePriceChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 🟢 anti memory leak
    }
}