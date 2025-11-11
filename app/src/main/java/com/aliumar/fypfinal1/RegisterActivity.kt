package com.aliumar.fypfinal1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
// ADDED: Activity Result Launcher imports
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
// ADDED: LatLng import (needed for selectedLatLng)
import com.google.android.gms.maps.model.LatLng
// REMOVED: com.google.android.gms.maps.CameraUpdateFactory, GoogleMap, MapView, OnMapReadyCallback, MarkerOptions imports
// ADDED: Firebase imports (already present, keeping for context)
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// MODIFIED: Removed OnMapReadyCallback interface
class RegisterActivity : AppCompatActivity() {

    // REMOVED: private lateinit var mapView: MapView
    // REMOVED: private var map: GoogleMap? = null
    // MODIFIED: Renamed variable ID and type
    private lateinit var textLocationStatus: TextView
    private var selectedLatLng: LatLng? = null

    // REMOVED: private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    // ADDED: Activity Result Launcher
    private lateinit var locationPickerLauncher: ActivityResultLauncher<Intent>

    private lateinit var userRef: com.google.firebase.database.DatabaseReference
    private lateinit var repairmenRef: com.google.firebase.database.DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // ADDED: Initialize ActivityResultLauncher
        locationPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                // Use keys defined in LocationPickerActivity.Companion
                val lat = data?.getDoubleExtra(LocationPickerActivity.EXTRA_LATITUDE, 0.0)
                val lng = data?.getDoubleExtra(LocationPickerActivity.EXTRA_LONGITUDE, 0.0)

                if (lat != null && lng != null) {
                    selectedLatLng = LatLng(lat, lng)

                    // --- THIS IS THE MODIFIED LINE ---
                    textLocationStatus.text = "Location Status: Selected"
                    // --- END MODIFICATION ---

                    Toast.makeText(this, "Location selected successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Location selection cancelled.", Toast.LENGTH_SHORT).show()
            }
        }

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
        // MODIFIED: Reference new TextView ID
        textLocationStatus = findViewById(R.id.textLocationStatus)
        // REMOVED: mapView initialization

        // Initialize Firebase DB references
        val database = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("users")
        repairmenRef = database.getReference("repairmen")

        // REMOVED: MapView lifecycle setup
        // REMOVED: mapView.onCreate(mapViewBundle)
        // REMOVED: mapView.getMapAsync(this)

        // MODIFIED: pickLocationButton launches the new activity
        pickLocationButton.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            // Optionally pass current location if known/previously picked
            selectedLatLng?.let {
                intent.putExtra(LocationPickerActivity.EXTRA_LATITUDE, it.latitude)
                intent.putExtra(LocationPickerActivity.EXTRA_LONGITUDE, it.longitude)
            }
            locationPickerLauncher.launch(intent)
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

            // NEW VALIDATION: Check if a location has been picked
            if (selectedLatLng == null) {
                Toast.makeText(this, "Please pick your location on the map first.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val lat = selectedLatLng?.latitude ?: 0.0
            val lng = selectedLatLng?.longitude ?: 0.0

            // Proceed with registration
            checkDuplicateCredentials(username, email, password, isRepairman, lat, lng)
        }
    }

    // Existing functions (checkDuplicateCredentials, checkRepairmenForDuplicate, registerNewAccount) remain the same.
    // ... [checkDuplicateCredentials function body]
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
                isSetupComplete = false, // Must complete setup later
                isApprovedByAdmin = false // <--- SET TO FALSE ON REGISTRATION
            )

            repairmenRef.child(username).setValue(repairman)
                .addOnSuccessListener {
                    Toast.makeText(this, "Repairman registered! Your account requires admin approval before login.", Toast.LENGTH_LONG).show()
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

    // REMOVED: All MapView lifecycle and OnMapReadyCallback methods.
}