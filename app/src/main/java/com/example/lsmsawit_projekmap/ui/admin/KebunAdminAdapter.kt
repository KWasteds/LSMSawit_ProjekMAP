package com.example.lsmsawit_projekmap.ui.admin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.model.Kebun
import com.example.lsmsawit_projekmap.model.KebunAdminViewData

// Ubah parameter constructor
class KebunAdminAdapter(
    private var list: MutableList<KebunAdminViewData>,
    private val onItemClick: (Kebun) -> Unit,
    private val onLocationClick: (Kebun) -> Unit
) : RecyclerView.Adapter<KebunAdminAdapter.ViewHolder>() {

    // Perbarui ViewHolder dengan view yang baru
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgKebunAdmin)
        val namaKebun: TextView = view.findViewById(R.id.tvNamaKebunAdmin)
        val status: TextView = view.findViewById(R.id.tvStatusAdmin)
        val namaPemilik: TextView = view.findViewById(R.id.tvNamaPemilikAdmin) // Baru
        val idKebun: TextView = view.findViewById(R.id.tvIdKebunAdmin)       // Baru
        val info: TextView = view.findViewById(R.id.tvInfoTambahanAdmin)
        val fotoTimestamp: TextView = view.findViewById(R.id.tvFotoTimestampAdmin) // Baru
        val btnLocation: ImageView = view.findViewById(R.id.btnLocationAdmin)     // Baru
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Gunakan layout yang sudah kita perbaiki
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_kebun_admin, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viewData = list[position]
        val kebun = viewData.kebun

        // Set semua data ke view
        holder.namaKebun.text = kebun.namaKebun
        holder.status.text = kebun.status
        holder.namaPemilik.text = "Pemilik: ${viewData.namaPemilik}"
        holder.idKebun.text = "ID: ${kebun.idKebun}"
        holder.info.text = "Luas: ${kebun.luas} ha • Tanam: ${kebun.tahunTanam}"

        val timestamp = kebun.fotoTimestamp ?: "-"
        val lokasi = kebun.lokasi.takeIf { it.isNotEmpty() } ?: "Lokasi tidak ada"
        holder.fotoTimestamp.text = "Diambil: $timestamp ($lokasi)"

        // Atur warna status
        when (kebun.status.lowercase()) {
            "pending" -> holder.status.setBackgroundResource(R.drawable.bg_status_pending)
            "revisi", "rejected" -> holder.status.setBackgroundResource(R.drawable.bg_status_ditolak)
            "approved", "accepted", "diterima" -> holder.status.setBackgroundResource(R.drawable.bg_status_diterima)
            else -> holder.status.setBackgroundResource(R.drawable.bg_status_pending)
        }

        Glide.with(holder.itemView.context)
            .load(kebun.imageUri)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .centerCrop()
            .into(holder.img)

        // Set dua listener yang berbeda
        holder.itemView.setOnClickListener { onItemClick(kebun) }
        holder.btnLocation.setOnClickListener { onLocationClick(kebun) }
    }

    override fun getItemCount(): Int = list.size

    // Ubah parameter fungsi updateList
    fun updateList(newList: List<KebunAdminViewData>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}