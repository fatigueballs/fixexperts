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
// ADDED: Firebase imports for data checking
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RegisterActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var map: GoogleMap? = null
    private lateinit var textLatLng: TextView
    private var selectedLatLng: LatLng? = null

    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    // ADDED: Firebase references for duplicate check
    private lateinit var userRef: com.google.firebase.database.DatabaseReference
    private lateinit var repairmenRef: com.google.firebase.database.DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // ADDED: Back button listener
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Closes this activity and goes back
        }

        val registerUsername = findViewById<EditText>(R.id.registerUsername)
        val registerEmail = findViewById<EditText>(R.id.registerEmail)
        val registerPassword = findViewById<EditText>(R.id.registerPassword)
        val repairmanCheck = findViewById<CheckBox>(R.id.repairmanTrue)
        val registerButton = findViewById<Button>(R.id.buttonRegister)
        val pickLocationButton = findViewById<Button>(R.id.buttonPickLocation)
        textLatLng = findViewById(R.id.textLatLng)
        mapView = findViewById(R.id.mapView)

        // Initialize Firebase DB references
        val database = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("users")
        repairmenRef = database.getReference("repairmen")

        // Initialize MapView
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

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

            // NEW: Start the duplicate check process
            checkDuplicateCredentials(username, email, password, isRepairman, lat, lng)
        }
    }

    // NEW: Function to check for duplicate credentials in both user and repairman nodes
    private fun checkDuplicateCredentials(username: String, email: String, password: String, isRepairman: Boolean, lat: Double, lng: Double) {
        // 1. Check existing users
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var duplicateFound = false
                for (userSnap in snapshot.children) {
                    val existingUsername = userSnap.child("username").value?.toString()
                    val existingEmail = userSnap.child("email").value?.toString()

                    if (username.equals(existingUsername, ignoreCase = true)) {
                        Toast.makeText(this@RegisterActivity, "Error: Username '$username' is already taken.", Toast.LENGTH_LONG).show()
                        duplicateFound = true
                        break
                    }
                    if (email.equals(existingEmail, ignoreCase = true)) {
                        Toast.makeText(this@RegisterActivity, "Error: Email '$email' is already in use.", Toast.LENGTH_LONG).show()
                        duplicateFound = true
                        break
                    }
                }

                if (duplicateFound) {
                    return // Stop registration if found in users node
                } else {
                    // 2. If no duplicate in users, check existing repairmen
                    checkRepairmenForDuplicate(username, email, password, isRepairman, lat, lng)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RegisterActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // NEW: Step 2 of the duplicate check
    private fun checkRepairmenForDuplicate(username: String, email: String, password: String, isRepairman: Boolean, lat: Double, lng: Double) {
        repairmenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var duplicateFound = false
                for (rmSnap in snapshot.children) {
                    val existingUsername = rmSnap.child("username").value?.toString()
                    val existingEmail = rmSnap.child("email").value?.toString()

                    if (username.equals(existingUsername, ignoreCase = true)) {
                        Toast.makeText(this@RegisterActivity, "Error: Username '$username' is already taken.", Toast.LENGTH_LONG).show()
                        duplicateFound = true
                        break
                    }
                    if (email.equals(existingEmail, ignoreCase = true)) {
                        Toast.makeText(this@RegisterActivity, "Error: Email '$email' is already in use.", Toast.LENGTH_LONG).show()
                        duplicateFound = true
                        break
                    }
                }

                if (!duplicateFound) {
                    // 3. If no duplicate found anywhere, proceed with registration
                    registerNewAccount(username, email, password, isRepairman, lat, lng)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RegisterActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // NEW: Function containing the original registration logic
    private fun registerNewAccount(username: String, email: String, password: String, isRepairman: Boolean, lat: Double, lng: Double) {
        if (isRepairman) {
            // MODIFIED: Initializing new fields
            val repairman = Repairman(
                username = username,
                email = email,
                password = password,
                latitude = lat,
                longitude = lng,
                rating = 0.0,
                storeName = "",
                specialties = listOf(), // Empty initially
                serviceDescription = "", // Empty initially
                isSetupComplete = false // Must complete setup later
            )

            repairmenRef.child(username).setValue(repairman)
                .addOnSuccessListener {
                    Toast.makeText(this, "Repairman registered! Please login to complete your profile setup.", Toast.LENGTH_LONG).show()
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