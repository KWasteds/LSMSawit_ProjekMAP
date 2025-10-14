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
import com.example.lsmsawit_projekmap.model.KebunAdminViewData

class SemuaKebunAdapter(
    private var list: MutableList<KebunAdminViewData>,
    private val onLocationClick: (Kebun) -> Unit
) : RecyclerView.Adapter<SemuaKebunAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgKebunSemua)
        val namaKebun: TextView = view.findViewById(R.id.tvNamaKebunSemua)
        val namaPemilik: TextView = view.findViewById(R.id.tvNamaPemilikSemua)
        val info: TextView = view.findViewById(R.id.tvInfoTambahanSemua)
        val btnLocation: ImageView = view.findViewById(R.id.btnLocationSemua)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_kebun_semua, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viewData = list[position]
        val kebun = viewData.kebun

        holder.namaKebun.text = kebun.namaKebun
        holder.namaPemilik.text = "Pemilik: ${viewData.namaPemilik}"
        holder.info.text = "Luas: ${kebun.luas} ha • Tanam: ${kebun.tahunTanam}"

        Glide.with(holder.itemView.context)
            .load(kebun.imageUri)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .centerCrop()
            .into(holder.img)

        holder.btnLocation.setOnClickListener { onLocationClick(kebun) }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<KebunAdminViewData>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}