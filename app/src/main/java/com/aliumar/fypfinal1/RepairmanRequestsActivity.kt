package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RepairmanRequestsActivity : AppCompatActivity() {

    private lateinit var recyclerViewRequests: RecyclerView
    private lateinit var requestList: MutableList<ServiceRequest>
    private lateinit var adapter: RequestAdapter
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var actualRepairmanId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repairman_requests)

        recyclerViewRequests = findViewById(R.id.recyclerViewRequests)
        recyclerViewRequests.layoutManager = LinearLayoutManager(this)

        requestList = mutableListOf()
        adapter = RequestAdapter(requestList) { request, action ->
            handleRequestAction(request, action)
        }

        recyclerViewRequests.adapter = adapter

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        resolveRepairmanIdAndLoadRequests()
    }

    private fun resolveRepairmanIdAndLoadRequests() {
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
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RepairmanRequestsActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleRequestAction(request: ServiceRequest, action: String) {
        val newStatus = when (action) {
            "accept" -> "Accepted"
            "decline" -> "Declined"
            "done" -> "Completed"
            else -> request.status
        }

        dbRef.child(request.id).child("status").setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Request $newStatus", Toast.LENGTH_SHORT).show()

                if (newStatus == "Completed") {
                    val intent = Intent(this, RateActivity::class.java)
                    intent.putExtra("requestId", request.id)
                    intent.putExtra("userId", request.userId)
                    intent.putExtra("repairmanId", request.repairmanId)
                    startActivity(intent)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update request", Toast.LENGTH_SHORT).show()
            }
    }
}
