package com.example.lsmsawit_projekmap.ui.riceprice

import com.example.lsmsawit_projekmap.ApiService
import com.example.lsmsawit_projekmap.PredictionRequest

class RicePriceRepository(
    private val apiService: ApiService
) {

    suspend fun predictRicePrice(
        hargaNasionalHariIni: Double,
        nasionalHistory: List<Double>, // 6 hari ke belakang
        globalHistory: List<Double>     // 6 hari ke belakang
    ): Double {

        if (nasionalHistory.size != 6 || globalHistory.size != 6) {
            throw IllegalArgumentException("History harus 6 hari")
        }

        val data7Hari = mutableListOf<List<Float>>()

        for (i in 0..6) {
            val hargaNasional =
                if (i < 6) nasionalHistory[i] else hargaNasionalHariIni

            val globalPrice =
                if (i < 6) globalHistory[i] else globalHistory.last()

            val fitur = listOf(
                hargaNasional,
                if (i > 0) nasionalHistory.getOrNull(i - 1) ?: hargaNasional else hargaNasional,
                nasionalHistory.take(i + 1).average(),
                nasionalHistory.getOrNull(i - 3) ?: hargaNasional,
                nasionalHistory.take(i + 1).average(),
                globalHistory.take(i + 1).average(),
                globalHistory.getOrNull(i - 30) ?: globalPrice,
                globalHistory.take(i + 1).average(),
                globalHistory.getOrNull(i - 7) ?: globalPrice,
                globalPrice,
                1.0 // dummy / bias
            ).map { it.toFloat() }

            data7Hari.add(fitur)
        }

        val request = PredictionRequest(data_7_hari = data7Hari)

        val response = apiService.predictPrice(request)

        if (!response.isSuccessful) {
            throw Exception("API Error ${response.code()}")
        }

        return response.body()?.prediksi_harga_besok
            ?: throw Exception("Empty prediction")
    }
}
