package com.example.lsmsawit_projekmap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RicePriceViewModel : ViewModel() {

    val prediction = MutableLiveData<Double>()
    val error = MutableLiveData<String>()

    // Fungsi sekarang menerima harga Global dan Nasional hari ini (H-1)
    fun predictRicePrice(hargaNasionalHMinus1: Double, hargaGlobalHMinus1: Double) {

        // --- PASTIKAN TOTAL 6 HARI (H-7 s/d H-2) ---
        val dummyDataHMinus7toHMinus2 = listOf(
            // H-7: 11 FITUR
            listOf(17545.45f, 0.912f, 0.917f, 0.926f, 0.928f, 0.968f, 0.217f, 0.218f, 0.218f, 0.218f, 0.218f),
            // H-6: 11 FITUR
            listOf(17554.54f, 0.906f, 0.912f, 0.919f, 0.923f, 0.961f, 0.217f, 0.218f, 0.218f, 0.218f, 0.218f),
            // H-5: 11 FITUR
            listOf(17563.63f, 0.901f, 0.906f, 0.912f, 0.917f, 0.953f, 0.217f, 0.218f, 0.218f, 0.218f, 0.218f),
            // H-4: 11 FITUR
            listOf(17572.72f, 0.895f, 0.901f, 0.906f, 0.912f, 0.946f, 0.217f, 0.218f, 0.218f, 0.218f, 0.218f),
            // H-3: 11 FITUR
            listOf(17581.81f, 0.890f, 0.895f, 0.899f, 0.906f, 0.939f, 0.217f, 0.218f, 0.218f, 0.218f, 0.218f),
            // H-2: TAMBAHAN SATU HARI LAGI (Agar total 6 hari)
            listOf(17590.90f, 0.884f, 0.890f, 0.892f, 0.901f, 0.932f, 0.217f, 0.218f, 0.218f, 0.218f, 0.218f)
        )

        // H-1 (Hanya 11 FITUR)
        val dataHMinus1 = listOf(
            hargaNasionalHMinus1.toFloat(), // Kolom 0 (Harga Beras Asli)
            hargaGlobalHMinus1.toFloat(),   // Kolom 1 (Fitur Global)
            // Sisa 9 FITUR DUMMY
            0.917f, 0.926f, 0.928f, 0.968f, 0.217f, 0.218f, 0.218f, 0.218f, 0.218f
        )

        // Gabungkan 6 hari dummy + 1 hari input user = 7 hari total
        val data7Hari = dummyDataHMinus7toHMinus2 + listOf(dataHMinus1)

        // Buat objek request JSON
        val requestBody = PredictionRequest(data_7_hari = data7Hari)

        viewModelScope.launch {
            try {
                // KIRIM requestBody melalui POST
                val response = ApiClient.api.predictPrice(requestBody)

                if (response.isSuccessful) {
                    // Ambil prediksi dari field 'prediksi_harga_besok'
                    prediction.postValue(response.body()?.prediksi_harga_besok ?: 0.0)
                } else {
                    error.postValue("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                error.postValue("Exception: ${e.message}")
            }
        }
    }
}