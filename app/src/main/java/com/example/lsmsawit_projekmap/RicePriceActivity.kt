package com.example.lsmsawit_projekmap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import android.content.Intent
import android.util.Log
import android.view.Menu
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lsmsawit_projekmap.databinding.ActivityRicePriceBinding
import com.example.lsmsawit_projekmap.session.SessionManager
import com.example.lsmsawit_projekmap.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RicePriceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRicePriceBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚨 Inflate binding yang benar
        binding = ActivityRicePriceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 🚨 Set Toolbar dari layout activity_rice_price.xml
        setSupportActionBar(binding.toolbarRice)

        drawerLayout = binding.drawerLayoutRice
        val navView: NavigationView = binding.navViewRice

        // --- Perbaikan sesuai permintaan ---

        // 1. NONAKTIFKAN SIDEBAR & Tombol Hamburger
        // Sidebar tidak bisa dibuka dengan swipe
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        // Sembunyikan tombol hamburger/up di Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = "Harga Beras"

        // 2. Muat RicePriceFragment secara manual
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                // 🚨 Ganti ID container fragment ke ricePriceContainer
                .replace(R.id.ricePriceContainer, RicePriceFragment())
                .commit()
        }

        // --- Konfigurasi Navigasi (Hanya Logika, tanpa tombol hamburger) ---

        // Setup Listener NavView
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Kembali ke MainActivity dan clear stack
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
                R.id.nav_account -> {
                    startActivity(Intent(this, AccountSettingActivity::class.java))
                }
                R.id.nav_rice_price -> {
                    // Karena sudah di Activity ini, tidak perlu tindakan
                }
            }

            // Walaupun terkunci, tetap close drawer untuk keamanan
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // --- Logika Autentikasi dan Logout ---

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Ambil tombol logout dari Nav Header (asumsi ID R.id.btnLogout ada di nav_header_main)
        val logoutButton = navView.getHeaderView(0).findViewById<Button>(R.id.btnLogout)
        logoutButton?.setOnClickListener {

            val session = SessionManager(this)
            session.logout()
            auth.signOut()

            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        }

        // Update Nav Header
        updateNavHeaderFromFirestore(navView)
    }

    private fun updateNavHeaderFromFirestore(navView: NavigationView) {
        val uid = auth.currentUser?.uid ?: return
        val headerView = navView.getHeaderView(0)
        val nameText = headerView.findViewById<TextView>(R.id.textViewName)
        val emailText = headerView.findViewById<TextView>(R.id.textViewEmail)
        val imageView = headerView.findViewById<ImageView>(R.id.imageView)

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                // ... (Logika update Nav Header tetap sama)
                if (document != null && document.exists()) {
                    nameText.text = document.getString("name") ?: "Nama Pengguna"
                    emailText.text = document.getString("email") ?: "email@contoh.com"
                    val photoUrl = document.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this).load(photoUrl).transform(CircleCrop())
                            .placeholder(R.drawable.ic_account_circle)
                            .into(imageView)
                    } else {
                        imageView.setImageResource(R.drawable.ic_account_circle)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("RicePriceActivity", "Error getting user data", exception)
            }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            // Panggil updateNavHeader dengan navView yang benar
            updateNavHeaderFromFirestore(binding.navViewRice)
        }
    }

    // Tidak perlu override onOptionsItemSelected yang berhubungan dengan drawer,
    // karena tombol up/hamburger sudah disembunyikan.
}