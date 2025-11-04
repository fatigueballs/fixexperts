package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton // <-- IMPORT THIS
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class UserActivityActivity : AppCompatActivity() {

    private lateinit var recyclerViewRequests: RecyclerView
    private lateinit var requestList: MutableList<ServiceRequest>
    private lateinit var adapter: UserRequestAdapter
    private lateinit var dbRef: DatabaseReference
    private var actualUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_activity) // Use the new layout

        // ADDED: Back button listener
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Closes this activity
        }

        recyclerViewRequests = findViewById(R.id.recyclerViewUserRequests)
        recyclerViewRequests.layoutManager = LinearLayoutManager(this)

        requestList = mutableListOf()
        adapter = UserRequestAdapter(
            requestList,
            // RENAME/REPURPOSE: This handler now confirms the job is done by the user
            { request -> handleJobDoneConfirmation(request) },
            { request -> launchRatingActivity(request) }
        )

        recyclerViewRequests.adapter = adapter
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        resolveUserIdAndLoadRequests()
    }

    private fun resolveUserIdAndLoadRequests() {
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
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UserActivityActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // RENAMED/REPURPOSED: User now confirms the job is done
    private fun handleJobDoneConfirmation(request: ServiceRequest) {
        val updates = HashMap<String, Any>()
        // User confirms the job is physically done
        updates["userConfirmedJobDone"] = true
        // Update status for clarity, awaiting RM to confirm payment.
        updates["status"] = "Job Confirmed by User"

        dbRef.child(request.id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Job completion confirmed. Awaiting repairman's payment confirmation.", Toast.LENGTH_LONG).show()
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