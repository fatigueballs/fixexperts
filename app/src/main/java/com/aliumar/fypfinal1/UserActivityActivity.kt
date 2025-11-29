package com.aliumar.fypfinal1

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class UserActivityActivity : AppCompatActivity() {

    private lateinit var recyclerViewRequests: RecyclerView
    private lateinit var tabLayoutStatus: TabLayout
    private lateinit var spinnerCategory: Spinner

    private lateinit var requestList: MutableList<ServiceRequest>
    private lateinit var filteredList: MutableList<ServiceRequest>
    private lateinit var adapter: UserRequestAdapter
    private lateinit var dbRef: DatabaseReference
    private var actualUserId: String? = null
    private var selectedRequestIdForUpload: String? = null

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

    // Image Picker
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImageToFirebase(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_activity)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        recyclerViewRequests = findViewById(R.id.recyclerViewUserRequests)
        recyclerViewRequests.layoutManager = LinearLayoutManager(this)
        tabLayoutStatus = findViewById(R.id.tabLayoutStatus)
        spinnerCategory = findViewById(R.id.spinnerCategory)

        requestList = mutableListOf()
        filteredList = mutableListOf()

        adapter = UserRequestAdapter(
            filteredList,
            onConfirmPayment = { /* Old Logic kept for safety */ },
            onRate = { request -> launchRatingActivity(request) },
            onChat = { request ->
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("REQUEST_ID", request.id)
                intent.putExtra("CURRENT_USER_ID", actualUserId)
                intent.putExtra("OTHER_USER_NAME", request.repairmanName)
                startActivity(intent)
            },
            onUploadPayment = { request ->
                selectedRequestIdForUpload = request.id
                getContent.launch("image/*")
            }
        )

        recyclerViewRequests.adapter = adapter
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        setupCategorySpinner()
        setupStatusTabs()
        resolveUserIdAndLoadRequests()
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

    private fun resolveUserIdAndLoadRequests() {
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val loggedInEmail = sharedPref.getString("email", null)
        if (loggedInEmail == null) return

        val database = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val userRef = database.getReference("users")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (uSnap in snapshot.children) {
                    val user = uSnap.getValue(User::class.java)
                    if (user != null && user.email.equals(loggedInEmail, ignoreCase = true)) {
                        actualUserId = uSnap.key
                        found = true
                        loadRequests()
                        break
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadRequests() {
        val currentUserId = actualUserId ?: return
        dbRef.orderByChild("userId").equalTo(currentUserId)
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
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun applyFilters() {
        filteredList.clear()
        for (request in requestList) {
            // Updated Completed logic
            val isJobFullyDone = request.workProofUrl.isNotEmpty() && request.paymentProofUrl.isNotEmpty()
            val isCompleted = request.userRated || isJobFullyDone

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

    private fun uploadImageToFirebase(imageUri: Uri) {
        val reqId = selectedRequestIdForUpload ?: return
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading Payment Receipt...")
        progressDialog.show()

        val fileName = "payment_proof/${UUID.randomUUID()}.jpg"
        val storageRef = FirebaseStorage.getInstance().getReference(fileName)

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val url = uri.toString()
                    dbRef.child(reqId).child("paymentProofUrl").setValue(url)
                    dbRef.child(reqId).child("status").setValue("Payment Uploaded (Waiting for Rating)")
                    progressDialog.dismiss()
                    Toast.makeText(this, "Payment receipt uploaded!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun launchRatingActivity(request: ServiceRequest) {
        val intent = Intent(this, RateActivity::class.java)
        intent.putExtra("requestId", request.id)
        intent.putExtra("userId", request.userId)
        intent.putExtra("repairmanId", request.repairmanId)
        startActivity(intent)
    }
}