package com.example.lsmsawit_projekmap.ui.adminlsm

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.lsmsawit_projekmap.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class MapsSemuaKebunFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps_semua_kebun, container, false)
        mapView = view.findViewById(R.id.mapViewSemuaKebun)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        loadAllKebunMarkers()
    }

    private fun loadAllKebunMarkers() {
        db.collection("kebun").get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val lokasiString = doc.getString("lokasi") ?: continue
                        val namaKebun = doc.getString("namaKebun") ?: "Tanpa Nama"

                        try {
                            val parts = lokasiString.split(",")
                            if (parts.size == 2) {
                                val lat = parts[0].trim().toDouble()
                                val lng = parts[1].trim().toDouble()
                                val position = LatLng(lat, lng)

                                googleMap?.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title(namaKebun)
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("MapsSemuaKebun", "Lokasi tidak valid: $lokasiString", e)
                        }
                    }

                    // Fokus kamera ke marker pertama jika ada
                    val first = snapshot.documents.firstOrNull()
                    if (first != null) {
                        val lokasiString = first.getString("lokasi")
                        lokasiString?.let {
                            val parts = it.split(",")
                            if (parts.size == 2) {
                                val lat = parts[0].trim().toDouble()
                                val lng = parts[1].trim().toDouble()
                                val firstPosition = LatLng(lat, lng)
                                googleMap?.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(firstPosition, 10f)
                                )
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapsSemuaKebun", "Gagal mengambil data kebun", e)
            }
    }

    fun zoomToKebun(idKebun: String) {
        if (googleMap == null) return

        db.collection("kebun")
            .whereEqualTo("idKebun", idKebun)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val lokasiString = doc.getString("lokasi")
                    val namaKebun = doc.getString("namaKebun") ?: "Tanpa Nama"

                    if (lokasiString != null) {
                        try {
                            val parts = lokasiString.split(",")
                            if (parts.size == 2) {
                                val lat = parts[0].trim().toDouble()
                                val lng = parts[1].trim().toDouble()
                                val position = LatLng(lat, lng)

                                // Tambahkan marker fokus (bisa dibedakan warnanya jika mau)
                                googleMap?.clear() // hapus marker lama agar fokus
                                googleMap?.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title(namaKebun)
                                )
                                googleMap?.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(position, 14f)
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("MapsSemuaKebun", "Error parsing lokasi", e)
                        }
                    }
                } else {
                    Log.w("MapsSemuaKebun", "Kebun dengan ID $idKebun tidak ditemukan")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapsSemuaKebun", "Gagal mencari kebun", e)
            }
    }

    // Lifecycle agar MapView tidak error
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }
}
