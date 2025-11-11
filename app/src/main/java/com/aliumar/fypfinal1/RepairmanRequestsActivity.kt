package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import android.widget.Spinner // NEW
import android.widget.AdapterView // NEW
import android.widget.ArrayAdapter // NEW
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout // NEW
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RepairmanRequestsActivity : AppCompatActivity() {

    private lateinit var recyclerViewRequests: RecyclerView
    private lateinit var tabLayoutStatus: TabLayout // NEW
    private lateinit var spinnerCategory: Spinner // NEW

    private lateinit var requestList: MutableList<ServiceRequest> // Holds all fetched requests
    private lateinit var filteredList: MutableList<ServiceRequest> // Holds requests after filtering
    private lateinit var adapter: RequestAdapter
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var actualRepairmanId: String? = null

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
        setContentView(R.layout.activity_repairman_requests)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        recyclerViewRequests = findViewById(R.id.recyclerViewRequests)
        recyclerViewRequests.layoutManager = LinearLayoutManager(this)
        tabLayoutStatus = findViewById(R.id.tabLayoutStatus) // NEW
        spinnerCategory = findViewById(R.id.spinnerCategory) // NEW

        requestList = mutableListOf()
        filteredList = mutableListOf() // Initialize filtered list

        // The adapter's click handler passes the correct action string
        adapter = RequestAdapter(filteredList) { request, action -> // Use filteredList
            handleRequestAction(request, action)
        }

        recyclerViewRequests.adapter = adapter

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        setupCategorySpinner() // NEW
        setupStatusTabs() // NEW

        resolveRepairmanIdAndLoadRequests()
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

    private fun resolveRepairmanIdAndLoadRequests() {
        // ... (existing logic to fetch repairman ID)
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val loggedInEmail = sharedPref.getString("email", null)

        if (loggedInEmail == null) {
            Toast.makeText(this, "Error: Login information missing.", Toast.LENGTH_LONG).show()
            return
        }

        // Reference to your 'repairmen' node
        val repairmenRef = FirebaseDatabase.getInstance().getReference("repairmen")

        // Find the repairman key by matching the email
        repairmenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (rSnap in snapshot.children) {
                    val repairman = rSnap.getValue(Repairman::class.java)
                    if (repairman != null && repairman.email.equals(loggedInEmail, ignoreCase = true)) {
                        actualRepairmanId = rSnap.key // Get the Firebase key
                        found = true
                        loadRequests() // Only load requests AFTER the ID is resolved
                        break
                    }
                }
                if (!found) {
                    Toast.makeText(this@RepairmanRequestsActivity, "Repairman not found in database.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RepairmanRequestsActivity, "Failed to resolve ID: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadRequests() {
        val currentRepairmanId = actualRepairmanId

        if (currentRepairmanId.isNullOrEmpty()) {
            Toast.makeText(this, "Could not retrieve Repairman ID.", Toast.LENGTH_SHORT).show()
            return
        }

        dbRef.orderByChild("repairmanId").equalTo(currentRepairmanId)
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
                    Toast.makeText(this@RepairmanRequestsActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
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
                "Ongoing" -> !isCompleted // Any request that is not fully completed
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


    private fun handleRequestAction(request: ServiceRequest, action: String) {
        val updates = HashMap<String, Any>()

        when (action) {
            "accept" -> updates["status"] = "Accepted"
            "decline" -> updates["status"] = "Declined"
            "confirm_payment" -> {
                if (!request.userConfirmedJobDone) {
                    Toast.makeText(this, "Cannot confirm payment: User has not marked the job as done.", Toast.LENGTH_LONG).show()
                    return
                }
                updates["repairmanConfirmedPayment"] = true
                updates["status"] = "Completed"
            }
            else -> return
        }

        dbRef.child(request.id).updateChildren(updates)
            .addOnSuccessListener {
                when (action) {
                    "confirm_payment" -> Toast.makeText(this, "Payment confirmed. Job is complete.", Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(this, "Request ${updates["status"]}", Toast.LENGTH_SHORT).show()
                }
                // Re-apply filters to refresh list state
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update request", Toast.LENGTH_SHORT).show()
            }
    }
}