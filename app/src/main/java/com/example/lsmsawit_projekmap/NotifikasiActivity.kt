package com.example.lsmsawit_projekmap

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lsmsawit_projekmap.model.Notifikasi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch

class NotifikasiActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotifikasiAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 👇 Deklarasikan list sebagai MutableList
    private val notifList = mutableListOf<Notifikasi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifikasi)

        val toolbar: Toolbar = findViewById(R.id.toolbar_notifikasi)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerViewNotifikasi)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inisialisasi adapter SEKALI saja dengan mutable list yang sudah dibuat
        adapter = NotifikasiAdapter(notifList)
        recyclerView.adapter = adapter

    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Anda harus login", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->

                // 🔹 Konversi manual agar id dokumen ikut masuk
                val newNotifList = documents.map { doc ->
                    val notif = doc.toObject(Notifikasi::class.java)
                    notif.copy(id = doc.id) // <- ini penting
                }

                adapter.updateList(newNotifList)
                markNotificationsAsRead(newNotifList)
            }
            .addOnFailureListener { e ->
                Log.e("NotifikasiActivity", "Error loading notifications: ${e.message}", e)
                Toast.makeText(this, "Gagal memuat notifikasi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markNotificationsAsRead(notifList: List<Notifikasi>) {
        val unreadNotifs = notifList.filter { !it.read }
        if (unreadNotifs.isEmpty()) return

        val batch = db.batch()
        for (notif in unreadNotifs) {
            val docRef = db.collection("notifications").document(notif.id)
            batch.update(docRef, "read", true)
        }

        batch.commit().addOnFailureListener { e ->
            Log.e("NotifikasiActivity", "Failed to mark notifications as read", e)
        }
    }

}