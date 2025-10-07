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
import android.net.Uri


class KebunAdapter(
    private var items: MutableList<Kebun>,
    private val onItemClick: (Kebun) -> Unit,
    private val onEditClick: (Kebun) -> Unit
) : RecyclerView.Adapter<KebunAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgKebun: ImageView = view.findViewById(R.id.imgKebun)
        val tvNama: TextView = view.findViewById(R.id.tvNamaKebun)
        val tvId: TextView = view.findViewById(R.id.tvIdKebun)
        val tvLokasi: TextView = view.findViewById(R.id.tvLokasi)
        val tvInfo: TextView = view.findViewById(R.id.tvInfoTambahan)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val root: View = view.findViewById(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_kebun, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val k = items[position]
        holder.tvNama.text = k.namaKebun
        holder.tvId.text = "ID: ${k.idKebun}"
        holder.tvLokasi.text = "Lokasi: ${k.lokasi}"
        holder.tvInfo.text = "Luas: ${k.luas} ha • Tanam: ${k.tahunTanam}"

        // 🔹 tampilkan gambar lokal (jika ada)
        if (!k.imageUri.isNullOrEmpty()) {
            try {
                Glide.with(holder.itemView.context)
                    .load(Uri.parse(k.imageUri))
                    .placeholder(R.drawable.placeholder)
                    .into(holder.itemView.findViewById(R.id.imgKebun))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            holder.itemView.findViewById<ImageView>(R.id.imgKebun)
                .setImageResource(R.drawable.placeholder)
        }

        holder.root.setOnClickListener { onItemClick(k) }
        holder.btnEdit.setOnClickListener { onEditClick(k) }
    }


    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Kebun>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}
