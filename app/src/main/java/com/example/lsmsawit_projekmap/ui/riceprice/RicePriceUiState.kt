package com.example.lsmsawit_projekmap.ui.riceprice

sealed class RicePriceUiState {
    object Loading : RicePriceUiState()
    // Tambahkan data grafik ke Success, yang berisi List<Pair<Hari, Harga>>
    data class Success(
        val price: Double,
        val nationalPrices: List<Double>, // Harga Nasional 7 hari (6 history + hari ini)
        val kaltengPrices: List<Double>   // Harga Kalteng 7 hari (dummy)
    ) : RicePriceUiState()
    data class Error(val message: String) : RicePriceUiState()
}