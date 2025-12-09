package com.example.lsmsawit_projekmap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lsmsawit_projekmap.databinding.ActivityRicePriceBinding
import com.example.lsmsawit_projekmap.session.SessionManager
import com.example.lsmsawit_projekmap.ui.auth.LoginActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RicePriceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRicePriceBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Cek Session Paling Awal
        val session = SessionManager(this)
        if (!session.isSessionValid()) {
            performLogout() // Logout rapi jika session habis
            return
        }

        // 2. Perpanjang Session (Hanya update waktu, tidak menimpa data)
        session.refreshSession()

        // 3. Setup Layout
        binding = ActivityRicePriceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Cek Auth Firebase (Backup check)
        if (auth.currentUser == null) {
            performLogout()
            return
        }

        setSupportActionBar(binding.toolbarRice)

        supportActionBar?.apply {
            title = "Harga Beras"
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowTitleEnabled(true)
        }

        val drawerLayout = binding.drawerLayoutRice
        val navView = binding.navViewRice
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.ricePriceContainer, RicePriceFragment())
                .commit()
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
                R.id.nav_account -> {
                    startActivity(Intent(this, AccountSettingActivity::class.java))
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // 4. Setup Tombol Logout dengan Benar
        // Kita harus ambil Header View dulu karena tombol ada di dalam header
        val headerView = navView.getHeaderView(0)

        // Gunakan headerView.findViewById, BUKAN navView.findViewById
        val logoutButton = headerView.findViewById<Button>(R.id.btnLogout)

        logoutButton?.setOnClickListener {
            performLogout()
        }

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
                if (document != null && document.exists()) {
                    nameText.text = document.getString("name") ?: "Nama Pengguna"
                    emailText.text = document.getString("email") ?: "-"
                    val photoUrl = document.getString("photoUrl")

                    if (!photoUrl.isNullOrEmpty() && !isDestroyed) {
                        Glide.with(this)
                            .load(photoUrl)
                            .transform(CircleCrop())
                            .placeholder(R.drawable.ic_account_circle)
                            .into(imageView)
                    }
                }
            }
            .addOnFailureListener { e -> Log.e("RicePrice", "Error user data", e) }
    }

    private fun performLogout() {
        val session = SessionManager(this)
        session.logout()
        auth.signOut()

        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Pastikan session tetap valid saat kembali ke activity ini
        val session = SessionManager(this)
        if (!session.isSessionValid()) {
            performLogout()
        } else {
            // Jika valid, update UI header jika perlu
            if (auth.currentUser != null) {
                updateNavHeaderFromFirestore(binding.navViewRice)
            }
        }
    }
}