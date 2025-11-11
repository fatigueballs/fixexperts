package com.aliumar.fypfinal1

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.Locale

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var searchLocation: EditText
    private lateinit var textSelectedCoordinates: TextView
    private lateinit var buttonConfirmLocation: Button
    private var selectedLatLng: LatLng? = null
    private var marker: Marker? = null

    companion object {
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        // Find views
        searchLocation = findViewById(R.id.searchLocation)
        textSelectedCoordinates = findViewById(R.id.textSelectedCoordinates)
        buttonConfirmLocation = findViewById(R.id.buttonConfirmLocation)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Get the SupportMapFragment and request notification when the map is ready.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up search button functionality
        searchLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchForLocation(searchLocation.text.toString())
                true
            } else {
                false
            }
        }

        // Set up confirm button
        buttonConfirmLocation.setOnClickListener {
            if (selectedLatLng != null) {
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_LATITUDE, selectedLatLng!!.latitude)
                    putExtra(EXTRA_LONGITUDE, selectedLatLng!!.longitude)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Please tap on the map or search for a location first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val defaultLocation = LatLng(3.139, 101.6869) // Kuala Lumpur
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        googleMap.setOnMapClickListener { latLng ->
            updateSelectedLocation(latLng, "Selected Location")
        }

        // Initialize with location passed from RegisterActivity if available
        val initialLat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        val initialLng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
        if (initialLat != 0.0 || initialLng != 0.0) {
            updateSelectedLocation(LatLng(initialLat, initialLng), "Your Current Location")
            // FIX: Use non-null assertion '!!' since selectedLatLng is guaranteed to be set by updateSelectedLocation
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng!!, 15f))
        }
    }

    private fun updateSelectedLocation(latLng: LatLng, title: String) {
        selectedLatLng = latLng
        marker?.remove() // Remove previous marker

        marker = googleMap.addMarker(MarkerOptions().position(latLng).title(title))

        // Use Geocoder to get address for display
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.adminArea ?: address.countryName ?: "Unknown Location"
                textSelectedCoordinates.text = "Location: $city (Lat: ${String.format("%.4f", latLng.latitude)}, Lng: ${String.format("%.4f", latLng.longitude)})"
            } else {
                textSelectedCoordinates.text = "Location: (Lat: ${String.format("%.4f", latLng.latitude)}, Lng: ${String.format("%.4f", latLng.longitude)})"
            }
        } catch (e: IOException) {
            textSelectedCoordinates.text = "Location: Geocoding failed (Lat: ${String.format("%.4f", latLng.latitude)}, Lng: ${String.format("%.4f", latLng.longitude)})"
        }

        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun searchForLocation(query: String) {
        if (query.isBlank()) {
            Toast.makeText(this, "Please enter a location name to search.", Toast.LENGTH_SHORT).show()
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(query, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val latLng = LatLng(address.latitude, address.longitude)
                updateSelectedLocation(latLng, "Search Result: $query")
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "Location not found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Geocoding service failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}