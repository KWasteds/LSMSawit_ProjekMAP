package com.example.lsmsawit_projekmap.model

data class Users(
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val email: String = "",
    val contact: String = "", // Ganti 'phone' menjadi 'contact' agar konsisten
    val role: String = "",
    val photoUrl: String? = null // Jadikan nullable untuk menandakan foto bisa tidak ada
)