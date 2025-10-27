package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class HomeActivity_User : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_user)

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
            val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

    }
}
