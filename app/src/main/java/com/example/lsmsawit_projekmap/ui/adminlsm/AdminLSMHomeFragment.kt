package com.example.lsmsawit_projekmap.ui.adminlsm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.lsmsawit_projekmap.R

class AdminLSMHomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_lsm_home, container, false)
    }

    // 🔹 Fungsi filterList dipanggil dari Activity
    fun filterList(query: String) {
        // TODO: Implementasikan logika pencarian LSM di sini
    }
}
