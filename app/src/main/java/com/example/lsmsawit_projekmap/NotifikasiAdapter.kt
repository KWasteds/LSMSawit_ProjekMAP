package com.example.lsmsawit_projekmap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lsmsawit_projekmap.model.Notifikasi
import java.text.SimpleDateFormat
import java.util.Locale

class NotifikasiAdapter(private val notifList: MutableList<Notifikasi>) : RecyclerView.Adapter<NotifikasiAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val indicator: View = view.findViewById(R.id.unread_indicator)
        val message: TextView = view.findViewById(R.id.tvNotifMessage)
        val note: TextView = view.findViewById(R.id.tvNotifNote)
        val timestamp: TextView = view.findViewById(R.id.tvNotifTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notifikasi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notifList[position]

        holder.message.text = notif.message

        if (!notif.note.isNullOrBlank()) {
            holder.note.visibility = View.VISIBLE
            holder.note.text = "Catatan: ${notif.note}"
        } else {
            holder.note.visibility = View.GONE
        }

        notif.timestamp?.let {
            holder.timestamp.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
        }

        holder.indicator.visibility = if (notif.read) View.INVISIBLE else View.VISIBLE
    }

    override fun getItemCount() = notifList.size

    // Fungsi ini sekarang akan bekerja karena notifList adalah MutableList
    fun updateList(newList: List<Notifikasi>) {
        notifList.clear()
        notifList.addAll(newList)
        notifyDataSetChanged()
    }
}