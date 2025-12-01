package com.example.lsmsawit_projekmap.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_kebun")
data class PendingKebun(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val kebunId: String,
    val nama: String,
    val lokasi: String,
    val luas: Double,
    val tahunTanam: Int,
    val imageUri: String?,
    val userId: String,
    val timestamp: Long
)
