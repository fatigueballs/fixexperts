package com.aliumar.fypfinal1

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class RepairmanListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RepairmanAdapter
    private lateinit var repairmenList: MutableList<Repairman>

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repairman_list)

        recyclerView = findViewById(R.id.recyclerViewRepairmen)
        recyclerView.layoutManager = LinearLayoutManager(this)
        repairmenList = mutableListOf()

        adapter = RepairmanAdapter(repairmenList) { repairman ->
            showDateTimePicker(repairman)
        }

        recyclerView.adapter = adapter

        loadRepairmen()
    }

    private fun loadRepairmen() {
        database.child("repairmen").get().addOnSuccessListener { snapshot ->
            repairmenList.clear()
            for (child in snapshot.children) {
                val repairman = child.getValue(Repairman::class.java)
                if (repairman != null) {
                    repairman.id = child.key ?: ""
                    repairmenList.add(repairman)
                }
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load repairmen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDateTimePicker(repairman: Repairman) {
        val calendar = Calendar.getInstance()

        // First show date picker
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            // Then show time picker
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)

                val selectedMillis = calendar.timeInMillis
                sendServiceRequest(repairman, selectedMillis)

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun sendServiceRequest(repairman: Repairman, dateMillis: Long) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val userName = currentUser.email ?: "Unknown User"

        val request = ServiceRequest(
            id = "",
            userId = userId,
            userName = userName,
            repairmanId = repairman.id,
            repairmanName = repairman.username,
            serviceType = repairman.specialties.joinToString(", "),
            date = System.currentTimeMillis().toString(),
            dateMillis = dateMillis,
            status = "Pending",
            timestamp = System.currentTimeMillis()
        )

        val requestKey = database.child("serviceRequests").push().key
        if (requestKey == null) {
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("serviceRequests").child(requestKey).setValue(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Request sent successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show()
            }
    }
}
