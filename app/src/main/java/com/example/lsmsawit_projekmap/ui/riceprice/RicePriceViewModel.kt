package com.example.lsmsawit_projekmap.ui.riceprice

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class RicePriceViewModel(
    private val repository: RicePriceRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<RicePriceUiState>()
    val uiState: LiveData<RicePriceUiState> = _uiState

    fun predictPrice(
        nationalPrice: Double,
        lag1: Double,
        lag3: Double
    ) {
        _uiState.value = RicePriceUiState.Loading

        viewModelScope.launch {
            try {
                val result = repository.predictRicePrice(
                    nationalPrice, lag1, lag3
                )
                _uiState.value = RicePriceUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = RicePriceUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }
}
