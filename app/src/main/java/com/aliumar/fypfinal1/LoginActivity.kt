package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var userRef: DatabaseReference
    private lateinit var repairmenRef: DatabaseReference
    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase references (test)
        val database = FirebaseDatabase.getInstance(
            "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        userRef = database.getReference("users")
        repairmenRef = database.getReference("repairmen")

        // Link layout components
        loginEmail = findViewById(R.id.loginEmail)
        loginPassword = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)

        // Check if user already logged in
        checkSavedLogin()

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val email = loginEmail.text.toString().trim()
            val password = loginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                checkAdminLogin(email, password)
            }
        }
    }

    private fun checkAdminLogin(email: String, password: String) {
        // Hardcoded Admin Credentials
        if (email == "admin@fixexperts.com" && password == "admin123") {
            saveLoginInfo(email, "admin")
            Toast.makeText(this, "Admin Login Successful!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AdminActivity::class.java)) // NEW ADMIN ACTIVITY
            finish()
            return
        }
        // If not admin, proceed to check regular users
        loginUser(email, password)
    }

    private fun loginUser(email: String, password: String) {
        userRef.get().addOnSuccessListener { snapshot ->
            var found = false
            for (userSnapshot in snapshot.children) {
                val userEmail = userSnapshot.child("email").value?.toString()
                val userPassword = userSnapshot.child("password").value?.toString()

                if (email == userEmail && password == userPassword) {
                    found = true
                    saveLoginInfo(email, "user")
                    Toast.makeText(this, "User Login Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity_User::class.java))
                    finish()
                    break
                }
            }
            if (!found) {
                checkRepairmanLogin(email, password)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkRepairmanLogin(email: String, password: String) {
        repairmenRef.get().addOnSuccessListener { snapshot ->
            var found = false
            for (repairmanSnapshot in snapshot.children) {
                val repairEmail = repairmanSnapshot.child("email").value?.toString()
                val repairPassword = repairmanSnapshot.child("password").value?.toString()
                // NEW: Read the approval status
                val isApproved = repairmanSnapshot.child("isApprovedByAdmin").value as? Boolean ?: false

                if (email == repairEmail && password == repairPassword) {
                    found = true
                    if (isApproved) { // <--- CORE CHECK: ONLY ALLOW LOGIN IF APPROVED
                        saveLoginInfo(email, "repairman")
                        Toast.makeText(this, "Repairman Login Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity_RM::class.java))
                        finish()
                    } else {
                        // <--- BLOCK LOGIN AND INFORM USER
                        Toast.makeText(this, "Account not yet verified by Admin. Please wait for approval.", Toast.LENGTH_LONG).show()
                    }
                    break
                }
            }
            if (!found) {
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLoginInfo(email: String, userType: String) {
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("email", email)
            putString("userType", userType)
            apply()
        }
    }

    private fun checkSavedLogin() {
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("email", null)
        val userType = sharedPref.getString("userType", null)

        if (savedEmail != null && userType != null) {
            when (userType) {
                "user" -> startActivity(Intent(this, HomeActivity_User::class.java))
                "repairman" -> startActivity(Intent(this, HomeActivity_RM::class.java))
                "admin" -> startActivity(Intent(this, AdminActivity::class.java))
            }
            finish()
        }
    }
}