package com.example.lsmsawit_projekmap.ui.adminlsm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.model.Kebun
import com.example.lsmsawit_projekmap.model.KebunAdminViewData // Pastikan menggunakan model yang tepat

class KebunLSMAdapter(
    private var list: MutableList<KebunAdminViewData>, // Pastikan menggunakan model yang tepat
    private val onItemClick: (Kebun) -> Unit,
    private val onLocationClick: (Kebun) -> Unit,
    private val onDownloadClick: (Kebun) -> Unit
) : RecyclerView.Adapter<KebunLSMAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgKebunAdminLSM)
        val namaKebun: TextView = view.findViewById(R.id.tvNamaKebunAdminLSM)
        val status: TextView = view.findViewById(R.id.tvStatusAdminLSM)
        val namaPemilik: TextView = view.findViewById(R.id.tvNamaPemilikAdminLSM)
        val idKebun: TextView = view.findViewById(R.id.tvIdKebunAdminLSM)
        val info: TextView = view.findViewById(R.id.tvInfoTambahanAdminLSM)
        val fotoTimestamp: TextView = view.findViewById(R.id.tvFotoTimestampAdminLSM)
        val btnLocation: ImageView = view.findViewById(R.id.btnLocationAdminLSM)
        val btnDownload: ImageView = view.findViewById(R.id.btnDownloadAdminLSM)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_kebun_lsm, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viewData = list[position]
        val kebun = viewData.kebun

        holder.namaKebun.text = kebun.namaKebun

        // Set status teks
        if (kebun.status.equals("Verifikasi1", ignoreCase = true)) {
            holder.status.text = "Pending"
        } else {
            holder.status.text = kebun.status
        }

        holder.namaPemilik.text = "Pemilik: ${viewData.namaPemilik}"
        holder.idKebun.text = "ID: ${kebun.idKebun}"
        holder.info.text = "Luas: ${kebun.luas} ha • Tanam: ${kebun.tahunTanam}"

        val timestamp = kebun.fotoTimestamp ?: "-"
        val lokasi = kebun.lokasi.takeIf { it.isNotEmpty() } ?: "Lokasi tidak ada"
        holder.fotoTimestamp.text = "Diambil: $timestamp ($lokasi)"

        // Warna status
        when (kebun.status.lowercase()) {
            "pending", "verifikasi1" -> holder.status.setBackgroundResource(R.drawable.bg_status_pending)
            "revisi", "rejected" -> holder.status.setBackgroundResource(R.drawable.bg_status_ditolak)
            "diterima", "approved", "accepted" -> holder.status.setBackgroundResource(R.drawable.bg_status_diterima)
            else -> holder.status.setBackgroundResource(R.drawable.bg_status_pending)
        }

        // Load gambar
        Glide.with(holder.itemView.context)
            .load(kebun.imageUri)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .centerCrop()
            .into(holder.img)

        holder.itemView.setOnClickListener { onItemClick(kebun) }
        holder.btnLocation.setOnClickListener { onLocationClick(kebun) }
        holder.btnDownload.setOnClickListener { onDownloadClick(kebun) }

        // ✅ Sembunyikan tombol download jika status belum "Diterima"
        if (kebun.status.equals("Diterima", ignoreCase = true) ||
            kebun.status.equals("Approved", ignoreCase = true) ||
            kebun.status.equals("Accepted", ignoreCase = true)
        ) {
            holder.btnDownload.visibility = View.VISIBLE
        } else {
            holder.btnDownload.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<KebunAdminViewData>) { // Pastikan menggunakan model yang tepat
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}