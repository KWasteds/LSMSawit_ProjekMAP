package com.example.lsmsawit_projekmap.ui.adminlsm

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lsmsawit_projekmap.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.example.lsmsawit_projekmap.model.Users


class ManajemenAkunFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AkunAdapter
    private val akunList = mutableListOf<Users>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manajemen_akun, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = view.findViewById(R.id.recyclerViewAkun)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = AkunAdapter(akunList,
            onDelete = { user -> confirmDelete(user) },
            onToggle = { user -> toggleStatus(user) }
        )
        recyclerView.adapter = adapter

        val btnTambah = view.findViewById<Button>(R.id.btnTambahAkun)
        btnTambah.setOnClickListener { showTambahDialog() }

        loadUsers()
        return view
    }

    private fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                akunList.clear()
                for (doc in result) {
                    val user = doc.toObject(Users::class.java)
                    user.id = doc.id
                    akunList.add(user)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat akun", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(user: Users) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Akun")
            .setMessage("Yakin ingin menghapus akun ${user.name}? Semua data terkait juga akan dihapus.")
            .setPositiveButton("Hapus") { _, _ ->
                val userId = user.id ?: return@setPositiveButton

                // 1. Hapus dokumen user
                db.collection("users").document(userId).delete()

                // 2. Hapus semua kebun milik user
                db.collection("kebun")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { kebunSnapshots ->
                        for (doc in kebunSnapshots) {
                            db.collection("kebun").document(doc.id).delete()
                        }
                    }

                // 3. Hapus semua notifikasi milik user
                db.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { notifSnapshots ->
                        for (doc in notifSnapshots) {
                            db.collection("notifications").document(doc.id).delete()
                        }
                    }

                Toast.makeText(requireContext(), "Akun dan data terkait dihapus", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .setNegativeButton("Batal", null)
            .show()
    }


    private fun toggleStatus(user: Users) {
        val newStatus = if (user.status == "aktif") "nonaktif" else "aktif"
        db.collection("users").document(user.id!!)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Status diubah menjadi $newStatus", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
    }

    private fun showTambahDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val nameField = EditText(requireContext()).apply { hint = "Nama" }
        val emailField = EditText(requireContext()).apply {
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val passwordField = EditText(requireContext()).apply {
            hint = "Password"
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val roleSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                listOf("petani", "adminwilayah", "adminlsm")
            )
        }

        layout.addView(nameField)
        layout.addView(emailField)
        layout.addView(passwordField)
        layout.addView(roleSpinner)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Akun Baru")
            .setView(layout)
            .setPositiveButton("Tambah") { _, _ ->
                val name = nameField.text.toString()
                val email = emailField.text.toString()
                val password = passwordField.text.toString()
                val role = roleSpinner.selectedItem.toString()

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), "Isi semua field", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        val newUser = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "role" to role,
                            "status" to "aktif"
                        )
                        db.collection("users").document(uid).set(newUser)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Akun berhasil dibuat", Toast.LENGTH_SHORT).show()
                                loadUsers()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal membuat akun", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    fun filterList(query: String) {
        val filtered = akunList.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
                    user.city.contains(query, ignoreCase = true)
        }
        adapter.updateList(filtered)
    }

}