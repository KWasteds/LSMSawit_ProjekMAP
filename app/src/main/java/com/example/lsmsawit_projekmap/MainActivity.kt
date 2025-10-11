package com.example.lsmsawit_projekmap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // ✅ Listener untuk menerima hasil dari AccountSettingActivity
    private val accountSettingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data
                val name = data?.getStringExtra("name")
                val email = data?.getStringExtra("email")
                val photoUri = data?.getStringExtra("photoUri")

                Toast.makeText(
                    this,
                    "Profil diperbarui: $name ($email)",
                    Toast.LENGTH_LONG
                ).show()

                // Simpan ke SharedPreferences agar tampil di sidebar
                val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("name", name)
                    putString("email", email)
                    putString("photoUri", photoUri)
                    apply()
                }

                updateNavHeader()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // Floating action button → buka form kebun
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

        // Navigasi ke AccountSettingActivity
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow -> {
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                }

                R.id.nav_account -> {
                    val intent = Intent(this, AccountSettingActivity::class.java)
                    accountSettingLauncher.launch(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
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
        updateNavHeader()
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

    // ✅ Fungsi untuk update header user (nama, email, foto)
    private fun updateNavHeader() {
        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
        val name = sharedPref.getString("name", "Android Studio")
        val email = sharedPref.getString("email", "android.studio@android.com")
        val photoUri = sharedPref.getString("photoUri", null)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)

        val nameText = headerView.findViewById<TextView>(R.id.textViewName)
        val emailText = headerView.findViewById<TextView>(R.id.textViewEmail)
        val imageView = headerView.findViewById<ImageView>(R.id.imageView)

        nameText.text = name
        emailText.text = email

        if (photoUri != null) {
            Glide.with(this)
                .load(Uri.parse(photoUri))
                .transform(CircleCrop())
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_account_circle)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload header setiap kali MainActivity kembali aktif
        updateNavHeader()
    }
}
