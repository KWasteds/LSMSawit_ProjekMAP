package com.example.lsmsawit_projekmap

import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Body

interface ApiService {
    @POST("predict")
    suspend fun predictPrice(@Body request: PredictionRequest): Response<PredictionResponse>
}

data class PredictionRequest(
    val data_7_hari: List<List<Float>> // Sesuai dengan definisi FastAPI
)

data class PredictionResponse(
    val status: String,
    val prediksi_harga_besok: Double,
    val satuan: String,
    val detail_lstm: Double?
)
