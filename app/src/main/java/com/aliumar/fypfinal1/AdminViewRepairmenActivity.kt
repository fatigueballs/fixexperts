package com.aliumar.fypfinal1

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AdminViewRepairmenActivity : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var repairmenRef: DatabaseReference
    private lateinit var repairmenList: MutableList<Repairman>
    private lateinit var adapter: AdminFullRepairmanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_view_repairmen)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        repairmenRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("repairmen")
        repairmenList = mutableListOf()

        adapter = AdminFullRepairmanAdapter(this, repairmenList,
            onApproveClick = { repairman ->
                approveRepairman(repairman)
            },
            onChangeRatingClick = { repairman ->
                showChangeRatingDialog(repairman)
            },
            onViewHistoryClick = { repairman ->
                val intent = Intent(this, AdminRequestHistoryActivity::class.java)
                intent.putExtra("USER_ID", repairman.id) // Pass repairman's ID
                intent.putExtra("USER_NAME", repairman.username)
                intent.putExtra("QUERY_CHILD", "repairmanId") // Tell history activity to query by repairmanId
                startActivity(intent)
            }
        )

        findViewById<RecyclerView>(R.id.recyclerViewRepairmen).apply {
            layoutManager = LinearLayoutManager(this@AdminViewRepairmenActivity)
            adapter = this@AdminViewRepairmenActivity.adapter
        }

        loadAllRepairmen()
    }

    private fun loadAllRepairmen() {
        repairmenRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                repairmenList.clear()
                for (rSnap in snapshot.children) {
                    val repairman = rSnap.getValue(Repairman::class.java)
                    if (repairman != null) {
                        repairman.id = rSnap.key ?: ""
                        repairmenList.add(repairman)
                    }
                }
                // Sort by approval status (unapproved first)
                repairmenList.sortBy { it.isApprovedByAdmin }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminViewRepairmenActivity, "Failed to load repairmen: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun approveRepairman(repairman: Repairman) {
        if (repairman.id.isEmpty()) return
        repairmenRef.child(repairman.id).child("isApprovedByAdmin").setValue(true)
            .addOnSuccessListener {
                Toast.makeText(this, "${repairman.username} approved!", Toast.LENGTH_SHORT).show()
                // List will refresh automatically via the listener
            }
            .addOnFailureListener {
                Toast.makeText(this, "Approval failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangeRatingDialog(repairman: Repairman) {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Enter new rating (e.g., 4.5)"

        AlertDialog.Builder(this)
            .setTitle("Set Rating for ${repairman.username}")
            .setMessage("Current: ${repairman.avgRating} (${repairman.ratingCount} reviews). This will override the average.")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                val newRatingStr = input.text.toString()
                val newRating = newRatingStr.toDoubleOrNull()
                if (newRating != null && newRating >= 0.0 && newRating <= 5.0) {
                    // Admin overrides the rating. We'll set avgRating and set count to 1 (or 'Admin Override')
                    val updates = mapOf(
                        "avgRating" to newRating,
                        "ratingCount" to 1 // Or 0, to signify admin override
                    )
                    repairmenRef.child(repairman.id).updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Rating updated for ${repairman.username}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Invalid rating. Please enter a number between 0.0 and 5.0", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}