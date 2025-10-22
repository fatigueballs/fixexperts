package com.aliumar.fypfinal1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class UserDashboardActivity : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val repairmenRef = database.getReference("repairmen")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        // Ensure these match the service names used elsewhere
        findViewById<Button>(R.id.buttonAC).setOnClickListener { showRepairmen("Air Conditioning Fix") }
        findViewById<Button>(R.id.buttonGas).setOnClickListener { showRepairmen("Gas Tank Replacement") }
        findViewById<Button>(R.id.buttonPlumbing).setOnClickListener { showRepairmen("Plumbing") }
        findViewById<Button>(R.id.buttonKitchen).setOnClickListener { showRepairmen("Kitchen Appliance Fix") }
        findViewById<Button>(R.id.buttonElectrician).setOnClickListener { showRepairmen("Electrician / Wiring") } // Note: Using the name from UserServiceSelectionActivity
        findViewById<Button>(R.id.buttonCleaning).setOnClickListener { showRepairmen("Cleaning Service") }
    }

    private fun showRepairmen(category: String) {
        // FIX: Redirect all flows to ChooseRepairmanActivity, which has the correct request logic
        val intent = Intent(this, ChooseRepairmanActivity::class.java)
        // FIX: Use SERVICE_TYPE key for consistency with ChooseRepairmanActivity
        intent.putExtra("SERVICE_TYPE", category)
        startActivity(intent)
    }
}
