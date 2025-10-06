package com.example.lsmsawit_projekmap.model

data class Kebun(
    var userId: String = "",
    var idKebun: String = "",
    var namaKebun: String = "",
    var lokasi: String = "", // "-6.2341,106.5567"
    var luas: Double = 0.0,
    var tahunTanam: Int = 0
)
