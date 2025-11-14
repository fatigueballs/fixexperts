package com.aliumar.fypfinal1

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // NEW: Import AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AdminViewUsersActivity : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var usersRef: DatabaseReference
    private lateinit var userList: MutableList<User>
    private lateinit var userKeyMap: MutableMap<String, String>
    private lateinit var adapter: AdminUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_view_users)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        usersRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("users")
        userList = mutableListOf()
        userKeyMap = mutableMapOf()

        // MODIFIED: Add onDeleteClick lambda
        adapter = AdminUserAdapter(this, userList,
            onViewHistoryClick = { user, username ->
                val userId = userKeyMap[username]
                if (userId != null) {
                    val intent = Intent(this, AdminRequestHistoryActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("USER_NAME", user.username)
                    intent.putExtra("QUERY_CHILD", "userId")
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Error: Could not find ID for ${user.username}", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { user, username -> // NEW: Handle delete click
                val userId = userKeyMap[username]
                if (userId != null) {
                    showDeleteUserDialog(user, userId)
                } else {
                    Toast.makeText(this, "Error: Could not find ID for ${user.username}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        findViewById<RecyclerView>(R.id.recyclerViewUsers).apply {
            layoutManager = LinearLayoutManager(this@AdminViewUsersActivity)
            adapter = this@AdminViewUsersActivity.adapter
        }

        loadAllUsers()
    }

    private fun loadAllUsers() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                userKeyMap.clear()
                for (uSnap in snapshot.children) {
                    val user = uSnap.getValue(User::class.java)
                    val userKey = uSnap.key
                    if (user != null && userKey != null) {
                        userList.add(user)
                        userKeyMap[user.username] = userKey
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminViewUsersActivity, "Failed to load users: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    // NEW: Function to show delete confirmation for user
    private fun showDeleteUserDialog(user: User, userId: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to permanently delete ${user.username}? This action cannot be undone.")
            .setPositiveButton("Yes, Delete") { _, _ ->
                deleteUser(userId, user.username)
            }
            .setNegativeButton("No, Cancel", null)
            .show()
    }

    // NEW: Function to delete the user
    private fun deleteUser(userId: String, username: String) {
        usersRef.child(userId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "$username has been deleted.", Toast.LENGTH_SHORT).show()
                // The ValueEventListener will automatically update the list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}