package com.example.lsmsawit_projekmap.ui.riceprice

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class RicePriceViewModel(
    private val repository: RicePriceRepository
) : ViewModel() {

    val uiState = MutableLiveData<RicePriceUiState>()

    fun predict(hargaNasionalHariIni: Double) {
        viewModelScope.launch {
            uiState.value = RicePriceUiState.Loading
            try {
                // 🔹 nanti diganti API real / Firestore
                val nasionalHistory = listOf(17000.0, 17050.0, 17100.0, 17080.0, 17040.0, 17020.0)
                val globalHistory = listOf(420.0, 422.0, 425.0, 424.0, 423.0, 421.0)

                val result = repository.predictRicePrice(
                    hargaNasionalHariIni,
                    nasionalHistory,
                    globalHistory
                )

                uiState.value = RicePriceUiState.Success(result)

            } catch (e: Exception) {
                uiState.value = RicePriceUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
