package com.aliumar.fypfinal1

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var map: GoogleMap? = null
    private lateinit var textLatLng: TextView
    private var selectedLatLng: LatLng? = null

    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val registerUsername = findViewById<EditText>(R.id.registerUsername)
        val registerEmail = findViewById<EditText>(R.id.registerEmail)
        val registerPassword = findViewById<EditText>(R.id.registerPassword)
        val repairmanCheck = findViewById<CheckBox>(R.id.repairmanTrue)
        val registerButton = findViewById<Button>(R.id.buttonRegister)
        val pickLocationButton = findViewById<Button>(R.id.buttonPickLocation)
        textLatLng = findViewById(R.id.textLatLng)
        mapView = findViewById(R.id.mapView)

        // Initialize MapView
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        val database = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val userRef = database.getReference("users")
        val repairmenRef = database.getReference("repairmen")

        pickLocationButton.setOnClickListener {
            Toast.makeText(this, "Tap on the map to pick your location", Toast.LENGTH_SHORT).show()
        }

        registerButton.setOnClickListener {
            val username = registerUsername.text.toString().trim()
            val email = registerEmail.text.toString().trim()
            val password = registerPassword.text.toString().trim()
            val isRepairman = repairmanCheck.isChecked

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lat = selectedLatLng?.latitude ?: 0.0
            val lng = selectedLatLng?.longitude ?: 0.0

            if (isRepairman) {
                val repairman = Repairman(
                    username = username,
                    email = email,
                    password = password,
                    latitude = lat,
                    longitude = lng,
                    rating = 0.0,
                    storeName = "",
                    specialties = listOf("")
                )

                repairmenRef.child(username).setValue(repairman)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Repairman registered!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }

            } else {
                val user = User(
                    username = username,
                    email = email,
                    password = password,
                    latitude = lat,
                    longitude = lng,
                    rating = 0.0
                )

                userRef.child(username).setValue(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "User registered!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val defaultLocation = LatLng(3.139, 101.6869) // Kuala Lumpur
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        // Tap map to set location
        map?.setOnMapClickListener { latLng ->
            map?.clear()
            map?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            selectedLatLng = latLng
            textLatLng.text = "Latitude: ${latLng.latitude}, Longitude: ${latLng.longitude}"
        }
    }

    // Lifecycle for MapView
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle)
    }
}
