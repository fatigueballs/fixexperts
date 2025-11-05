package com.aliumar.fypfinal1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.content.Context
import android.content.Intent
import android.widget.TextView // Import TextView
import android.widget.Toast
// Add Firebase imports
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeActivity_RM : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var repairmenRef: DatabaseReference
    private var loggedInEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_rm)

        // Initialize Firebase reference
        repairmenRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("repairmen")

        // Get logged-in email
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        loggedInEmail = sharedPref.getString("email", null)

        // Find TextView for greeting
        val tvGreeting = findViewById<TextView>(R.id.tvWelcomeGreeting)

        // If email is missing, go to login immediately
        if (loggedInEmail == null) {
            Toast.makeText(this, "Authentication error. Please re-login.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // New: Fetch and set the actual repairman username in the greeting
        fetchAndSetGreeting(loggedInEmail!!, "repairmen", tvGreeting)


        // Set up button listeners directly (no more mandatory setup check)

        // 1. View Requests Button
        findViewById<Button>(R.id.buttonViewRequests).setOnClickListener {
            startActivity(Intent(this, RepairmanRequestsActivity::class.java))
        }

        // 2. NEW: Edit Profile Button (launches the setup screen for editing)
        findViewById<Button>(R.id.buttonEditProfile).setOnClickListener {
            startActivity(Intent(this, RepairmanSetupActivity::class.java))
        }

        // 3. Logout Button
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // New function to handle database lookup and UI update
    private fun fetchAndSetGreeting(email: String, node: String, textView: TextView) {
        val dbRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference(node)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children) {
                    val userEmail: String?
                    val username: String?

                    if (node == "users") {
                        // This case shouldn't happen in HomeActivity_RM
                        val user = snap.getValue(User::class.java)
                        userEmail = user?.email
                        username = user?.username
                    } else {
                        val repairman = snap.getValue(Repairman::class.java)
                        userEmail = repairman?.email
                        username = repairman?.username
                    }

                    if (userEmail.equals(email, ignoreCase = true) && username != null) {
                        textView.text = "Welcome, $username!"
                        return
                    }
                }
                textView.text = "Welcome, Repairman!" // Fallback if username is not found
            }

            override fun onCancelled(error: DatabaseError) {
                textView.text = "Welcome!" // Fallback on error
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // No listener to remove, clean up complete.
    }
}