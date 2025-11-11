package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AdminActivity : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var unapprovedRepairmenList: MutableList<Repairman>
    private lateinit var allRequestsList: MutableList<ServiceRequest>

    private lateinit var repairmenRef: DatabaseReference
    private lateinit var requestsRef: DatabaseReference

    private lateinit var unapprovedAdapter: AdminRepairmanAdapter
    private lateinit var requestsAdapter: AdminRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_requests)

        // Back button functions as logout for admin
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            Toast.makeText(this, "Admin Logged Out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Initialize Firebase
        repairmenRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("repairmen")
        requestsRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("serviceRequests")

        // Initialize Lists
        unapprovedRepairmenList = mutableListOf()
        allRequestsList = mutableListOf()

        // Setup Adapters
        unapprovedAdapter = AdminRepairmanAdapter(this, unapprovedRepairmenList) { repairman ->
            approveRepairman(repairman)
        }
        requestsAdapter = AdminRequestAdapter(allRequestsList)

        // Setup RecyclerViews
        findViewById<RecyclerView>(R.id.recyclerViewUnapprovedRepairmen).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = unapprovedAdapter
        }

        findViewById<RecyclerView>(R.id.recyclerViewAllRequests).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = requestsAdapter
        }

        loadData()
    }

    private fun loadData() {
        // 1. Load Unapproved Repairmen
        repairmenRef.orderByChild("isApprovedByAdmin").equalTo(false)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    unapprovedRepairmenList.clear()
                    for (rSnap in snapshot.children) {
                        val repairman = rSnap.getValue(Repairman::class.java)
                        // Double check the approval status from the object just in case the query fails.
                        if (repairman != null && repairman.isApprovedByAdmin == false) {
                            repairman.id = rSnap.key ?: ""
                            unapprovedRepairmenList.add(repairman)
                        }
                    }
                    unapprovedAdapter.notifyDataSetChanged()
                    if (unapprovedRepairmenList.isEmpty()) {
                        Toast.makeText(this@AdminActivity, "All repairmen are approved.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminActivity, "Failed to load repairmen: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })

        // 2. Load All Service Requests
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
                // Sort by newest first
                allRequestsList.sortByDescending { it.dateMillis }
                requestsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminActivity, "Failed to load requests: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun approveRepairman(repairman: Repairman) {
        if (repairman.id.isEmpty()) {
            Toast.makeText(this, "Error: Repairman ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf<String, Any>(
            "isApprovedByAdmin" to true
        )

        repairmenRef.child(repairman.id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "${repairman.username} approved successfully! They can now log in.", Toast.LENGTH_LONG).show()
                // The ValueEventListener in loadData will automatically refresh the list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to approve: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}