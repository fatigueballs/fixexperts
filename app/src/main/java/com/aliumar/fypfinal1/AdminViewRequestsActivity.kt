package com.aliumar.fypfinal1

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AdminViewRequestsActivity : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var requestsRef: DatabaseReference

    private lateinit var spinnerCategory: Spinner
    private lateinit var recyclerView: RecyclerView

    private lateinit var allRequestsList: MutableList<ServiceRequest>
    private lateinit var filteredRequestsList: MutableList<ServiceRequest>
    private lateinit var adapter: AdminRequestAdapter

    private var currentCategoryFilter: String = "All Categories"

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
        setContentView(R.layout.activity_admin_view_requests)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        requestsRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("serviceRequests")
        allRequestsList = mutableListOf()
        filteredRequestsList = mutableListOf()

        adapter = AdminRequestAdapter(filteredRequestsList)
        recyclerView = findViewById(R.id.recyclerViewAllRequests)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupCategorySpinner()
        loadAllRequests()
    }

    private fun setupCategorySpinner() {
        spinnerCategory = findViewById(R.id.spinnerCategory)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ALL_SERVICES_FILTER)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentCategoryFilter = ALL_SERVICES_FILTER[position]
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadAllRequests() {
        requestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allRequestsList.clear()
                for (rSnap in snapshot.children) {
                    val request = rSnap.getValue(ServiceRequest::class.java)
                    if (request != null) {
                        request.id = rSnap.key ?: ""
                        allRequestsList.add(request)
                    }
                }
                applyFilter() // Apply initial filter (which is "All Categories")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminViewRequestsActivity, "Failed to load requests: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun applyFilter() {
        filteredRequestsList.clear()

        if (currentCategoryFilter == "All Categories") {
            filteredRequestsList.addAll(allRequestsList)
        } else {
            for (request in allRequestsList) {
                if (request.serviceType.equals(currentCategoryFilter, ignoreCase = true)) {
                    filteredRequestsList.add(request)
                }
            }
        }

        // Sort by newest first
        filteredRequestsList.sortByDescending { it.dateMillis }
        adapter.notifyDataSetChanged()
    }
}