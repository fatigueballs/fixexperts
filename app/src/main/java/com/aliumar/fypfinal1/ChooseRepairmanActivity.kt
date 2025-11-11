package com.aliumar.fypfinal1

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
// ADDED: Import AlertDialog and LayoutInflater
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
// END ADDED
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
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

    // REMOVED: private lateinit var editProblemDescription: EditText

    private var loggedInUserEmail: String? = null
    private var actualUserId: String? = null
    private var actualUserName: String = "Unknown User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_repairman)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.recyclerViewRepairmen)
        recyclerView.layoutManager = LinearLayoutManager(this)

        selectedService = intent.getStringExtra("SERVICE_TYPE") ?: ""
        findViewById<TextView>(R.id.textSelectedService).text = "$selectedService"

        // REMOVED: editProblemDescription initialization

        Log.d(TAG, "$selectedService")

        dbRef = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("repairmen")
        auth = FirebaseAuth.getInstance()

        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        loggedInUserEmail = sharedPref.getString("email", null)

        if (loggedInUserEmail != null) {
            fetchUserKeyByEmail(loggedInUserEmail!!)
        } else {
            Toast.makeText(this, "Error: You are not logged in (Email missing).", Toast.LENGTH_LONG).show()
        }

        repairmenList = mutableListOf()
        adapter = RepairmanAdapter(this, repairmenList) { repairman ->
            checkCustomAuthAndShowDateTimePicker(repairman)
        }

        recyclerView.adapter = adapter
        loadRepairmen()
    }

    private fun fetchUserKeyByEmail(email: String) {
        val userRef = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (userSnap in snapshot.children) {
                    val user = userSnap.getValue(User::class.java)
                    if (user != null && user.email.equals(email, ignoreCase = true)) {
                        actualUserId = userSnap.key
                        actualUserName = user.username
                        Log.d(TAG, "Custom User Key found: $actualUserId")
                        found = true
                        break
                    }
                }
                if (!found) {
                    Toast.makeText(this@ChooseRepairmanActivity, "Error: User data not found.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch user key: ${error.message}")
            }
        })
    }

    /**
     * Checks custom authentication state and initiates the description input process.
     */
    private fun checkCustomAuthAndShowDateTimePicker(repairman: Repairman) {
        val userId = actualUserId

        if (userId != null) {
            // User is authenticated, proceed to get description via dialog
            showDescriptionInputDialog(repairman, userId, actualUserName)
        } else {
            Log.e(TAG, "Custom Auth failed. User ID is null.")
            Toast.makeText(this, "Please log in to send a request.", Toast.LENGTH_SHORT).show()
        }
    }

    // NEW: Function to display dialog for problem description input
    private fun showDescriptionInputDialog(repairman: Repairman, userId: String, userName: String) {
        // Dynamically create a multi-line EditText
        val descriptionInput = EditText(this).apply {
            hint = "Describe your problem here..."
            maxLines = 5
            minLines = 3
            setPadding(30, 30, 30, 30) // Add padding
            // Reuse existing background defined in XML
            setBackgroundResource(R.drawable.input_field_background)
        }

        AlertDialog.Builder(this)
            .setTitle("Describe Your Problem for ${repairman.username}")
            .setView(descriptionInput)
            .setPositiveButton("Next: Pick Date/Time") { dialog, _ ->
                val problemDescription = descriptionInput.text.toString().trim()
                if (problemDescription.isEmpty()) {
                    Toast.makeText(this, "Problem description is required.", Toast.LENGTH_LONG).show()
                } else {
                    // Proceed to Date/Time Picker, passing the collected description
                    showDateTimePicker(repairman, userId, userName, problemDescription)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    /**
     * Shows a Date Picker followed by a Time Picker to schedule the service.
     */
    private fun showDateTimePicker(repairman: Repairman, userId: String, userName: String, problemDescription: String) {
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
                sendServiceRequest(repairman, selectedMillis, userId, userName, problemDescription)

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    /**
     * Sends the service request to Firebase after date/time selection.
     */
    private fun sendServiceRequest(repairman: Repairman, dateMillis: Long, userId: String, userName: String, problemDescription: String) {
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
            repairmanId = repairman.id,
            repairmanName = repairman.username,
            serviceType = selectedService,
            date = "",
            dateMillis = dateMillis,
            status = "Pending",
            timestamp = System.currentTimeMillis(),
            problemDescription = problemDescription
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
                        r.id = rSnap.key ?: ""

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

    override fun onDestroy() {
        super.onDestroy()
    }
}