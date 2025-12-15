package com.example.lsmsawit_projekmap.ui.riceprice

sealed class RicePriceUiState {
    object Loading : RicePriceUiState()
    data class Success(val price: Double) : RicePriceUiState()
    data class Error(val message: String) : RicePriceUiState()
}
