package com.aliumar.fypfinal1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import android.content.Context
import android.content.Intent

class HomeActivity_RM : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_rm)

        val btnViewRequests = findViewById<Button>(R.id.buttonViewRequests)
        btnViewRequests.setOnClickListener {
            startActivity(Intent(this, RepairmanRequestsActivity::class.java))
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