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
import com.google.android.material.tabs.TabLayout // NEW: Import TabLayout

class AdminViewRepairmenActivity : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var repairmenRef: DatabaseReference

    // MODIFIED: Rename lists for clarity
    private lateinit var allRepairmenList: MutableList<Repairman>
    private lateinit var filteredRepairmenList: MutableList<Repairman>

    private lateinit var adapter: AdminFullRepairmanAdapter
    private lateinit var tabLayoutStatus: TabLayout // NEW: Add TabLayout variable
    private var currentFilterStatus: Boolean = false // false = Pending, true = Approved

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_view_repairmen)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        tabLayoutStatus = findViewById(R.id.tabLayoutStatus) // NEW: Find layout

        repairmenRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("repairmen")
        allRepairmenList = mutableListOf()
        filteredRepairmenList = mutableListOf() // NEW: Init filtered list

        adapter = AdminFullRepairmanAdapter(this, filteredRepairmenList, // MODIFIED: Use filtered list
            onApproveClick = { repairman ->
                approveRepairman(repairman)
            },
            onUnapproveClick = { repairman ->
                showUnapproveDialog(repairman)
            },
            onChangeRatingClick = { repairman ->
                showChangeRatingDialog(repairman)
            },
            onViewHistoryClick = { repairman ->
                val intent = Intent(this, AdminRequestHistoryActivity::class.java)
                intent.putExtra("USER_ID", repairman.id)
                intent.putExtra("USER_NAME", repairman.username)
                intent.putExtra("QUERY_CHILD", "repairmanId")
                startActivity(intent)
            },
            onDeleteClick = { repairman -> // NEW: Handle delete click
                showDeleteRepairmanDialog(repairman)
            }
        )

        findViewById<RecyclerView>(R.id.recyclerViewRepairmen).apply {
            layoutManager = LinearLayoutManager(this@AdminViewRepairmenActivity)
            adapter = this@AdminViewRepairmenActivity.adapter
        }

        setupStatusTabs() // NEW: Call tab setup
        loadAllRepairmen()
    }

    // NEW: Function to set up tab listener
    private fun setupStatusTabs() {
        tabLayoutStatus.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // 0 = Pending, 1 = Approved
                currentFilterStatus = (tab?.position == 1)
                applyFilter()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadAllRepairmen() {
        repairmenRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allRepairmenList.clear() // MODIFIED: Populate all list
                for (rSnap in snapshot.children) {
                    val repairman = rSnap.getValue(Repairman::class.java)
                    if (repairman != null) {
                        repairman.id = rSnap.key ?: ""
                        allRepairmenList.add(repairman) // MODIFIED: Add to all list
                    }
                }
                // MODIFIED: Call filter function instead of sorting here
                applyFilter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminViewRepairmenActivity, "Failed to load repairmen: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    // NEW: Function to apply filter based on tab
    private fun applyFilter() {
        filteredRepairmenList.clear()
        for (repairman in allRepairmenList) {
            if (repairman.isApprovedByAdmin == currentFilterStatus) {
                filteredRepairmenList.add(repairman)
            }
        }
        adapter.notifyDataSetChanged()
    }

    // ... (existing approveRepairman, showUnapproveDialog, unapproveRepairman, showChangeRatingDialog) ...

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

    private fun showUnapproveDialog(repairman: Repairman) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Unapproval")
            .setMessage("Are you sure you want to unapprove ${repairman.username}? This will block them from logging in.")
            .setPositiveButton("Yes, Unapprove") { _, _ ->
                unapproveRepairman(repairman)
            }
            .setNegativeButton("No, Cancel", null)
            .show()
    }

    private fun unapproveRepairman(repairman: Repairman) {
        if (repairman.id.isEmpty()) return
        repairmenRef.child(repairman.id).child("isApprovedByAdmin").setValue(false)
            .addOnSuccessListener {
                Toast.makeText(this, "${repairman.username} has been unapproved.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to unapprove: ${it.message}", Toast.LENGTH_SHORT).show()
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
                    val updates = mapOf(
                        "avgRating" to newRating,
                        "ratingCount" to 1
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

    // NEW: Function to show delete confirmation for repairman
    private fun showDeleteRepairmanDialog(repairman: Repairman) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to permanently delete ${repairman.username}? This action cannot be undone.")
            .setPositiveButton("Yes, Delete") { _, _ ->
                deleteRepairman(repairman)
            }
            .setNegativeButton("No, Cancel", null)
            .show()
    }

    // NEW: Function to delete the repairman
    private fun deleteRepairman(repairman: Repairman) {
        if (repairman.id.isEmpty()) {
            Toast.makeText(this, "Error: Cannot delete, repairman ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }
        repairmenRef.child(repairman.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "${repairman.username} has been deleted.", Toast.LENGTH_SHORT).show()
                // The ValueEventListener will automatically update the list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}