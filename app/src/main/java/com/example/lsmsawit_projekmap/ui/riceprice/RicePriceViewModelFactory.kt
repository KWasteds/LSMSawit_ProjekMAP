package com.example.lsmsawit_projekmap.ui.riceprice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RicePriceViewModelFactory(
    private val repository: RicePriceRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RicePriceViewModel::class.java)) {
            return RicePriceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
