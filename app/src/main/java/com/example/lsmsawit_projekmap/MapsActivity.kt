package com.example.lsmsawit_projekmap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.Toast


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val lokasiString = intent.getStringExtra("lokasi")
        val namaKebun = intent.getStringExtra("namaKebun") ?: "Lokasi Kebun"

        if (!lokasiString.isNullOrEmpty()) {
            try {
                val parts = lokasiString.split(",")
                val lat = parts[0].toDouble()
                val lng = parts[1].toDouble()
                val posisi = LatLng(lat, lng)

                mMap.addMarker(
                    MarkerOptions()
                        .position(posisi)
                        .title(namaKebun)
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posisi, 16f))

            } catch (e: Exception) {
                Toast.makeText(this, "Format lokasi tidak valid", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }
}
