package com.example.lsmsawit_projekmap

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import androidx.work.*
import com.example.lsmsawit_projekmap.sync.KebunSyncWorker
import java.util.concurrent.TimeUnit
import android.content.Context
import android.util.Log

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 🔥 Enable Offline Persistence
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()

        val request = PeriodicWorkRequestBuilder<KebunSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "kebun_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
