package com.example.lsmsawit_projekmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import kotlin.getValue
import com.example.lsmsawit_projekmap.databinding.FragmentRicePriceBinding


class RicePriceFragment : Fragment() {

    private var _binding: FragmentRicePriceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RicePriceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRicePriceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPredict.setOnClickListener {
            val nasional = binding.inputNasional.text.toString().toDoubleOrNull()
            val global = binding.inputGlobal.text.toString().toDoubleOrNull()

            if (nasional != null && global != null) {
                viewModel.predictRicePrice(nasional, global)
            }
        }

        viewModel.prediction.observe(viewLifecycleOwner) {
            binding.txtResult.text = "Prediction: $it"
        }

        viewModel.error.observe(viewLifecycleOwner) {
            binding.txtResult.text = "Error: $it"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
