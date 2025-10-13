package com.example.lsmsawit_projekmap.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.model.Kebun
import android.content.Intent
import com.example.lsmsawit_projekmap.MapsActivity

class KebunAdapter(
    private var list: MutableList<Kebun>,
    private val onItemClick: (Kebun) -> Unit,
    private val onEditClick: (Kebun) -> Unit,
    private val onStatusClick: (Kebun) -> Unit
) : RecyclerView.Adapter<KebunAdapter.KebunViewHolder>() {

    inner class KebunViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgKebun: ImageView = itemView.findViewById(R.id.imgKebun)
        val tvNamaKebun: TextView = itemView.findViewById(R.id.tvNamaKebun)
        val tvIdKebun: TextView = itemView.findViewById(R.id.tvIdKebun)
        val tvLokasi: TextView = itemView.findViewById(R.id.tvLokasi)
        val tvInfoTambahan: TextView = itemView.findViewById(R.id.tvInfoTambahan)
        val tvFotoTimestamp: TextView = itemView.findViewById(R.id.tvFotoTimestamp)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnLocation: ImageView = itemView.findViewById(R.id.btnLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KebunViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kebun, parent, false)
        return KebunViewHolder(view)
    }

    override fun onBindViewHolder(holder: KebunViewHolder, position: Int) {
        val kebun = list[position]

        // --- Isi data dasar ---
        holder.tvNamaKebun.text = kebun.namaKebun
        holder.tvIdKebun.text = "ID: ${kebun.idKebun}"
        holder.tvLokasi.text = "Lokasi: ${kebun.lokasi}"
        holder.tvInfoTambahan.text = "Luas: ${kebun.luas} ha • Tanam: ${kebun.tahunTanam}"

        if (!kebun.fotoTimestamp.isNullOrEmpty()) {
            holder.tvFotoTimestamp.visibility = View.VISIBLE
            holder.tvFotoTimestamp.text = "Foto diambil: ${kebun.fotoTimestamp}"
        } else {
            holder.tvFotoTimestamp.visibility = View.GONE
        }

        // --- Status visual ---
        holder.tvStatus.text = kebun.status
        when (kebun.status.lowercase()) {
            "pending" -> holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            "revisi" -> holder.tvStatus.setBackgroundResource(R.drawable.bg_status_ditolak)
            "verifikasi1" -> {
                holder.tvStatus.text = "Diproses"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            }
            "approved" -> {
                holder.tvStatus.text = "Disetujui"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_diterima)
            }
            "rejected" -> {
                holder.tvStatus.text = "Ditolak"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_ditolak)
            }
            "complete" -> {
                holder.tvStatus.text = "Complete"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_diterima)
            }
            else -> holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
        }

        if (kebun.status.equals("rejected", ignoreCase = true)) {
            holder.tvStatus.isEnabled = false
            holder.tvStatus.alpha = 0.5f // efek visual agar terlihat disabled
            holder.tvStatus.setOnClickListener(null)
        } else {
            holder.tvStatus.isEnabled = true
            holder.tvStatus.alpha = 1f
            holder.tvStatus.setOnClickListener {
                onStatusClick(kebun)
            }
        }

        // --- Gambar kebun ---
        Glide.with(holder.itemView.context)
            .load(kebun.imageUri)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .centerCrop()
            .into(holder.imgKebun)

        // --- Tombol lokasi ---
        holder.btnLocation.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MapsActivity::class.java)
            intent.putExtra("lokasi", kebun.lokasi)
            intent.putExtra("namaKebun", kebun.namaKebun)
            context.startActivity(intent)
        }

        // --- Logika edit dan klik ---
        val isEditable = kebun.status.lowercase() in listOf("pending", "revisi")

        if (isEditable) {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener { onEditClick(kebun) }
            holder.itemView.setOnClickListener(null)
        } else {
            holder.btnEdit.visibility = View.GONE
            holder.itemView.setOnClickListener { onItemClick(kebun) }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<Kebun>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}