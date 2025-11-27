package com.example.lsmsawit_projekmap.ui.home.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.net.Uri

class KebunFormViewModel : ViewModel() {
    val idKebun = MutableLiveData<String?>()
    val namaKebun = MutableLiveData<String?>()
    val lokasi = MutableLiveData<String?>()
    val luas = MutableLiveData<String?>()
    val tahunTanam = MutableLiveData<String?>()

    val selectedImageUri = MutableLiveData<Uri?>()
    val photoTimestamp = MutableLiveData<String?>()

    // Fungsi baru untuk mereset semua data
    fun clearData() {
        idKebun.value = null
        namaKebun.value = null
        lokasi.value = null
        luas.value = null
        tahunTanam.value = null
        selectedImageUri.value = null
        photoTimestamp.value = null
    }

    fun setImage(uri: Uri?, timestamp: String?) {
        selectedImageUri.value = uri
        photoTimestamp.value = timestamp
    }
}