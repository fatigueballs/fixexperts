package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*

class UserActivityActivity : AppCompatActivity() {

    private lateinit var recyclerViewRequests: RecyclerView
    private lateinit var tabLayoutStatus: TabLayout // NEW
    private lateinit var spinnerCategory: Spinner // NEW

    private lateinit var requestList: MutableList<ServiceRequest> // Holds all fetched requests
    private lateinit var filteredList: MutableList<ServiceRequest> // Holds requests after filtering
    private lateinit var adapter: UserRequestAdapter
    private lateinit var dbRef: DatabaseReference
    private var actualUserId: String? = null

    private var currentStatusFilter: String = "Ongoing" // "Ongoing" or "Completed"
    private var currentCategoryFilter: String = "All Categories"

    // List of all available services (including the 'All' option)
    private val ALL_SERVICES_FILTER = listOf(
        "All Categories",
        "Air Conditioning Fix",
        "Gas Tank Replacement",
        "Plumbing",
        "Kitchen Appliance Fix",
        "Electrician / Wiring",
        "Cleaning Service"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_activity)

        // ADDED: Back button listener
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        recyclerViewRequests = findViewById(R.id.recyclerViewUserRequests)
        recyclerViewRequests.layoutManager = LinearLayoutManager(this)
        tabLayoutStatus = findViewById(R.id.tabLayoutStatus) // NEW
        spinnerCategory = findViewById(R.id.spinnerCategory) // NEW

        requestList = mutableListOf()
        filteredList = mutableListOf() // Initialize filtered list

        adapter = UserRequestAdapter(
            filteredList, // Use the filtered list for the adapter
            { request -> handleJobDoneConfirmation(request) },
            { request -> launchRatingActivity(request) }
        )

        recyclerViewRequests.adapter = adapter
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        setupCategorySpinner() // NEW
        setupStatusTabs() // NEW

        resolveUserIdAndLoadRequests()
    }

    // NEW: Setup spinner with categories
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ALL_SERVICES_FILTER)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentCategoryFilter = ALL_SERVICES_FILTER[position]
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // NEW: Setup tabs for Ongoing/History
    private fun setupStatusTabs() {
        tabLayoutStatus.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatusFilter = if (tab?.position == 0) "Ongoing" else "Completed"
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun resolveUserIdAndLoadRequests() {
        // ... (existing logic to fetch user ID)
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val loggedInEmail = sharedPref.getString("email", null)

        if (loggedInEmail == null) {
            Toast.makeText(this, "Error: Login information missing.", Toast.LENGTH_LONG).show()
            return
        }

        // Use the Firebase instance that has the correct database URL
        val database = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val userRef = database.getReference("users")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (uSnap in snapshot.children) {
                    val user = uSnap.getValue(User::class.java)
                    if (user != null && user.email.equals(loggedInEmail, ignoreCase = true)) {
                        actualUserId = uSnap.key // Get the Firebase key
                        found = true
                        loadRequests() // Only load requests AFTER the ID is resolved
                        break
                    }
                }
                if (!found) {
                    Toast.makeText(this@UserActivityActivity, "User not found in database.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserActivityActivity, "Failed to resolve ID: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadRequests() {
        val currentUserId = actualUserId

        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(this, "Could not retrieve User ID.", Toast.LENGTH_SHORT).show()
            return
        }

        dbRef.orderByChild("userId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requestList.clear()
                    for (reqSnap in snapshot.children) {
                        val request = reqSnap.getValue(ServiceRequest::class.java)
                        if (request != null) {
                            request.id = reqSnap.key ?: ""
                            requestList.add(request)
                        }
                    }
                    // NEW: Apply filters after initial load
                    applyFilters()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UserActivityActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // NEW: Function to apply all active filters
    private fun applyFilters() {
        filteredList.clear()

        for (request in requestList) {
            // 1. Status Filter (Ongoing vs. Completed)
            // A request is completed if both user and repairman confirmed the job/payment
            val isCompleted = request.userConfirmedJobDone && request.repairmanConfirmedPayment
            val statusMatches = when (currentStatusFilter) {
                "Ongoing" -> !isCompleted // Not completed
                "Completed" -> isCompleted
                else -> false // Should not happen
            }

            // 2. Category Filter
            val categoryMatches = currentCategoryFilter == "All Categories" ||
                    request.serviceType.equals(currentCategoryFilter, ignoreCase = true)

            if (statusMatches && categoryMatches) {
                filteredList.add(request)
            }
        }

        // Sort by date (newest first)
        filteredList.sortByDescending { it.dateMillis }

        adapter.notifyDataSetChanged()
    }

    private fun handleJobDoneConfirmation(request: ServiceRequest) {
        val updates = HashMap<String, Any>()
        updates["userConfirmedJobDone"] = true
        updates["status"] = "Job Confirmed by User"

        dbRef.child(request.id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Job completion confirmed. Awaiting repairman's payment confirmation.", Toast.LENGTH_LONG).show()
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to confirm job completion", Toast.LENGTH_SHORT).show()
            }
    }

    private fun launchRatingActivity(request: ServiceRequest) {
        val intent = Intent(this, RateActivity::class.java)
        intent.putExtra("requestId", request.id)
        intent.putExtra("userId", request.userId)
        intent.putExtra("repairmanId", request.repairmanId)
        startActivity(intent)
    }
}