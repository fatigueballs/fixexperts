package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class ActivityFragment : Fragment() {

    private lateinit var recyclerViewRequests: RecyclerView
    private lateinit var tabLayoutStatus: TabLayout
    private lateinit var spinnerCategory: Spinner

    private lateinit var requestList: MutableList<ServiceRequest>
    private lateinit var filteredList: MutableList<ServiceRequest>
    private lateinit var adapter: UserRequestAdapter
    private lateinit var dbRef: DatabaseReference
    private var actualUserId: String? = null

    // Variables for Image Upload
    private var selectedRequestForUpload: ServiceRequest? = null

    // Image Picker Launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadPaymentProof(it) }
    }

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewRequests = view.findViewById(R.id.recyclerViewUserRequests)
        recyclerViewRequests.layoutManager = LinearLayoutManager(requireContext())
        tabLayoutStatus = view.findViewById(R.id.tabLayoutStatus)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)

        requestList = mutableListOf()
        filteredList = mutableListOf()

        // --- UPDATED ADAPTER INITIALIZATION ---
        adapter = UserRequestAdapter(
            filteredList,
            onConfirmPayment = { request -> handleJobDoneConfirmation(request) },
            onRate = { request -> launchRatingActivity(request) },
            onChat = { request ->
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("REQUEST_ID", request.id)
                intent.putExtra("CURRENT_USER_ID", actualUserId)
                intent.putExtra("OTHER_USER_NAME", request.repairmanName)
                startActivity(intent)
            },
            // FIX: Add the missing 4th parameter here
            onUploadPayment = { request ->
                selectedRequestForUpload = request
                imagePickerLauncher.launch("image/*") // Open Gallery
            }
        )

        recyclerViewRequests.adapter = adapter
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        setupCategorySpinner()
        setupStatusTabs()

        resolveUserIdAndLoadRequests()
    }

    // ... (Your existing setupCategorySpinner, setupStatusTabs, resolveUserId, and loadRequests methods remain exactly the same) ...

    private fun setupCategorySpinner() {
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ALL_SERVICES_FILTER)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

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
        val sharedPref = activity?.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val loggedInEmail = sharedPref?.getString("email", null)

        if (loggedInEmail == null) {
            Toast.makeText(requireContext(), "Error: Login information missing.", Toast.LENGTH_LONG).show()
            return
        }

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
                if (!found) {
                    Toast.makeText(requireContext(), "User not found in database.", Toast.LENGTH_LONG).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to resolve ID: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadRequests() {
        val currentUserId = actualUserId
        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Could not retrieve User ID.", Toast.LENGTH_SHORT).show()
            return
        }

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
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load requests", Toast.LENGTH_SHORT).show()
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

    private fun handleJobDoneConfirmation(request: ServiceRequest) {
        val updates = HashMap<String, Any>()
        updates["userConfirmedJobDone"] = true
        updates["status"] = "Job Confirmed by User"

        dbRef.child(request.id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Job completion confirmed.", Toast.LENGTH_LONG).show()
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to confirm job completion", Toast.LENGTH_SHORT).show()
            }
    }

    private fun launchRatingActivity(request: ServiceRequest) {
        val intent = Intent(activity, RateActivity::class.java).apply {
            putExtra("requestId", request.id)
            putExtra("userId", request.userId)
            putExtra("repairmanId", request.repairmanId)
        }
        startActivity(intent)
    }

    // --- NEW: UPLOAD LOGIC ---
    private fun uploadPaymentProof(imageUri: Uri) {
        val request = selectedRequestForUpload ?: return

        Toast.makeText(requireContext(), "Uploading Receipt...", Toast.LENGTH_SHORT).show()

        val storageRef = FirebaseStorage.getInstance().reference
            .child("proofs/payment_${request.id}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updatePaymentStatus(request.id, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePaymentStatus(requestId: String, imageUrl: String) {
        val updates = HashMap<String, Any>()
        updates["paymentProofImage"] = imageUrl
        updates["userConfirmedJobDone"] = true
        updates["status"] = "Payment Sent - Awaiting Confirmation"

        dbRef.child(requestId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Receipt Sent Successfully!", Toast.LENGTH_LONG).show()
                // List will refresh automatically due to addValueEventListener
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Database update failed", Toast.LENGTH_SHORT).show()
            }
    }
}