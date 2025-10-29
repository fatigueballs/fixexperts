package com.aliumar.fypfinal1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import android.content.Context
import android.content.Intent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseReference

class HomeActivity_RM : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var repairmenRef: DatabaseReference
    private var setupListener: ValueEventListener? = null
    private var loggedInEmail: String? = null
    private var isProfileSetupChecked = false

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

        // Initially hide the buttons until setup status is confirmed
        findViewById<Button>(R.id.buttonViewRequests).visibility = Button.GONE
        findViewById<Button>(R.id.btnLogout).visibility = Button.GONE

        // Check Setup Status on load using a real-time listener
        checkRepairmanSetup()

        findViewById<Button>(R.id.buttonViewRequests).setOnClickListener {
            startActivity(Intent(this, RepairmanRequestsActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun checkRepairmanSetup() {
        if (loggedInEmail.isNullOrEmpty()) return

        // Use query to find the repairman by email
        val query = repairmenRef.orderByChild("email").equalTo(loggedInEmail)

        setupListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var isSetupComplete = false
                    var repairmanKey: String? = null

                    for (rSnap in snapshot.children) {
                        val repairman = rSnap.getValue(Repairman::class.java)
                        if (repairman != null) {
                            repairmanKey = rSnap.key
                            isSetupComplete = repairman.isSetupComplete
                            break // Found the repairman
                        }
                    }

                    if (!isProfileSetupChecked) {
                        isProfileSetupChecked = true
                        if (!isSetupComplete) {
                            // Setup incomplete, redirect to setup screen
                            Toast.makeText(this@HomeActivity_RM, "Please complete your profile setup.", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@HomeActivity_RM, RepairmanSetupActivity::class.java)
                            // Clear back stack to force setup completion
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            // Setup complete, show main content
                            showMainContent()
                        }
                    } else if (isSetupComplete) {
                        // Handle case where we receive update that setup is now complete
                        showMainContent()
                        // Stop listening once setup is complete and we've verified the state
                        removeSetupListener()
                    }
                } else {
                    // This case handles if the repairman node isn't found (shouldn't happen post-login)
                    Toast.makeText(this@HomeActivity_RM, "Repairman profile data error. Logging out.", Toast.LENGTH_LONG).show()
                    val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()
                    startActivity(Intent(this@HomeActivity_RM, LoginActivity::class.java))
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity_RM, "Failed to load profile status: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        query.addValueEventListener(setupListener as ValueEventListener)
    }

    private fun showMainContent() {
        // Show the buttons when setup is complete
        findViewById<Button>(R.id.buttonViewRequests).visibility = Button.VISIBLE
        findViewById<Button>(R.id.btnLogout).visibility = Button.VISIBLE
    }

    private fun removeSetupListener() {
        setupListener?.let { listener ->
            repairmenRef.orderByChild("email").equalTo(loggedInEmail).removeEventListener(listener)
            setupListener = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeSetupListener() // Crucial to prevent leaks
    }
}
