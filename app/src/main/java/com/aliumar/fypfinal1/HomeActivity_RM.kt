package com.aliumar.fypfinal1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

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

        // If email is missing, go to login immediately
        if (loggedInEmail == null) {
            Toast.makeText(this, "Authentication error. Please re-login.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

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

    // Removed checkRepairmanSetup(), showMainContent(), and removeSetupListener() as they are no longer needed.
    // The forced redirection logic is completely removed.

    override fun onDestroy() {
        super.onDestroy()
        // No listener to remove, clean up complete.
    }
}