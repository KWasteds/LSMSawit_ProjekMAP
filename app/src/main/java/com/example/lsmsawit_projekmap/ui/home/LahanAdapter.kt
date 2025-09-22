package com.example.lsmsawit_projekmap.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lsmsawit_projekmap.model.Lahan
import com.example.lsmsawit_projekmap.R

class LahanAdapter(
    private val list: List<Lahan>,
    private val onAttachmentClick: (Lahan) -> Unit
) : RecyclerView.Adapter<LahanAdapter.LahanViewHolder>() {

    inner class LahanViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.imgFotoLahan)
        val txtLokasi: TextView = view.findViewById(R.id.txtLokasi)
        val txtDeskripsi: TextView = view.findViewById(R.id.txtDeskripsi)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val btnAttachment: ImageButton = view.findViewById(R.id.btnAttachment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LahanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lahan, parent, false)
        return LahanViewHolder(view)
    }

    override fun onBindViewHolder(holder: LahanViewHolder, position: Int) {
        val item = list[position]
        holder.txtLokasi.text = item.lokasi
        holder.txtDeskripsi.text = "Luas: ${item.luas} Ha - Pemilik: ${item.namaPetani}"
        holder.txtStatus.text = item.status

        Glide.with(holder.view).load(item.fotoUrl).into(holder.imgFoto)

        holder.btnAttachment.setOnClickListener {
            onAttachmentClick(item)
        }
    }

    override fun getItemCount() = list.size
}
