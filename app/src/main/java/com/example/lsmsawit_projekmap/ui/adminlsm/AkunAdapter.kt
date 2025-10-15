package com.example.lsmsawit_projekmap.ui.adminlsm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lsmsawit_projekmap.R

class AkunAdapter(
    private val akunList: List<User>,
    private val onDelete: (User) -> Unit,
    private val onToggle: (User) -> Unit
) : RecyclerView.Adapter<AkunAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textNama)
        val email: TextView = view.findViewById(R.id.textEmail)
        val role: TextView = view.findViewById(R.id.textRole)
        val city: TextView = view.findViewById(R.id.textCity)
        val btnToggle: Button = view.findViewById(R.id.btnToggleStatus)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_akun, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = akunList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = akunList[position]
        holder.name.text = user.name
        holder.email.text = user.email
        holder.role.text = "Role: ${user.role}"
//        holder.city.text = "Kota: ${user.city}"

        holder.btnToggle.text = if (user.status == "aktif") "Nonaktifkan" else "Aktifkan"

        holder.btnToggle.setOnClickListener { onToggle(user) }
        holder.btnDelete.setOnClickListener { onDelete(user) }
    }
}
