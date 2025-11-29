package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
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

    // Image Upload Logic
    private var selectedRequestForImage: ServiceRequest? = null
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri ->
            selectedRequestForImage?.let { request ->
                uploadPaymentProof(imageUri, request)
            }
        }
    }

    // ... [Filters and Other variables same as before] ...
    private var currentStatusFilter: String = "Ongoing"
    private var currentCategoryFilter: String = "All Categories"
    private val ALL_SERVICES_FILTER = listOf("All Categories", "Air Conditioning Fix", "Gas Tank Replacement", "Plumbing", "Kitchen Appliance Fix", "Electrician / Wiring", "Cleaning Service")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        adapter = UserRequestAdapter(
            filteredList,
            { request -> handleJobDoneConfirmation(request) },
            { request -> launchRatingActivity(request) },
            { request ->
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("REQUEST_ID", request.id)
                intent.putExtra("CURRENT_USER_ID", actualUserId)
                intent.putExtra("OTHER_USER_NAME", request.repairmanName)
                startActivity(intent)
            },
            { request ->
                // Upload Payment Logic
                selectedRequestForImage = request
                getContent.launch("image/*")
            }
        )

        recyclerViewRequests.adapter = adapter
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        setupCategorySpinner()
        setupStatusTabs()
        resolveUserIdAndLoadRequests()
    }

    private fun uploadPaymentProof(imageUri: android.net.Uri, request: ServiceRequest) {
        val progressDialog = android.app.ProgressDialog(requireContext())
        progressDialog.setTitle("Uploading Payment Proof...")
        progressDialog.show()

        val storageRef = FirebaseStorage.getInstance().reference.child("uploads/payment_${request.id}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    progressDialog.dismiss()
                    val updates = HashMap<String, Any>()
                    updates["userPaymentProofUrl"] = uri.toString()
                    dbRef.child(request.id).updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Payment Proof Uploaded", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Upload Failed", Toast.LENGTH_SHORT).show()
            }
    }

    // ... [Rest of methods: setupCategorySpinner, setupStatusTabs, resolveUserIdAndLoadRequests, loadRequests, applyFilters, handleJobDoneConfirmation, launchRatingActivity remain same] ...
    // Included generic placeholders for methods that didn't change logic,
    // but ensure handleJobDoneConfirmation still exists as in your previous code.
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
        val loggedInEmail = sharedPref?.getString("email", null) ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (uSnap in snapshot.children) {
                    val user = uSnap.getValue(User::class.java)
                    if (user != null && user.email.equals(loggedInEmail, ignoreCase = true)) {
                        actualUserId = uSnap.key
                        loadRequests()
                        return
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadRequests() {
        val currentUserId = actualUserId ?: return
        dbRef.orderByChild("userId").equalTo(currentUserId).addValueEventListener(object : ValueEventListener {
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
            val isCompleted = request.userConfirmedJobDone && request.repairmanConfirmedPayment
            val statusMatches = when (currentStatusFilter) {
                "Ongoing" -> !isCompleted
                "Completed" -> isCompleted
                else -> false
            }
            val categoryMatches = currentCategoryFilter == "All Categories" || request.serviceType.equals(currentCategoryFilter, ignoreCase = true)
            if (statusMatches && categoryMatches) filteredList.add(request)
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
                Toast.makeText(requireContext(), "Job confirmed. Please upload payment proof.", Toast.LENGTH_LONG).show()
                applyFilters()
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
}