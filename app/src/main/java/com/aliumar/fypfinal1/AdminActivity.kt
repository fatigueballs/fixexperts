package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_requests) // This layout is now the button dashboard

        // Back button functions as logout for admin
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            Toast.makeText(this, "Admin Logged Out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Set up button click listeners
        findViewById<Button>(R.id.btnViewRepairmen).setOnClickListener {
            startActivity(Intent(this, AdminViewRepairmenActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewUsers).setOnClickListener {
            startActivity(Intent(this, AdminViewUsersActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewRequests).setOnClickListener {
            startActivity(Intent(this, AdminViewRequestsActivity::class.java))
        }
    }
}