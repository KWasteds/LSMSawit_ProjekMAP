package com.example.lsmsawit_projekmap.ui.adminlsm

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.lsmsawit_projekmap.R
import com.example.lsmsawit_projekmap.ui.admin.VerifikasiDialogListener

class VerifikasiDialogFragmentLSM : DialogFragment() {

    // ... (kode companion object tidak berubah)
    companion object {
        fun newInstance(idKebun: String, nama: String, ownerUserId: String): VerifikasiDialogFragmentLSM {
            val f = VerifikasiDialogFragmentLSM()
            val args = Bundle().apply {
                putString("idKebun", idKebun)
                putString("nama", nama)
                putString("ownerUserId", ownerUserId)
            }
            f.arguments = args
            return f
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // ... (kode di atasnya tidak berubah)
        val root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_verifikasi_lsm, null)
        val radioGroup = root.findViewById<RadioGroup>(R.id.rgActionsLSM)
        val rbApprove = root.findViewById<RadioButton>(R.id.rbApproveLSM)
        val rbReject = root.findViewById<RadioButton>(R.id.rbRejectLSM)
        val etNote = root.findViewById<EditText>(R.id.etNoteLSM)
        val btnOk = root.findViewById<Button>(R.id.btnOkLSM)
        val btnCancel = root.findViewById<Button>(R.id.btnCancelLSM)
        val tvTitle = root.findViewById<TextView>(R.id.tvDialogTitleLSM)

        val nama = requireArguments().getString("nama", "Kebun")
        val idKebun = requireArguments().getString("idKebun", "")
        val ownerUserId = requireArguments().getString("ownerUserId", "")

        tvTitle.text = "Verifikasi Final: $nama"
        etNote.visibility = View.GONE

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            etNote.visibility = if (checkedId == R.id.rbRejectLSM) View.VISIBLE else View.GONE
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(root)
            .create()

        btnCancel.setOnClickListener { dismiss() }


        btnOk.setOnClickListener {
            // ... (logika status dan note tidak berubah)
            val status = when {
                rbApprove.isChecked -> "Diterima"
                rbReject.isChecked -> "Revisi"
                else -> null
            }

            val note = etNote.text.toString().trim()

            if (status == null) {
                Toast.makeText(requireContext(), "Pilih aksi terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (status == "Revisi" && note.isEmpty()){
                Toast.makeText(requireContext(), "Catatan wajib diisi jika menolak pengajuan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (idKebun.isEmpty() || ownerUserId.isEmpty()) {
                Toast.makeText(requireContext(), "Error: Data internal kebun tidak lengkap.", Toast.LENGTH_SHORT).show()
                Log.e("VerifikasiDialogLSM", "Data Kritis Kosong: idKebun=$idKebun, ownerUserId=$ownerUserId")
                return@setOnClickListener
            }

            try {
                // 🎯 **PERBAIKAN DI SINI** 🎯
                // Ganti 'parentFragment' menjadi 'targetFragment'
                val listener = targetFragment as VerifikasiDialogListener
                listener.onVerificationResult(idKebun, ownerUserId, nama, status, note.ifEmpty { null })
            } catch (e: Exception) { // Gunakan Exception yang lebih umum untuk menangkap error
                throw ClassCastException("$targetFragment must implement VerifikasiDialogListener")
            }

            dismiss()
        }

        return dialog
    }
}