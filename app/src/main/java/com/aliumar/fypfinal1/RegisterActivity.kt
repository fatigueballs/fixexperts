package com.aliumar.fypfinal1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod

class RegisterActivity : AppCompatActivity() {

    private lateinit var textLocationStatus: TextView
    private var selectedLatLng: LatLng? = null
    private lateinit var locationPickerLauncher: ActivityResultLauncher<Intent>

    private lateinit var userRef: com.google.firebase.database.DatabaseReference
    private lateinit var repairmenRef: com.google.firebase.database.DatabaseReference

    // Auth Reference
    private lateinit var auth: FirebaseAuth
    private lateinit var btnToggleRegisterPass: ImageButton
    private var isRegisterPassVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Auth
        auth = FirebaseAuth.getInstance()

        locationPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val lat = data?.getDoubleExtra(LocationPickerActivity.EXTRA_LATITUDE, 0.0)
                val lng = data?.getDoubleExtra(LocationPickerActivity.EXTRA_LONGITUDE, 0.0)

                if (lat != null && lng != null) {
                    selectedLatLng = LatLng(lat, lng)
                    textLocationStatus.text = "Location Status: Selected"
                    Toast.makeText(this, "Location selected successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Location selection cancelled.", Toast.LENGTH_SHORT).show()
            }
        }

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        val registerUsername = findViewById<EditText>(R.id.registerUsername)
        val registerEmail = findViewById<EditText>(R.id.registerEmail)
        val registerPassword = findViewById<EditText>(R.id.registerPassword)
        val repairmanCheck = findViewById<CheckBox>(R.id.repairmanTrue)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        // Initialize the toggle button
        btnToggleRegisterPass = findViewById(R.id.btnToggleRegisterPass)

        // Add Click Listener for Password Toggle
        btnToggleRegisterPass.setOnClickListener {
            isRegisterPassVisible = !isRegisterPassVisible
            if (isRegisterPassVisible) {
                registerPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                registerPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            registerPassword.setSelection(registerPassword.text.length)
        }

        val pickLocationButton = findViewById<Button>(R.id.buttonPickLocation)
        textLocationStatus = findViewById(R.id.textLocationStatus)

        val database = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("users")
        repairmenRef = database.getReference("repairmen")

        pickLocationButton.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
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

            if (selectedLatLng == null) {
                Toast.makeText(this, "Please pick your location on the map first.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val lat = selectedLatLng?.latitude ?: 0.0
            val lng = selectedLatLng?.longitude ?: 0.0

            // Proceed with unique check, then registration
            checkDuplicateCredentials(username, email, password, isRepairman, lat, lng)
        }
    }

    private fun checkDuplicateCredentials(username: String, email: String, password: String, isRepairman: Boolean, lat: Double, lng: Double) {
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var duplicateFound = false
                for (userSnap in snapshot.children) {
                    val existingUsername = userSnap.child("username").value?.toString()
                    // We still check email in DB to keep data clean, even though Auth handles it too
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
                    return
                } else {
                    checkRepairmenForDuplicate(username, email, password, isRepairman, lat, lng)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RegisterActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

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
                    // Proceed to create Auth User and then save to DB
                    registerNewAccount(username, email, password, isRepairman, lat, lng)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RegisterActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // MODIFIED: Uses Firebase Auth + Email Verification
    private fun registerNewAccount(username: String, email: String, password: String, isRepairman: Boolean, lat: Double, lng: Double) {
        // 1. Create User in Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser

                    // 2. Send Verification Email
                    firebaseUser?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            // Show success dialog instead of toast
                            showErrorDialog("Success", "Verification email sent to $email. Please verify before logging in.") {
                                // 3. Save User Data to Realtime Database
                                saveUserDataToDatabase(username, email, password, isRepairman, lat, lng)
                            }
                        } else {
                            val errorMsg = verifyTask.exception?.message ?: "Unknown email verification error"
                            Log.e("RegisterError", "Email Verification Failed: $errorMsg")
                            showErrorDialog("Verification Failed", errorMsg, null)
                        }
                    }
                } else {
                    // --- THIS IS WHERE YOUR ERROR IS COMING FROM ---
                    val errorMsg = task.exception?.message ?: "Unknown registration error"
                    Log.e("RegisterError", "Auth Creation Failed: $errorMsg")

                    // Show the FULL error in a dialog box so you can read it
                    showErrorDialog("Registration Failed", errorMsg, null)
                }
            }
    }

    // Helper function to show the dialog
    private fun showErrorDialog(title: String, message: String, onPositive: (() -> Unit)?) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            onPositive?.invoke()
        }
        builder.show()
    }

    private fun saveUserDataToDatabase(username: String, email: String, password: String, isRepairman: Boolean, lat: Double, lng: Double) {
        if (isRepairman) {
            val repairman = Repairman(
                username = username,
                email = email,
                password = password,
                latitude = lat,
                longitude = lng,
                rating = 0.0,
                storeName = "",
                specialties = listOf(),
                serviceDescription = "",
                isSetupComplete = false,
                isApprovedByAdmin = false // Requires admin approval
            )

            repairmenRef.child(username).setValue(repairman)
                .addOnSuccessListener {
                    // Sign out immediately so they have to login (and check verification)
                    auth.signOut()
                    Toast.makeText(this, "Account created! Please verify your email.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Db Error: ${it.message}", Toast.LENGTH_SHORT).show()
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
                    auth.signOut()
                    Toast.makeText(this, "Account created! Please verify your email.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Db Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}