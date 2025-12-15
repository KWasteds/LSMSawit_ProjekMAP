package com.example.lsmsawit_projekmap.repository

import com.example.lsmsawit_projekmap.model.Kebun
import com.example.lsmsawit_projekmap.model.KebunAdminViewData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class KebunRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllKebunWithOwner(): List<KebunAdminViewData> {
        // 1. Ambil kebun
        val kebunSnap = db.collection("kebun")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        val kebunList = kebunSnap.documents.mapNotNull { d ->
            d.toObject(Kebun::class.java)?.copy(idKebun = d.id)
        }

        if (kebunList.isEmpty()) return emptyList()

        // 2. Ambil user pemilik
        val userIds = kebunList.map { it.userId }.distinct()

        val usersSnap = db.collection("users")
            .whereIn(FieldPath.documentId(), userIds)
            .get()
            .await()

        val userMap = usersSnap.documents.associate { doc ->
            doc.id to (doc.getString("name") ?: "Tanpa Nama")
        }

        // 3. Gabungkan
        return kebunList.map { kebun ->
            KebunAdminViewData(
                kebun = kebun,
                namaPemilik = userMap[kebun.userId] ?: "Tidak Diketahui"
            )
        }
    }
}
