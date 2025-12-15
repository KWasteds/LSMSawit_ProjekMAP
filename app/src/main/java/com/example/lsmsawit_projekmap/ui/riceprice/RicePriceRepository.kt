package com.example.lsmsawit_projekmap.ui.riceprice

import com.example.lsmsawit_projekmap.ApiService
import com.example.lsmsawit_projekmap.PredictionRequest

class RicePriceRepository(
    private val apiService: ApiService
) {

    suspend fun predictRicePrice(
        nationalPrice: Double,
        lag1: Double,
        lag3: Double
    ): Double {

        // 🔹 Contoh bentuk data 7 hari (dummy / sederhana)
        val data7Hari = listOf(
            listOf(nationalPrice.toFloat(), lag1.toFloat(), lag3.toFloat())
        )

        val request = PredictionRequest(
            data_7_hari = data7Hari
        )

        val response = apiService.predictPrice(request)

        if (response.isSuccessful) {
            return response.body()?.prediksi_harga_besok
                ?: throw Exception("Prediction empty")
        } else {
            throw Exception("API Error: ${response.code()}")
        }
    }
}
