package com.example.lsmsawit_projekmap.ui.riceprice

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.lsmsawit_projekmap.ApiClient
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.databinding.FragmentRicePriceBinding

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
            viewModel.predictPrice(
                nationalPrice = binding.etNational.text.toString().toDouble(),
                lag1 = binding.etLag1.text.toString().toDouble(),
                lag3 = binding.etLag3.text.toString().toDouble()
            )
        }
    }

    private fun observeState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RicePriceUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RicePriceUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvResult.text =
                        "Predicted Price: ${state.price}"
                }
                is RicePriceUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 🟢 anti memory leak
    }
}
