package com.example.lsmsawit_projekmap.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lsmsawit_projekmap.AccountSettingActivity
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.ui.auth.LoginActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.text.TextWatcher
import android.text.Editable

class AdminActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var drawerLayout: DrawerLayout // Jadikan properti kelas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val toolbar: Toolbar = findViewById(R.id.toolbar_admin)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout_admin)
        val navView: NavigationView = findViewById(R.id.nav_view_admin)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // <-- TAMBAHKAN BLOK KODE INI -->
        // Ini adalah bagian kunci untuk memuat daftar kebun Anda
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.admin_content, AdminHomeFragment())
                .commit()
        }
        // <-- BATAS PENAMBAHAN KODE -->

        // ✅ Tombol Logout di Drawer
        val logoutButton = navView.findViewById<Button>(R.id.btnLogout)
        logoutButton?.setOnClickListener {
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // ✅ Navigasi Drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow -> {
                    Toast.makeText(this, "Menu ${menuItem.title} belum diatur", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_account -> {
                    val intent = Intent(this, AccountSettingActivity::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // ✅ Update profil header admin
        updateNavHeaderFromFirestore(navView)

        // ✅ Fitur Search Bar
        val searchField = findViewById<EditText>(R.id.searchField)

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Tidak perlu melakukan apa-apa di sini
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Tidak perlu melakukan apa-apa di sini
            }

            override fun afterTextChanged(s: Editable?) {
                // Setelah teks berubah, panggil fungsi filter di fragment
                val query = s.toString()

                // Cari fragment yang sedang aktif
                val fragment = supportFragmentManager.findFragmentById(R.id.admin_content)

                // Pastikan fragment itu adalah AdminHomeFragment, lalu panggil fungsinya
                if (fragment is AdminHomeFragment) {
                    fragment.filterList(query)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val navView: NavigationView = findViewById(R.id.nav_view_admin)
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
                    nameText.text = document.getString("name") ?: "Nama Admin"
                    emailText.text = document.getString("email") ?: "admin@contoh.com"
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
                } else {
                    nameText.text = "Nama Admin"
                    emailText.text = "admin@contoh.com"
                    imageView.setImageResource(R.drawable.ic_account_circle)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat profil admin", Toast.LENGTH_SHORT).show()
            }
    }


}