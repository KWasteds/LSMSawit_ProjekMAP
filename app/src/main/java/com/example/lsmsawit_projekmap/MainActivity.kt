package com.example.lsmsawit_projekmap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lsmsawit_projekmap.databinding.ActivityMainBinding
import com.example.lsmsawit_projekmap.ui.auth.LoginActivity
import com.example.lsmsawit_projekmap.ui.home.FormIsiDataKebun
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.text.get

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // ✅ Tambahkan variabel untuk Firebase Auth dan Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inisialisasi Firebase tidak lagi diperlukan di sini jika sudah ada di Application class
        // FirebaseApp.initializeApp(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Inisialisasi instance Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener {
            val formFragment = FormIsiDataKebun()
            formFragment.show(supportFragmentManager, "FormIsiDataKebunTag")
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // ✅ Sederhanakan navigasi ke AccountSettingActivity
        // Tidak perlu lagi ActivityResultLauncher karena data diambil langsung dari Firestore
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow -> {
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                }
                R.id.nav_account -> {
                    // Cukup mulai activity-nya
                    val intent = Intent(this, AccountSettingActivity::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // ✅ Cek apakah pengguna sudah login sebelum memuat data
        if (auth.currentUser == null) {
            // Jika belum, arahkan ke halaman Login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Tombol Logout
        val logoutButton = navView.findViewById<Button>(R.id.btnLogout)
        logoutButton?.setOnClickListener {
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Update tampilan header saat awal
        updateNavHeaderFromFirestore()
    }

    // Fungsi onResume memastikan data diperbarui setiap kali activity ini kembali aktif
    // (misalnya setelah selesai mengedit profil di AccountSettingActivity)
    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            updateNavHeaderFromFirestore()
        }
    }

    // ✅ Fungsi untuk update header user (nama, email, foto) dari Firestore
    private fun updateNavHeaderFromFirestore() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w("MainActivity", "User UID is null, cannot update nav header.")
            return // Keluar jika UID tidak ada
        }

        val headerView = binding.navView.getHeaderView(0)
        val nameText = headerView.findViewById<TextView>(R.id.textViewName)
        val emailText = headerView.findViewById<TextView>(R.id.textViewEmail)
        val imageView = headerView.findViewById<ImageView>(R.id.imageView)

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: "Nama Pengguna"
                    val email = document.getString("email") ?: "email@contoh.com"
                    val photoUrl = document.getString("photoUrl")

                    nameText.text = name
                    emailText.text = email

                    if (!photoUrl.isNullOrEmpty()) {
                        // Muat gambar dari URL Firestore menggunakan Glide
                        Glide.with(this)
                            .load(photoUrl)
                            .transform(CircleCrop())
                            .placeholder(R.drawable.ic_account_circle) // Gambar default saat loading
                            .error(R.drawable.ic_account_circle) // Gambar default jika gagal load
                            .into(imageView)
                    } else {
                        // Jika tidak ada URL, tampilkan gambar default
                        imageView.setImageResource(R.drawable.ic_account_circle)
                    }
                } else {
                    Log.d("MainActivity", "No such document for user: $uid")
                    // Atur ke default jika data tidak ditemukan
                    nameText.text = "Nama Pengguna"
                    emailText.text = "email@contoh.com"
                    imageView.setImageResource(R.drawable.ic_account_circle)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error getting user data", exception)
                Toast.makeText(this, "Gagal memuat data profil", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}