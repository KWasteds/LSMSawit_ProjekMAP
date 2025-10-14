package com.example.lsmsawit_projekmap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem // Pastikan import ini ada
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

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // 👇 TAMBAHKAN DEKLARASI INI
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener {
            // Kita belum menambahkan status di sini, jadi form akan default bisa diedit
            // Ini untuk FAB (tombol tambah baru), bukan edit
            val formFragment = FormIsiDataKebun()
            formFragment.show(supportFragmentManager, "FormIsiDataKebunTag")
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        val menu = navView.menu
        val petaniMenuItem = menu.findItem(R.id.nav_petani)
        petaniMenuItem?.isVisible = false

        val semuaKebunMenuItem = menu.findItem(R.id.nav_semua_kebun)
        semuaKebunMenuItem?.isVisible = false

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                }
                R.id.nav_account -> {
                    startActivity(Intent(this, AccountSettingActivity::class.java))
                }
            }


            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val logoutButton = navView.findViewById<Button>(R.id.btnLogout)
        logoutButton?.setOnClickListener {
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            auth.signOut() // Sign out dari Firebase Auth
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Tutup MainActivity
        }

        updateNavHeaderFromFirestore()
    }

    private fun updateNavHeaderFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        val headerView = binding.navView.getHeaderView(0)
        val nameText = headerView.findViewById<TextView>(R.id.textViewName)
        val emailText = headerView.findViewById<TextView>(R.id.textViewEmail)
        val imageView = headerView.findViewById<ImageView>(R.id.imageView)

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
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
                Log.e("MainActivity", "Error getting user data", exception)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        this.menu = menu // Simpan referensi menu
        checkUnreadNotifications() // Cek notifikasi saat menu pertama kali dibuat
        return true
    }

    // onResume akan memanggil checkUnreadNotifications
    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            updateNavHeaderFromFirestore()
            checkUnreadNotifications()
        }
    }

    private fun checkUnreadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val notifIcon = menu?.findItem(R.id.action_notifications)
                if (notifIcon != null) {
                    if (!documents.isEmpty) {
                        notifIcon.setIcon(R.drawable.ic_notification_on)
                    } else {
                        notifIcon.setIcon(R.drawable.ic_notification_off)
                    }
                }
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                startActivity(Intent(this, NotifikasiActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}