package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView // Import TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot // Import Firebase classes
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeActivity_User : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_user)

        val tvGreeting = findViewById<TextView>(R.id.tvWelcomeGreeting)
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val loggedInEmail = sharedPref.getString("email", null)

        if (loggedInEmail != null) {
            // New: Fetch and set the actual username in the greeting for a user
            fetchAndSetGreeting(loggedInEmail, "users", tvGreeting)
        } else {
            tvGreeting.text = "Welcome, Guest!"
        }

        val btnRequest = findViewById<Button>(R.id.buttonRequestService)
        btnRequest.setOnClickListener {
            startActivity(Intent(this, UserServiceSelectionActivity::class.java))
        }

        // NEW: Button to view service activity
        val btnViewActivity = findViewById<Button>(R.id.buttonViewActivity)
        btnViewActivity.setOnClickListener {
            startActivity(Intent(this, UserActivityActivity::class.java))
        }

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
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
                        val user = snap.getValue(User::class.java)
                        userEmail = user?.email
                        username = user?.username
                    } else {
                        // For a generic lookup, though typically split by activity
                        val repairman = snap.getValue(Repairman::class.java)
                        userEmail = repairman?.email
                        username = repairman?.username
                    }

                    if (userEmail.equals(email, ignoreCase = true) && username != null) {
                        textView.text = "Welcome, $username!"
                        return
                    }
                }
                textView.text = "Welcome, User!" // Fallback if username is not found
            }

            override fun onCancelled(error: DatabaseError) {
                textView.text = "Welcome!" // Fallback on error
            }
        })
    }
}