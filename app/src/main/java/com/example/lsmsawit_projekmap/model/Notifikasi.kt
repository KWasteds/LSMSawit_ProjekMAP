package com.example.lsmsawit_projekmap.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Notifikasi(
    val id: String = "",
    val userId: String = "",
    val kebunId: String = "",
    val kebunName: String = "",
    val message: String = "",
    val note: String? = null,
    val adminId: String = "",
    var read: Boolean = false,

    @ServerTimestamp
    val timestamp: Date? = null
)
