package com.example.lsmsawit_projekmap

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lsmsawit_projekmap.session.SessionManager
import com.example.lsmsawit_projekmap.ui.admin.AdminActivity
import com.example.lsmsawit_projekmap.ui.adminlsm.AdminLSMActivity
import com.example.lsmsawit_projekmap.ui.auth.LoginActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        if (session.isSessionValid()) {
            // 🔥 Arahkan sesuai role yang disimpan
            when (session.getUserRole()?.lowercase()) {
                "adminwilayah" -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                }
                "adminpusat" -> {
                    startActivity(Intent(this, AdminLSMActivity::class.java))
                }
                else -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        } else {
            // Session expired / belum login
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish()
    }
}
