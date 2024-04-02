package com.example.mapviewapp

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var markerList: MutableList<LatLng> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Haetaan Google Maps -karttanäkymän fragmentti ja asetetaan OnMapReadyCallback tämän aktiviteetin luokalle.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragManager) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Kun kartta on valmis, otetaan Google Maps -olio käyttöön ja asetetaan se muuttujaan.
        mMap = googleMap

        // Asetetaan kuuntelija kartan klikkauksille.
        mMap.setOnMapClickListener { latLng ->
            // Lisätään klikatun sijainnin LatLng-arvo listaan.
            markerList.add(latLng)
            // Lisätään kartalle merkki klikattuun sijaintiin.
            mMap.addMarker(MarkerOptions().position(latLng))

            // Jos listassa on kaksi sijaintia, piirretään niiden välille viiva ja lasketaan niiden välinen etäisyys.
            if (markerList.size == 2) {
                drawLine(markerList[0], markerList[1])
                val distance = calculateDistance(markerList[0], markerList[1])
                // Näytetään etäisyys Toast-ilmoituksena.
                Toast.makeText(this, "Distance: $distance kilometers", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Metodi viivan piirtämiseen kahden pisteen välille.
    private fun drawLine(start: LatLng, end: LatLng) {
        mMap.addPolyline(
            PolylineOptions()
                .add(start, end)
                .width(5f)
                .color(Color.RED)
        )
    }

    // Metodi etäisyyden laskemiseen kahden pisteen välillä Haversine-kaavan avulla.
    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val radius = 6371 // Maapallon säde metreinä
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLat = Math.toRadians(end.latitude - start.latitude)
        val deltaLng = Math.toRadians(end.longitude - start.longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Palautetaan etäisyys kilometreinä.
        return radius * c
    }
}
