package com.example.lsmsawit_projekmap.model

data class Users(
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val email: String = "",
    val contact: String = "",
    val role: String = "",
    val photoUrl: String? = null,
    // Tambahkan status di sini
    val status: String = "aktif"
)