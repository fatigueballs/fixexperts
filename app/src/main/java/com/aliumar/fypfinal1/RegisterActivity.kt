package com.aliumar.fypfinal1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import android.content.Intent
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val inputUsername = findViewById<EditText>(R.id.registerUsername)
        val inputPassword = findViewById<EditText>(R.id.registerPassword)
        val inputEmail = findViewById<EditText>(R.id.registerEmail)
        val isRepairman = findViewById<CheckBox>(R.id.repairmanTrue)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)

        val db = FirebaseDatabase.getInstance()
        val usersRef = db.getReference("users")
        val repairmenRef = db.getReference("repairmen")

        buttonRegister.setOnClickListener {
            val username = inputUsername.text.toString()
            val email = inputEmail.text.toString()
            val password = inputPassword.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isRepairman.isChecked) {
                val input = Repairman(username, email, password)
                repairmenRef.child(username).setValue(input)
                Toast.makeText(this, "Repairman registered successfully", Toast.LENGTH_SHORT).show()
            } else {
                val input = User(username, email, password)
                usersRef.child(username).setValue(input)
                Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()

    }
}
}


