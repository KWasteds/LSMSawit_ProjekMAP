package com.example.lsmsawit_projekmap.model

data class Lahan(
    val id: Int,
    val namaPetani: String,
    val lokasi: String,
    val luas: Double,
    val fotoUrl: String,
    val status: String, // Menunggu / Disetujui / Ditolak
    val koperasi: String?
)
