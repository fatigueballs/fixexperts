package com.aliumar.fypfinal1

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AdminRequestHistoryActivity : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var requestsRef: DatabaseReference
    private lateinit var requestList: MutableList<ServiceRequest>
    private lateinit var adapter: AdminRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_request_history)

        val userId = intent.getStringExtra("USER_ID")
        val userName = intent.getStringExtra("USER_NAME")
        val queryChild = intent.getStringExtra("QUERY_CHILD") // "userId" or "repairmanId"

        if (userId == null || queryChild == null) {
            Toast.makeText(this, "Error: Missing user/repairman ID", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvTitle).text = "History for $userName"

        requestsRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("serviceRequests")
        requestList = mutableListOf()
        adapter = AdminRequestAdapter(requestList)

        findViewById<RecyclerView>(R.id.recyclerViewHistory).apply {
            layoutManager = LinearLayoutManager(this@AdminRequestHistoryActivity)
            adapter = this@AdminRequestHistoryActivity.adapter
        }

        loadHistory(queryChild, userId)
    }

    private fun loadHistory(queryChild: String, id: String) {
        requestsRef.orderByChild(queryChild).equalTo(id)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requestList.clear()
                    for (rSnap in snapshot.children) {
                        val request = rSnap.getValue(ServiceRequest::class.java)
                        if (request != null) {
                            request.id = rSnap.key ?: ""
                            requestList.add(request)
                        }
                    }
                    requestList.sortByDescending { it.dateMillis }
                    adapter.notifyDataSetChanged()
                    if (requestList.isEmpty()) {
                        Toast.makeText(this@AdminRequestHistoryActivity, "No history found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminRequestHistoryActivity, "Failed to load history: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}