package com.aliumar.fypfinal1

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageButton

class UserServiceSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_service_selection)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Closes this activity and goes back
        }

        val buttons = listOf(
            R.id.btnAircon to "Air Conditioning Fix",
            R.id.btnGas to "Gas Tank Replacement",
            R.id.btnPlumbing to "Plumbing",
            R.id.btnKitchen to "Kitchen Appliance Fix",
            R.id.btnElectrician to "Electrician / Wiring",
            R.id.btnCleaning to "Cleaning Service"
        )

        for ((id, type) in buttons) {
            findViewById<Button>(id).setOnClickListener {
                val intent = Intent(this, ChooseRepairmanActivity::class.java)
                intent.putExtra("SERVICE_TYPE", type)
                startActivity(intent)
            }
        }
    }
}
