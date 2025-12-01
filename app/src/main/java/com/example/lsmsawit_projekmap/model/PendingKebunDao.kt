package com.example.lsmsawit_projekmap.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import android.util.Log

@Dao
interface PendingKebunDao {

    @Insert
    suspend fun insertPending(data: PendingKebun)

    @Query("SELECT * FROM pending_kebun ORDER BY timestamp ASC")
    suspend fun getAll(): List<PendingKebun>

    @Delete
    suspend fun delete(item: PendingKebun)

    @Query("SELECT COUNT(*) FROM pending_kebun")
    suspend fun count(): Int
}

