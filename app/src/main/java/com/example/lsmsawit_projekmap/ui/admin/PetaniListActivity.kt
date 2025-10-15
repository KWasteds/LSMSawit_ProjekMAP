package com.example.lsmsawit_projekmap.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.model.Users
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.lsmsawit_projekmap.AccountSettingActivity
import com.example.lsmsawit_projekmap.ui.auth.LoginActivity
import com.google.android.material.navigation.NavigationView
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop

class PetaniListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PetaniAdapter
    private val list = mutableListOf<PetaniItem>()

    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_petani_list)

        // ✅ Setup Drawer & Toolbar
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayoutPetani)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_petani)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(resources.getColor(android.R.color.white))

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        toggle.drawerArrowDrawable.color = resources.getColor(android.R.color.white)


        val navigationView = findViewById<NavigationView>(R.id.navigationViewPetani)
        updateNavHeader(navigationView)

        // ✅ Tombol Logout
        val logoutButton = navigationView.findViewById<Button>(R.id.btnLogoutPetani)
        logoutButton?.setOnClickListener {
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        val menu = navigationView.menu
        val semuaKebunMenuItem = menu.findItem(R.id.nav_semua_kebun)
        semuaKebunMenuItem?.isVisible = false

        val mapsKebunMenuItem = menu.findItem(R.id.nav_maps_semua_kebun)
        mapsKebunMenuItem?.isVisible = false

        val manajemenAkunMenuItem = menu.findItem(R.id.nav_manajemen_akun)
        manajemenAkunMenuItem?.isVisible = false

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> startActivity(Intent(this, AdminActivity::class.java))
                R.id.nav_petani -> { /* sudah di halaman ini */ }
                R.id.nav_account -> startActivity(Intent(this, AccountSettingActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }


        // ✅ RecyclerView & Data
        recyclerView = findViewById(R.id.recyclerViewPetani)
        progressBar = findViewById(R.id.progressBarPetani)
        tvEmpty = findViewById(R.id.tvEmptyPetani)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PetaniAdapter(list)
        recyclerView.adapter = adapter

        loadAdminCityAndPetani()
    }



    private fun loadAdminCityAndPetani() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            showEmpty("User belum login")
            Log.e("PetaniList", "UID is null, user not logged in")
            return
        }

        Log.d("PetaniList", "Getting admin document for UID: $uid")
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val city = doc.getString("city") ?: ""
                Log.d("PetaniList", "Admin city retrieved: '$city'")

                val toolbar = findViewById<Toolbar>(R.id.toolbar_petani)
                toolbar.title = "Daftar Petani $city"

                if (city.isBlank()) {
                    Log.e("PetaniList", "City is blank or null for admin")
                    showEmpty("Kota admin belum diatur")
                    return@addOnSuccessListener
                }

                loadPetaniByCity(city)
            }
            .addOnFailureListener { e ->
                Log.e("PetaniList", "Failed to load admin city", e)
                progressBar.visibility = View.GONE
                showEmpty("Gagal memuat data admin")
            }
    }

    private fun loadPetaniByCity(city: String) {
        Log.d("PetaniList", "Querying users with role='petani' and city='$city'")
        list.clear()
        adapter.notifyDataSetChanged()

        db.collection("users")
            .whereEqualTo("role", "petani")
            .whereEqualTo("city", city)
            .get()
            .addOnSuccessListener { result ->
                Log.d("PetaniList", "Total petani ditemukan: ${result.size()}")

                if (result.isEmpty) {
                    progressBar.visibility = View.GONE
                    showEmpty("Tidak ada petani di $city")
                    Log.w("PetaniList", "No petani found in this city")
                    return@addOnSuccessListener
                }

                var remaining = result.size()
                for (doc in result) {
                    val user = doc.toObject(Users::class.java)
                    val userId = doc.id
                    Log.d("PetaniList", "Petani found: ${user.name} (UID: $userId)")

                    db.collection("kebun")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { kebunDocs ->
                            val count = kebunDocs.size()
                            Log.d("PetaniList", "Kebun count for $userId = $count")

                            list.add(PetaniItem(userId, user, count))
                            remaining--
                            if (remaining == 0) {
                                progressBar.visibility = View.GONE
                                adapter.updateList(list.sortedBy { it.user.name.lowercase() })
                                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                                Log.d("PetaniList", "Final list loaded: ${list.size} items")
                            }
                        }
                        .addOnFailureListener { errKebun ->
                            Log.e("PetaniList", "Failed to count kebun for $userId", errKebun)
                            remaining--
                            list.add(PetaniItem(userId, user, 0))
                            if (remaining == 0) {
                                progressBar.visibility = View.GONE
                                adapter.updateList(list.sortedBy { it.user.name.lowercase() })
                                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                }
                adapter.updateList(list.sortedBy { it.user.name.lowercase() })
            }
            .addOnFailureListener { e ->
                Log.e("PetaniList", "Error loading petani list", e)
                progressBar.visibility = View.GONE
                showEmpty("Gagal memuat daftar petani")
            }
    }

    private fun updateNavHeader(navigationView: NavigationView) {
        val uid = auth.currentUser?.uid ?: return
        val headerView = navigationView.getHeaderView(0)
        val nameText = headerView.findViewById<TextView>(R.id.textViewName)
        val emailText = headerView.findViewById<TextView>(R.id.textViewEmail)
        val imageView = headerView.findViewById<ImageView>(R.id.imageView)

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    nameText.text = document.getString("name") ?: "Nama Pengguna"
                    emailText.text = document.getString("email") ?: "email@contoh.com"

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
                    nameText.text = "Nama Pengguna"
                    emailText.text = "email@contoh.com"
                    imageView.setImageResource(R.drawable.ic_account_circle)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat profil", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEmpty(message: String) {
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = message
    }
}

data class PetaniItem(
    val uid: String,
    val user: Users,
    val kebunCount: Int
)
