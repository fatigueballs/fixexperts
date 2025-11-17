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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ActivityFragment : Fragment() {

    private lateinit var recyclerViewRequests: RecyclerView
    private lateinit var tabLayoutStatus: TabLayout
    private lateinit var spinnerCategory: Spinner

    private lateinit var requestList: MutableList<ServiceRequest>
    private lateinit var filteredList: MutableList<ServiceRequest>
    private lateinit var adapter: UserRequestAdapter
    private lateinit var dbRef: DatabaseReference
    private var actualUserId: String? = null

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use 'view.findViewById' to find views within the fragment's layout
        recyclerViewRequests = view.findViewById(R.id.recyclerViewUserRequests)
        recyclerViewRequests.layoutManager = LinearLayoutManager(requireContext())
        tabLayoutStatus = view.findViewById(R.id.tabLayoutStatus)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)

        requestList = mutableListOf()
        filteredList = mutableListOf()

        adapter = UserRequestAdapter(
            filteredList,
            { request -> handleJobDoneConfirmation(request) },
            { request -> launchRatingActivity(request) }
        )

        recyclerViewRequests.adapter = adapter
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        setupCategorySpinner()
        setupStatusTabs()

        resolveUserIdAndLoadRequests()
    }

    private fun setupCategorySpinner() {
        // Use requireContext() to get the context in a Fragment
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
}