package com.example.lsmsawit_projekmap.ui.admin

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.lsmsawit_projekmap.R

interface VerifikasiDialogListener {
    fun onVerificationResult(idKebun: String, ownerUserId: String, namaKebun: String, newStatus: String, note: String?)
}

class VerifikasiDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(idKebun: String, nama: String, ownerUserId: String): VerifikasiDialogFragment {
            val f = VerifikasiDialogFragment()
            // Menggunakan apply untuk kode yang lebih bersih
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
        val root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_verifikasi, null)
        val radioGroup = root.findViewById<RadioGroup>(R.id.rgActions)
        val rbApprove = root.findViewById<RadioButton>(R.id.rbApprove)
        val rbReject = root.findViewById<RadioButton>(R.id.rbReject)
        val rbRevise = root.findViewById<RadioButton>(R.id.rbRevise)
        val etNote = root.findViewById<EditText>(R.id.etNote)
        val btnOk = root.findViewById<Button>(R.id.btnOk)
        val btnCancel = root.findViewById<Button>(R.id.btnCancel)
        val tvTitle = root.findViewById<TextView>(R.id.tvDialogTitle)

        // Menggunakan requireArguments() agar lebih aman dari null
        val nama = requireArguments().getString("nama", "Kebun")
        val idKebun = requireArguments().getString("idKebun", "")
        val ownerUserId = requireArguments().getString("ownerUserId", "")

        tvTitle.text = "Verifikasi: $nama"
        etNote.visibility = View.GONE

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            etNote.visibility = if (checkedId == R.id.rbRevise) View.VISIBLE else View.GONE
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(root)
            .create()

        btnCancel.setOnClickListener { dismiss() }

        btnOk.setOnClickListener {
            val status = when {
                rbApprove.isChecked -> "Verifikasi1"
                rbReject.isChecked -> "Rejected"
                rbRevise.isChecked -> "Revisi"
                else -> null
            }

            val note = etNote.text.toString().trim().ifEmpty { null }

            if (status == null) {
                Toast.makeText(requireContext(), "Pilih aksi terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (idKebun.isEmpty() || ownerUserId.isEmpty()) {
                Toast.makeText(requireContext(), "Error: Data internal kebun tidak lengkap.", Toast.LENGTH_SHORT).show()
                Log.e("VerifikasiDialog", "Data Kritis Kosong: idKebun=$idKebun, ownerUserId=$ownerUserId")
                return@setOnClickListener
            }

            val resultBundle = bundleOf(
                "idKebun" to idKebun,
                "namaKebun" to nama,
                "ownerUserId" to ownerUserId,
                "status" to status,
                "note" to note
            )

            Log.d("VerifikasiDialog", "Mengirim hasil: $resultBundle")
            try {
                // Pastikan parent fragment mengimplementasikan interface ini
                val listener = parentFragment as VerifikasiDialogListener
                listener.onVerificationResult(idKebun, ownerUserId, nama, status, note)
            } catch (e: ClassCastException) {
                // Tangani error jika fragment tidak mengimplementasikan listener
                throw ClassCastException("$parentFragment must implement VerifikasiDialogListener")
            }

            dismiss()
        }

        return dialog
    }
}