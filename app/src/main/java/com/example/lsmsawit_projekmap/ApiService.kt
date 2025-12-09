package com.example.lsmsawit_projekmap

import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Body

interface ApiService {

    // UBAH DARI @GET KE @POST
    @POST("predict")
    // Menerima data 7 hari dalam BODY (JSON)
    suspend fun predictPrice(@Body request: PredictionRequest): Response<PredictionResponse>
}

// Model untuk data yang dikirim ke API (Request Body)
data class PredictionRequest(
    val data_7_hari: List<List<Float>> // Sesuai dengan definisi FastAPI
)

// Model untuk data yang diterima dari API (Response Body)
data class PredictionResponse(
    val status: String,
    val prediksi_harga_besok: Double, // Nama variabel harus SAMA PERSIS dengan API
    val satuan: String,
    val detail_lstm: Double? // Jadikan nullable
)