package com.example.lsmsawit_projekmap.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lsmsawit_projekmap.R

class PetaniAdapter(private val items: MutableList<PetaniItem>) :
    RecyclerView.Adapter<PetaniAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProfile: ImageView = itemView.findViewById(R.id.imgPetaniProfile)
        val tvName: TextView = itemView.findViewById(R.id.tvPetaniName)
        val tvContact: TextView = itemView.findViewById(R.id.tvPetaniContact)
        val tvEmail: TextView = itemView.findViewById(R.id.tvPetaniEmail)
        val tvAddress: TextView = itemView.findViewById(R.id.tvPetaniAddress)
        val tvCount: TextView = itemView.findViewById(R.id.tvPetaniKebunCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_petani, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.tvName.text = it.user.name.ifBlank { "—" }
        holder.tvContact.text = "Telp: ${it.user.contact.ifBlank { "-" }}"
        holder.tvEmail.text = "Email: ${it.user.email.ifBlank { "-" }}"
        holder.tvAddress.text = "Alamat: ${it.user.address.ifBlank { "-" }}"
        holder.tvCount.text = "Jumlah kebun: ${it.kebunCount}"

        val photoUrl = it.user.photoUrl
        val size = holder.itemView.resources.getDimensionPixelSize(R.dimen.profile_size)
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(photoUrl)
                .override(size, size)
                .centerCrop()
                .transform(CircleCrop())
                .placeholder(R.drawable.ic_account_circle)
                .into(holder.imgProfile)
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_account_circle)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<PetaniItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}
