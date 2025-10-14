package com.example.lsmsawit_projekmap.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.lsmsawit_projekmap.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class TimelineBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_STATUS = "current_status"
        fun newInstance(currentStatus: String): TimelineBottomSheet {
            val fragment = TimelineBottomSheet()
            val args = Bundle()
            args.putString(ARG_STATUS, currentStatus)
            fragment.arguments = args
            return fragment
        }
    }

    // Urutan status yang diminta: pending - acc admin wilayah - acc lsm - complete
    private val timelineSteps = listOf(
        "Pending",
        "Acc Admin Wilayah",
        "Diproses Pihak LSM",
        "Selesai"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout dialog_timeline.xml
        return inflater.inflate(R.layout.dialog_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil status yang dikirim dari HomeFragment
        val currentStatus = arguments?.getString(ARG_STATUS)?.lowercase(Locale.ROOT) ?: "pending"

        // Mengambil referensi semua elemen timeline
        val tvSteps = listOf(
            view.findViewById<TextView>(R.id.tvStepPending),
            view.findViewById<TextView>(R.id.tvStepAdmin),
            view.findViewById<TextView>(R.id.tvStepLSM),
            view.findViewById<TextView>(R.id.tvStepComplete)
        )

        val lines = listOf(
            view.findViewById<View>(R.id.line1),
            view.findViewById<View>(R.id.line2),
            view.findViewById<View>(R.id.line3)
        )

        // Menentukan index status saat ini berdasarkan status dari database
        val currentStepIndex = when (currentStatus) {
            "pending", "revisi" -> 0
            "verifikasi1" -> 1 // Asumsi verifikasi1 adalah Acc Admin Wilayah
            "approved" -> 2
            "rejected" -> 0 // Jika rejected, tampilkan status kembali ke awal (Pending/Revisi)
            "diterima" -> 3
            else -> 0
        }

        val colorGreen = ContextCompat.getColor(requireContext(), R.color.green_primary)
        val colorGray = ContextCompat.getColor(requireContext(), R.color.gray_secondary)
        val iconCheck = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle)
        val iconPending = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pending_circle)

        // Looping untuk mengatur tampilan setiap langkah
        for (index in timelineSteps.indices) {
            val tvStep = tvSteps[index]
            val isCompleted = index <= currentStepIndex

            // 1. Atur Teks Status (hanya untuk memastikan teks sama dengan timelineSteps)
            tvStep.text = timelineSteps[index]

            // 2. Atur Warna dan Ikon (Completed vs Pending)
            if (isCompleted) {
                // Status sudah tercapai
                tvStep.setTextColor(colorGreen)
                tvStep.setCompoundDrawablesWithIntrinsicBounds(iconCheck, null, null, null)
            } else {
                // Status belum tercapai
                tvStep.setTextColor(colorGray)
                tvStep.setCompoundDrawablesWithIntrinsicBounds(iconPending, null, null, null)
            }

            // 3. Atur Garis Penghubung
            if (index < lines.size) {
                val lineView = lines[index]
                if (index < currentStepIndex) {
                    // Garis sudah dilewati
                    lineView.setBackgroundColor(colorGreen)
                } else {
                    // Garis belum dilewati
                    lineView.setBackgroundColor(colorGray)
                }
            }
        }
    }
}