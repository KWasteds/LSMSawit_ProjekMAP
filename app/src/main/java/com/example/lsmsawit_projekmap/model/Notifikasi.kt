package com.example.lsmsawit_projekmap.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Notifikasi(
    val id: String = "",
    val userId: String = "",         // ID Petani yang akan menerima notifikasi
    val kebunId: String = "",
    val kebunName: String = "",
    val message: String = "",        // Pesan notifikasi (misal: "Kebun Anda Disetujui")
    val note: String? = null,        // Catatan revisi dari admin
    val adminId: String = "",        // ID Admin yang melakukan verifikasi
    var isRead: Boolean = false,
    @ServerTimestamp
    val timestamp: Date? = null      // Timestamp kapan notifikasi dibuat
)