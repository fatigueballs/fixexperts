package com.aliumar.fypfinal1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import android.content.Intent
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var userRef: DatabaseReference
    private lateinit var repairmenRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val loginEmail = findViewById<EditText>(R.id.loginEmail)
        val loginPassword = findViewById<EditText>(R.id.loginPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        val database = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("users")
        repairmenRef = database.getReference("repairmen")

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
                loginUser(email, password)
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        userRef.get().addOnSuccessListener { snapshot ->
            var found = false
            for (userSnapshot in snapshot.children) {
                val userEmail = userSnapshot.child("email").value.toString()
                val userPassword = userSnapshot.child("password").value.toString()

                if (email == userEmail && password == userPassword) {
                    found = true
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
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
                val repairEmail = repairmanSnapshot.child("email").value.toString()
                val repairPassword = repairmanSnapshot.child("password").value.toString()

                if (email == repairEmail && password == repairPassword) {
                    found = true
                    Toast.makeText(this, "Repairman Login Successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, RegisterActivity::class.java)
                    startActivity(intent)
                    finish()
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

}
