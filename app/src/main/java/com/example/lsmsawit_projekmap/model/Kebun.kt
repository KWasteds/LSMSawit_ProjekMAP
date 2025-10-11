package com.example.lsmsawit_projekmap.model

data class Kebun(
    val idKebun: String = "",
    val namaKebun: String = "",
    val lokasi: String = "",
    val luas: Double = 0.0,
    val tahunTanam: Int = 0,
    val imageUri: String = "",
    val fotoTimestamp: String? = null,
    val status: String = "Pending" // ➕ default status baru
)
