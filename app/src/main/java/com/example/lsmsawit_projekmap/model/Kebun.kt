package com.example.lsmsawit_projekmap.model

import com.google.firebase.Timestamp

data class Kebun(
    val idKebun: String = "",
    val userId: String = "",
    val namaKebun: String = "",
    val lokasi: String = "",
    val luas: Double = 0.0,
    val tahunTanam: Int = 0,
    val imageUri: String = "",
    val fotoTimestamp: String? = null,
    val status: String = "Pending",

    val verifiedAt: Timestamp? = null,
    val verifiedLsmAt: Timestamp? = null,
    val verifierId: String = "",
    val verifierLsmId: String = "",
    val createdAt: Timestamp? = null
)


