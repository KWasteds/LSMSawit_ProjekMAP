package com.example.lsmsawit_projekmap

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.example.lsmsawit_projekmap.databinding.ActivitySplashBinding
import com.example.lsmsawit_projekmap.session.SessionManager
import com.example.lsmsawit_projekmap.ui.admin.AdminActivity
import com.example.lsmsawit_projekmap.ui.auth.LoginActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Session Manager
        session = SessionManager(this)

        startRotateAnimation()
    }

    private fun startRotateAnimation() {
        val rotate = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        rotate.duration = 1500
        rotate.repeatCount = Animation.INFINITE

        binding.logo.startAnimation(rotate)

        // Gunakan Handler dengan Looper Main
        Handler(Looper.getMainLooper()).postDelayed({

            checkSessionAndNavigate()

        }, 2000)
    }

    private fun checkSessionAndNavigate() {
        Log.d("SessionCheck", "🚀 SPLASH: Memeriksa session...")

        if (session.isSessionValid()) {
            Log.d("SessionCheck", "✅ SPLASH: Session Valid. Mengarahkan sesuai Role.")

            // Ambil role dari session untuk menentukan tujuan
            val role = session.getUserRole()

            val intent = when (role?.lowercase()) {
                "adminwilayah" -> Intent(this, AdminActivity::class.java)
                // Pastikan import untuk AdminLSMActivity sudah benar
                "adminpusat" -> Intent(this, com.example.lsmsawit_projekmap.ui.adminlsm.AdminLSMActivity::class.java)
                else -> Intent(this, MainActivity::class.java) // Default ke MainActivity (Petani/User)
            }

            // Penting: Reset timer session agar diperpanjang saat buka aplikasi
            session.refreshSession()

            startActivity(intent)
        } else {
            Log.d("SessionCheck", "⛔ SPLASH: Session Kosong/Expired. Ke Login.")
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish()
    }
}