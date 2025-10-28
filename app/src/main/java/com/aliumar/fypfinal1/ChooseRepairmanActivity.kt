package com.aliumar.fypfinal1

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.util.Calendar

class ChooseRepairmanActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RepairmanAdapter
    private lateinit var repairmenList: MutableList<Repairman>
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var selectedService: String = ""
    private val TAG = "ChooseRepairmanActivity"

    // NEW: Variable to hold the user's details obtained via Shared Preferences and DB lookup
    private var loggedInUserEmail: String? = null
    private var actualUserId: String? = null // This will be the Firebase DB key (UID substitute)
    private var actualUserName: String = "Unknown User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_repairman)

        recyclerView = findViewById(R.id.recyclerViewRepairmen)
        recyclerView.layoutManager = LinearLayoutManager(this)

        selectedService = intent.getStringExtra("SERVICE_TYPE") ?: ""
        findViewById<TextView>(R.id.textSelectedService).text = "Selected Service: $selectedService"

        Log.d(TAG, "Selected Service: $selectedService")

        dbRef = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("repairmen")
        auth = FirebaseAuth.getInstance()

        // FIX 1: Retrieve logged-in user's email from shared preferences
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        loggedInUserEmail = sharedPref.getString("email", null)

        // FIX 2: Look up the user's actual Firebase key (UID) using their email
        if (loggedInUserEmail != null) {
            fetchUserKeyByEmail(loggedInUserEmail!!)
        } else {
            Toast.makeText(this, "Error: You are not logged in (Email missing).", Toast.LENGTH_LONG).show()
        }

        repairmenList = mutableListOf()
        // MODIFIED: Pass 'this' (Context) to the RepairmanAdapter
        adapter = RepairmanAdapter(this, repairmenList) { repairman ->
            // Calling the new authentication check that relies on custom login state
            checkCustomAuthAndShowDateTimePicker(repairman)
        }

        recyclerView.adapter = adapter
        loadRepairmen()
    }

    /**
     * Looks up the user's actual Firebase key (which serves as the UID substitute)
     * using the email stored in Shared Preferences.
     */
    private fun fetchUserKeyByEmail(email: String) {
        val userRef = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users")

        // Note: This relies on your 'users' node using the 'username' as the key.
        // We iterate through all users to find a match by email.
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (userSnap in snapshot.children) {
                    val user = userSnap.getValue(User::class.java)
                    if (user != null && user.email.equals(email, ignoreCase = true)) {
                        actualUserId = userSnap.key // Get the Firebase key (username)
                        actualUserName = user.username
                        Log.d(TAG, "Custom User Key found: $actualUserId")
                        found = true
                        break
                    }
                }
                if (!found) {
                    // This could happen if a repairman logs in but tries to access the user request flow
                    Toast.makeText(this@ChooseRepairmanActivity, "Error: User data not found.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch user key: ${error.message}")
            }
        })
    }

    /**
     * Checks custom authentication state (if we have a User ID from DB)
     */
    private fun checkCustomAuthAndShowDateTimePicker(repairman: Repairman) {
        val userId = actualUserId

        if (userId != null) {
            // User is authenticated via your custom system, proceed immediately.
            showDateTimePicker(repairman, userId, actualUserName)
        } else {
            // The lookup failed, or user is not logged in.
            Log.e(TAG, "Custom Auth failed. User ID is null.")
            Toast.makeText(this, "Please log in to send a request.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shows a Date Picker followed by a Time Picker to schedule the service.
     * NOTE: This now requires the resolved custom userId and userName.
     */
    private fun showDateTimePicker(repairman: Repairman, userId: String, userName: String) {
        val calendar = Calendar.getInstance()

        // 1. Date Picker Dialog
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            // 2. Time Picker Dialog
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)

                val selectedMillis = calendar.timeInMillis
                // Proceed to send the request with the scheduled time
                sendServiceRequest(repairman, selectedMillis, userId, userName)

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show() // 'true' for 24-hour format

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    /**
     * Sends the service request to Firebase after date/time selection.
     */
    private fun sendServiceRequest(repairman: Repairman, dateMillis: Long, userId: String, userName: String) {
        if (repairman.id.isEmpty()) {
            Toast.makeText(this, "Error: Repairman ID is missing. Cannot send request.", Toast.LENGTH_SHORT).show()
            return
        }

        val requestsRef = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("serviceRequests")
        val requestKey = requestsRef.push().key

        if (requestKey == null) {
            Toast.makeText(this, "Error creating request key.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the ServiceRequest object
        val request = ServiceRequest(
            id = requestKey,
            userId = userId,
            userName = userName,
            repairmanId = repairman.id, // Use the Repairman's Firebase Key (username in your scheme)
            repairmanName = repairman.username,
            serviceType = selectedService,
            date = "",
            dateMillis = dateMillis,
            status = "Pending",
            timestamp = System.currentTimeMillis()
        )

        // Save the request to Firebase
        requestsRef.child(requestKey).setValue(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Request sent to ${repairman.username} for $selectedService!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send request: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    /**
     * Loads and filters repairmen based on the selected service type.
     */
    private fun loadRepairmen() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                repairmenList.clear()
                val targetService = selectedService.trim()
                Log.d(TAG, "Filtering for service: '$targetService'")

                var repairmenFound = false

                for (rSnap in snapshot.children) {
                    val r = rSnap.getValue(Repairman::class.java)
                    if (r != null) {
                        // Ensure the Firebase key is saved as the Repairman's ID
                        r.id = rSnap.key ?: ""

                        // Use a case-insensitive, fuzzy match check for the specialty
                        val specialtyMatches = r.specialties.any { specialty ->
                            specialty.trim().equals(targetService, ignoreCase = true)
                        }

                        if (targetService.isEmpty() || specialtyMatches) {
                            repairmenList.add(r)
                            repairmenFound = true
                        } else {
                            Log.d(TAG, "Excluding ${r.username}. Specialties: ${r.specialties.joinToString()}")
                        }
                    }
                }
                adapter.notifyDataSetChanged()

                if (!repairmenFound && targetService.isNotEmpty()) {
                    Toast.makeText(this@ChooseRepairmanActivity, "No repairmen found for '$targetService'.", Toast.LENGTH_LONG).show()
                } else if (repairmenFound) {
                    Log.d(TAG, "${repairmenList.size} repairmen loaded.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChooseRepairmanActivity, "Failed to load repairmen: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // The unused Firebase Auth logic has been removed/commented out to rely fully on the custom system
    override fun onDestroy() {
        super.onDestroy()
    }
}
