package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var userRef: DatabaseReference
    private lateinit var repairmenRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val database = FirebaseDatabase.getInstance(
            "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        userRef = database.getReference("users")
        repairmenRef = database.getReference("repairmen")
        auth = FirebaseAuth.getInstance()

        loginEmail = findViewById(R.id.loginEmail)
        loginPassword = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        checkSavedLogin()

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
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

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")

        val input = EditText(this)
        input.hint = "Enter your email"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50
        params.rightMargin = 50
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Send") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reset link sent to your email.", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please enter an email.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun checkAdminLogin(email: String, password: String) {
        // Hardcoded Admin Credentials
        if (email == "admin@fixexperts.com" && password == "admin123") {
            saveLoginInfo(email, "admin")
            Toast.makeText(this, "Admin Login Successful!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AdminActivity::class.java))
            finish()
            return
        }
        // Use Firebase Auth for regular users/repairmen
        performAuthLogin(email, password)
    }

    private fun performAuthLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = auth.currentUser
                if (user != null && user.isEmailVerified) {
                    // Auth successful and verified -> Find role in DB
                    findUserRoleAndRedirect(email)
                } else {
                    auth.signOut()
                    Toast.makeText(this, "Please verify your email address first.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun findUserRoleAndRedirect(email: String) {
        // 1. Check Users Node
        userRef.get().addOnSuccessListener { snapshot ->
            var isUser = false
            for (child in snapshot.children) {
                val dbEmail = child.child("email").value?.toString()
                if (dbEmail.equals(email, ignoreCase = true)) {
                    isUser = true
                    saveLoginInfo(email, "user")
                    Toast.makeText(this, "User Login Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, UserMainActivity::class.java))
                    finish()
                    break
                }
            }
            // 2. If not in Users, check Repairmen Node
            if (!isUser) {
                checkRepairmanNode(email)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "DB Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkRepairmanNode(email: String) {
        repairmenRef.get().addOnSuccessListener { snapshot ->
            var isRepairman = false
            for (child in snapshot.children) {
                val dbEmail = child.child("email").value?.toString()
                if (dbEmail.equals(email, ignoreCase = true)) {
                    isRepairman = true
                    val isApproved = child.child("isApprovedByAdmin").value as? Boolean ?: false

                    if (isApproved) {
                        saveLoginInfo(email, "repairman")
                        Toast.makeText(this, "Repairman Login Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity_RM::class.java))
                        finish()
                    } else {
                        auth.signOut() // Logout if not approved
                        Toast.makeText(this, "Account pending Admin approval.", Toast.LENGTH_LONG).show()
                    }
                    break
                }
            }
            if (!isRepairman) {
                // Auth exists but data missing in DB
                auth.signOut()
                Toast.makeText(this, "User data not found in database.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "DB Error: ${it.message}", Toast.LENGTH_SHORT).show()
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
                "user" -> startActivity(Intent(this, UserMainActivity::class.java))
                "repairman" -> startActivity(Intent(this, HomeActivity_RM::class.java))
                "admin" -> startActivity(Intent(this, AdminActivity::class.java))
            }
            finish()
        }
    }
}