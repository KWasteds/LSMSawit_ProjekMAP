package com.example.lsmsawit_projekmap.model

data class Kebun(
    var userId: String = "",
    var idKebun: String = "",
    var namaKebun: String = "",
    var lokasi: String = "",
    var luas: Double = 0.0,
    var tahunTanam: Int = 0,
    var imageUri: String = "" // tambahkan ini
)

