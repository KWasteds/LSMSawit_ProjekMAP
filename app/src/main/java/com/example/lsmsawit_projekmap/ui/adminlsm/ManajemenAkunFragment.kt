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

class ManajemenAkunFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AkunAdapter
    private val akunList = mutableListOf<User>()

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
                    val user = doc.toObject(User::class.java)
                    user.id = doc.id
                    akunList.add(user)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat akun", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Akun")
            .setMessage("Yakin ingin menghapus akun ${user.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("users").document(user.id!!).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Akun dihapus", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun toggleStatus(user: User) {
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

        val nameField = EditText(requireContext()).apply {
            hint = "Nama"
        }
        val emailField = EditText(requireContext()).apply {
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
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
        layout.addView(roleSpinner)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Akun Baru")
            .setView(layout)
            .setPositiveButton("Tambah") { _, _ ->
                val name = nameField.text.toString()
                val email = emailField.text.toString()
                val role = roleSpinner.selectedItem.toString()

                if (name.isEmpty() || email.isEmpty()) {
                    Toast.makeText(requireContext(), "Isi semua field", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newUser = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to role,
                    "status" to "aktif"
                )
                db.collection("users").add(newUser)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Akun berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}

data class User(
    var id: String? = null,
    var name: String? = null,
    var email: String? = null,
    var role: String? = null,
    var status: String? = null
)
