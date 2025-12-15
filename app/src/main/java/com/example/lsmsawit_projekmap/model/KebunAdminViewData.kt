package com.example.lsmsawit_projekmap.model

// Data class ini menggabungkan data Kebun dengan nama pemiliknya
data class KebunAdminViewData(
    val kebun: Kebun,
    val namaPemilik: String,
    val city: String = ""
)