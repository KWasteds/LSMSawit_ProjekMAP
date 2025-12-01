package com.example.lsmsawit_projekmap.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lsmsawit_projekmap.model.AppDatabase
import com.example.lsmsawit_projekmap.model.PendingKebun
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import android.util.Log
import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject

class KebunSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    override suspend fun doWork(): Result {
        Log.d("KebunSync", "Worker started")

        val dao = AppDatabase.getDatabase(applicationContext).pendingKebunDao()
        val pendingList = dao.getAll()

        if (pendingList.isEmpty()) return Result.success()

        for (item in pendingList) {
            try {
                when (item.action) {
                    "create" -> uploadCreate(item)
                    "update" -> uploadUpdate(item)
                    "delete" -> uploadDelete(item)
                }

                dao.delete(item)

            } catch (e: Exception) {
                return Result.retry()
            }
        }

        return Result.success()
    }

    private suspend fun uploadCreate(item: PendingKebun) {
        val imageUrl = if (item.imageUri != null) {
            uploadImageToCloudinary(item.imageUri)
        } else {
            ""
        }

        val createdAtTs = com.google.firebase.Timestamp.now()

        val lokasi = item.lokasi

        val map = mapOf(
            "idKebun" to item.kebunId,
            "namaKebun" to item.nama,
            "lokasi" to lokasi,
            "luas" to item.luas,
            "tahunTanam" to item.tahunTanam,
            "imageUri" to imageUrl,
            "status" to "pending",

            "userId" to item.userId,
            "createdAt" to createdAtTs,
            "fotoTimestamp" to "01/01/1970, 07:00:00",
            "verifiedAt" to null,
            "verifiedLsmAt" to null,
            "verifierId" to "",
            "verifierLsmId" to "",
        )

        db.collection("kebun").document(item.kebunId).set(map).await()
    }

    private suspend fun uploadUpdate(item: PendingKebun) {

        val updateMap = mutableMapOf<String, Any>(
            "namaKebun" to item.nama,
            "lokasi" to item.lokasi,
            "luas" to item.luas,
            "tahunTanam" to item.tahunTanam
        )

        if (item.imageUri != null && item.imageUri.startsWith("content://")) {
            val newImage = uploadImageToCloudinary(item.imageUri)
            updateMap["imageUri"] = newImage
        }

        db.collection("kebun").document(item.kebunId).update(updateMap).await()
    }

    private suspend fun uploadDelete(item: PendingKebun) {
        db.collection("kebun").document(item.kebunId).delete().await()
    }

    private suspend fun uploadImageToCloudinary(localUri: String): String {
        val cloudName = "dw5jofoyu"
        val uploadPreset = "fotokebun"
        val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

        val uri = Uri.parse(localUri)
        val inputStream = applicationContext.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "kebun_${System.currentTimeMillis()}.jpg",
                RequestBody.create("image/*".toMediaTypeOrNull(), bytes!!)
            )
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder().url(uploadUrl).post(body).build()
        val client = OkHttpClient()

        val response = client.newCall(request).execute()
        val json = JSONObject(response.body!!.string())

        return json.getString("secure_url")
    }
}

