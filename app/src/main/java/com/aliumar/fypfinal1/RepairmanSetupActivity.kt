package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RepairmanSetupActivity : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val repairmenRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("repairmen")
    private lateinit var loggedInEmail: String
    private var repairmanKey: String? = null

    private val serviceCheckboxes: MutableMap<String, CheckBox> = mutableMapOf()
    private lateinit var editServiceDescription: EditText
    private lateinit var buttonSaveProfile: Button

    // List of all available services (must match strings used elsewhere)
    private val ALL_SERVICES = listOf(
        "Air Conditioning Fix",
        "Gas Tank Replacement",
        "Plumbing",
        "Kitchen Appliance Fix",
        "Electrician / Wiring",
        "Cleaning Service"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repairman_setup)

        // 1. Get logged-in email from SharedPreferences
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        loggedInEmail = sharedPref.getString("email", null) ?: run {
            Toast.makeText(this, "Login session lost. Please re-login.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 2. Initialize UI components
        editServiceDescription = findViewById(R.id.editServiceDescription)
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile)

        serviceCheckboxes["Air Conditioning Fix"] = findViewById(R.id.cbAircon)
        serviceCheckboxes["Gas Tank Replacement"] = findViewById(R.id.cbGas)
        serviceCheckboxes["Plumbing"] = findViewById(R.id.cbPlumbing)
        serviceCheckboxes["Kitchen Appliance Fix"] = findViewById(R.id.cbKitchen)
        serviceCheckboxes["Electrician / Wiring"] = findViewById(R.id.cbElectrician)
        serviceCheckboxes["Cleaning Service"] = findViewById(R.id.cbCleaning)

        // 3. Find the repairman's unique key and load existing data
        fetchRepairmanData()

        // 4. Set up save button listener
        buttonSaveProfile.setOnClickListener {
            saveProfileData()
        }
    }

    private fun fetchRepairmanData() {
        // Iterate through all repairmen to find the one matching the logged-in email
        repairmenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (rSnap in snapshot.children) {
                    val repairman = rSnap.getValue(Repairman::class.java)
                    if (repairman != null && repairman.email.equals(loggedInEmail, ignoreCase = true)) {
                        repairmanKey = rSnap.key
                        loadExistingProfile(repairman)
                        found = true
                        break
                    }
                }
                if (!found) {
                    Toast.makeText(this@RepairmanSetupActivity, "Repairman profile not found.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RepairmanSetupActivity, "Error loading profile: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadExistingProfile(repairman: Repairman) {
        // Pre-fill fields with existing data (if any)
        editServiceDescription.setText(repairman.serviceDescription)

        // Check the checkboxes based on existing specialties
        repairman.specialties.forEach { specialty ->
            serviceCheckboxes[specialty]?.isChecked = true
        }

        if (repairman.isSetupComplete) {
            Toast.makeText(this, "Profile loaded. You can update your details.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileData() {
        val key = repairmanKey
        if (key == null) {
            Toast.makeText(this, "Error: Profile key missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedSpecialties = mutableListOf<String>()
        // Get all selected specialties
        serviceCheckboxes.forEach { (serviceName, checkbox) ->
            if (checkbox.isChecked) {
                selectedSpecialties.add(serviceName)
            }
        }

        if (selectedSpecialties.isEmpty()) {
            Toast.makeText(this, "Please select at least one specialty.", Toast.LENGTH_LONG).show()
            return
        }

        val description = editServiceDescription.text.toString().trim()

        val updates = HashMap<String, Any>()
        updates["specialties"] = selectedSpecialties
        updates["serviceDescription"] = description
        updates["isSetupComplete"] = true // Mark setup as complete

        // Update Firebase
        repairmenRef.child(key).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_LONG).show()
                // Redirect back to the repairman home screen
                val intent = Intent(this, HomeActivity_RM::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save profile: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
