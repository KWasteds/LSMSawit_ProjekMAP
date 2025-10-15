package com.example.lsmsawit_projekmap.ui.adminlsm

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lsmsawit_projekmap.AccountSettingActivity
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.ui.auth.LoginActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.lsmsawit_projekmap.ui.adminlsm.ManajemenAkunFragment


class AdminLSMActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar // 🎯 Jadikan properti class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_lsm)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        toolbar = findViewById(R.id.toolbar_admin_lsm) // 🎯 Inisialisasi toolbar
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout_admin_lsm)
        val navView: NavigationView = findViewById(R.id.nav_view_admin_lsm)

        // 🎯 SEMBUNYIKAN MENU "PETANI WILAYAH"
        navView.menu.findItem(R.id.nav_petani).isVisible = false

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        if (savedInstanceState == null) {
            replaceFragment(AdminLSMHomeFragment(), "Dashboard Admin LSM")
            navView.setCheckedItem(R.id.nav_home)
        }

        val logoutButton = navView.findViewById<Button>(R.id.btnLogoutLSM)
        logoutButton?.setOnClickListener {
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // 🎯 LOGIKA NAVIGASI BARU
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    replaceFragment(AdminLSMHomeFragment(), "Dashboard Admin LSM")
                }
                R.id.nav_semua_kebun -> {
                    replaceFragment(SemuaKebunFragment(), "Semua Data Kebun")
                }
                R.id.nav_maps_semua_kebun -> {
                    val fragment = MapsSemuaKebunFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.admin_lsm_content, fragment)
                        .commit()

                    supportActionBar?.title = "Pemetaan Semua Kebun"
                }
                R.id.nav_manajemen_akun -> {
                    replaceFragment(ManajemenAkunFragment(), "Manajemen Akun Pengguna")
                }
                R.id.nav_account -> {
                    startActivity(Intent(this, AccountSettingActivity::class.java))
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        updateNavHeaderFromFirestore(navView)

        // 🎯 LOGIKA SEARCH BAR DINAMIS
        val searchField = findViewById<EditText>(R.id.searchFieldLSM)
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                val currentFragment = supportFragmentManager.findFragmentById(R.id.admin_lsm_content)

                when (currentFragment) {
                    is AdminLSMHomeFragment -> currentFragment.filterList(query)
                    is SemuaKebunFragment -> currentFragment.filterList(query)
                    is MapsSemuaKebunFragment -> {
                        if (query.length >= 5) { // minimal 5 karakter agar tidak langsung query
                            currentFragment.zoomToKebun(query.trim())
                        }
                    }
                }
            }
        })
    }

    // 🎯 FUNGSI HELPER UNTUK MENGGANTI FRAGMENT
    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_lsm_content, fragment)
            .commit()
        toolbar.title = title // Mengubah judul toolbar sesuai halaman
        val searchField = findViewById<EditText>(R.id.searchFieldLSM)
        searchField.text.clear() // Bersihkan field pencarian setiap pindah halaman
    }

    override fun onResume() {
        super.onResume()
        val navView: NavigationView = findViewById(R.id.nav_view_admin_lsm)
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
                if (document.exists()) {
                    nameText.text = document.getString("name") ?: "Nama Admin LSM"
                    emailText.text = document.getString("email") ?: "adminlsm@contoh.com"
                    val photoUrl = document.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .transform(CircleCrop())
                            .placeholder(R.drawable.ic_account_circle)
                            .into(imageView)
                    } else {
                        imageView.setImageResource(R.drawable.ic_account_circle)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat profil", Toast.LENGTH_SHORT).show()
            }
    }
}