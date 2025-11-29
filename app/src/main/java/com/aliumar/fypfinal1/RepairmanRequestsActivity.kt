package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class RepairmanRequestsActivity : AppCompatActivity() {

    private lateinit var recyclerViewRequests: RecyclerView
    private lateinit var tabLayoutStatus: TabLayout
    private lateinit var spinnerCategory: Spinner

    private lateinit var requestList: MutableList<ServiceRequest>
    private lateinit var filteredList: MutableList<ServiceRequest>
    private lateinit var adapter: RequestAdapter
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var actualRepairmanId: String? = null

    private var currentStatusFilter: String = "Ongoing"
    private var currentCategoryFilter: String = "All Categories"

    private val ALL_SERVICES_FILTER = listOf(
        "All Categories",
        "Air Conditioning Fix",
        "Gas Tank Replacement",
        "Plumbing",
        "Kitchen Appliance Fix",
        "Electrician / Wiring",
        "Cleaning Service"
    )

    private var selectedRequestForImage: ServiceRequest? = null
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri ->
            selectedRequestForImage?.let { request ->
                uploadImageToFirebase(imageUri, request, isWorkProof = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repairman_requests)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        recyclerViewRequests = findViewById(R.id.recyclerViewRequests)
        recyclerViewRequests.layoutManager = LinearLayoutManager(this)
        tabLayoutStatus = findViewById(R.id.tabLayoutStatus)
        spinnerCategory = findViewById(R.id.spinnerCategory)

        requestList = mutableListOf()
        filteredList = mutableListOf()

        adapter = RequestAdapter(filteredList) { request, action ->
            handleRequestAction(request, action)
        }

        recyclerViewRequests.adapter = adapter

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        setupCategorySpinner()
        setupStatusTabs()

        resolveRepairmanIdAndLoadRequests()
    }

    private fun uploadImageToFirebase(imageUri: android.net.Uri, request: ServiceRequest, isWorkProof: Boolean) {
        val progressDialog = android.app.ProgressDialog(this)
        progressDialog.setTitle("Uploading...")
        progressDialog.show()

        val fileName = if (isWorkProof) "work_proof_${request.id}" else "payment_proof_${request.id}"
        val storageRef = FirebaseStorage.getInstance().reference.child("uploads/$fileName.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    progressDialog.dismiss()
                    val downloadUrl = uri.toString()
                    updateRequestWithImage(request.id, downloadUrl, isWorkProof)
                }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Upload Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRequestWithImage(requestId: String, url: String, isWorkProof: Boolean) {
        val fieldToUpdate = if (isWorkProof) "repairmanProofUrl" else "userPaymentProofUrl"
        val updates = HashMap<String, Any>()
        updates[fieldToUpdate] = url

        dbRef.child(requestId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Image Uploaded Successfully", Toast.LENGTH_SHORT).show()
                // The listener in loadRequests() will automatically refresh the list
            }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ALL_SERVICES_FILTER)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentCategoryFilter = ALL_SERVICES_FILTER[position]
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupStatusTabs() {
        tabLayoutStatus.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatusFilter = if (tab?.position == 0) "Ongoing" else "Completed"
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun resolveRepairmanIdAndLoadRequests() {
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val loggedInEmail = sharedPref.getString("email", null)

        if (loggedInEmail == null) {
            Toast.makeText(this, "Error: Login information missing.", Toast.LENGTH_LONG).show()
            return
        }

        val repairmenRef = FirebaseDatabase.getInstance().getReference("repairmen")

        repairmenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (rSnap in snapshot.children) {
                    val repairman = rSnap.getValue(Repairman::class.java)
                    if (repairman != null && repairman.email.equals(loggedInEmail, ignoreCase = true)) {
                        actualRepairmanId = rSnap.key
                        found = true
                        loadRequests()
                        break
                    }
                }
                if (!found) {
                    Toast.makeText(this@RepairmanRequestsActivity, "Repairman not found in database.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RepairmanRequestsActivity, "Failed to resolve ID: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadRequests() {
        val currentRepairmanId = actualRepairmanId

        if (currentRepairmanId.isNullOrEmpty()) {
            Toast.makeText(this, "Could not retrieve Repairman ID.", Toast.LENGTH_SHORT).show()
            return
        }

        dbRef.orderByChild("repairmanId").equalTo(currentRepairmanId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requestList.clear()
                    for (reqSnap in snapshot.children) {
                        val request = reqSnap.getValue(ServiceRequest::class.java)
                        if (request != null) {
                            request.id = reqSnap.key ?: ""
                            requestList.add(request)
                        }
                    }
                    applyFilters()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RepairmanRequestsActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun applyFilters() {
        filteredList.clear()

        for (request in requestList) {
            val isCompleted = request.userConfirmedJobDone && request.repairmanConfirmedPayment
            val statusMatches = when (currentStatusFilter) {
                "Ongoing" -> !isCompleted
                "Completed" -> isCompleted
                else -> false
            }

            val categoryMatches = currentCategoryFilter == "All Categories" ||
                    request.serviceType.equals(currentCategoryFilter, ignoreCase = true)

            if (statusMatches && categoryMatches) {
                filteredList.add(request)
            }
        }
        filteredList.sortByDescending { it.dateMillis }
        adapter.notifyDataSetChanged()
    }

    private fun handleRequestAction(request: ServiceRequest, action: String) {
        // NEW: Handle Chat Action
        if (action == "chat") {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("REQUEST_ID", request.id)
            intent.putExtra("CURRENT_USER_ID", actualRepairmanId) // Use repairman ID
            intent.putExtra("OTHER_USER_NAME", request.userName) // Chatting with User
            startActivity(intent)
            return
        }

        if (action == "upload_proof") {
            selectedRequestForImage = request
            getContent.launch("image/*")
            return
        }

        val updates = HashMap<String, Any>()

        when (action) {
            "accept" -> updates["status"] = "Accepted"
            "decline" -> updates["status"] = "Declined"
            "confirm_payment" -> {
                if (!request.userConfirmedJobDone) {
                    Toast.makeText(this, "Cannot confirm payment: User has not marked the job as done.", Toast.LENGTH_LONG).show()
                    return
                }
                updates["repairmanConfirmedPayment"] = true
                updates["status"] = "Completed"
            }
            else -> return
        }

        dbRef.child(request.id).updateChildren(updates)
            .addOnSuccessListener {
                when (action) {
                    "confirm_payment" -> Toast.makeText(this, "Payment confirmed. Job is complete.", Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(this, "Request ${updates["status"]}", Toast.LENGTH_SHORT).show()
                }
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update request", Toast.LENGTH_SHORT).show()
            }
    }
}