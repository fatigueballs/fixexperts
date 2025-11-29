package com.aliumar.fypfinal1

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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
    private var selectedRequestForImage: ServiceRequest? = null
    private var isUploadingWorkProof = true

    private var currentStatusFilter: String = "Ongoing"
    private var currentCategoryFilter: String = "All Categories"

    private val ALL_SERVICES_FILTER = listOf(
        "All Categories", "Air Conditioning Fix", "Gas Tank Replacement",
        "Plumbing", "Kitchen Appliance Fix", "Electrician / Wiring", "Cleaning Service"
    )

    // Image Picker
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uploadImageToFirebase(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repairman_requests)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("serviceRequests")

        recyclerViewRequests = findViewById(R.id.recyclerViewRequests)
        tabLayoutStatus = findViewById(R.id.tabLayoutStatus)
        spinnerCategory = findViewById(R.id.spinnerCategory)

        requestList = mutableListOf()
        filteredList = mutableListOf()

        recyclerViewRequests.layoutManager = LinearLayoutManager(this)
        adapter = RequestAdapter(filteredList) { request, action ->
            handleRequestAction(request, action)
        }
        recyclerViewRequests.adapter = adapter

        setupCategorySpinner()
        setupStatusTabs()
        resolveRepairmanIdAndLoadRequests()
    }

    private fun handleRequestAction(request: ServiceRequest, action: String) {
        if (action == "chat") {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("REQUEST_ID", request.id)
            intent.putExtra("CURRENT_USER_ID", actualRepairmanId)
            intent.putExtra("OTHER_USER_NAME", request.userName)
            startActivity(intent)
            return
        }

        // Handle Action Buttons
        val updates = HashMap<String, Any>()

        when (action) {
            "accept" -> updates["status"] = "Accepted"
            "decline" -> updates["status"] = "Declined"
            "upload_work" -> {
                // Not used in Adapter explicitly but good for future extension
                selectedRequestForImage = request
                isUploadingWorkProof = true
                imagePickerLauncher.launch("image/*")
                return
            }
            "confirm_payment" -> {
                // Only allow if user has uploaded receipt
                if (request.paymentProofImage.isEmpty()) {
                    Toast.makeText(this, "User has not uploaded payment receipt yet.", Toast.LENGTH_SHORT).show()
                    return
                }
                updates["repairmanConfirmedPayment"] = true
                updates["status"] = "Completed"
                updates["userConfirmedJobDone"] = true
            }
        }

        dbRef.child(request.id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Request Updated!", Toast.LENGTH_SHORT).show()
                // loadRequests() will trigger automatically via listener
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val request = selectedRequestForImage ?: return
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading Proof...")
        progressDialog.show()

        val fileName = "work_proof/${UUID.randomUUID()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child(fileName)

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val updates = HashMap<String, Any>()
                    updates["workProofImage"] = uri.toString()
                    updates["status"] = "Work Proof Sent"

                    dbRef.child(request.id).updateChildren(updates)
                    progressDialog.dismiss()
                    Toast.makeText(this, "Proof Uploaded!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Standard Setup Functions (Same as before) ---
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ALL_SERVICES_FILTER)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                currentCategoryFilter = ALL_SERVICES_FILTER[pos]
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

    // ... resolveRepairmanIdAndLoadRequests and loadRequests remain the same ...
    // (Ensure you have these methods in your class or copy them from your existing file if omitted here for brevity)

    private fun resolveRepairmanIdAndLoadRequests() {
        val user = auth.currentUser
        if (user == null) { return }
        // Assuming you look up repairman ID from users table similar to ActivityFragment
        // For simplicity, using UID directly or your logic:
        val database = FirebaseDatabase.getInstance("https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val ref = database.getReference("users")
        ref.orderByChild("email").equalTo(user.email).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(child in snapshot.children) {
                    actualRepairmanId = child.key
                    loadRequests()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadRequests() {
        val rId = actualRepairmanId ?: return
        dbRef.orderByChild("repairmanId").equalTo(rId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                requestList.clear()
                for (snap in snapshot.children) {
                    val req = snap.getValue(ServiceRequest::class.java)
                    if (req != null) {
                        req.id = snap.key ?: ""
                        requestList.add(req)
                    }
                }
                applyFilters()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun applyFilters() {
        filteredList.clear()
        for (req in requestList) {
            val isCompleted = req.repairmanConfirmedPayment
            val statusMatches = when (currentStatusFilter) {
                "Ongoing" -> !isCompleted
                "Completed" -> isCompleted
                else -> false
            }
            val catMatches = currentCategoryFilter == "All Categories" || req.serviceType == currentCategoryFilter

            if (statusMatches && catMatches) filteredList.add(req)
        }
        filteredList.sortByDescending { it.dateMillis }
        adapter.notifyDataSetChanged()
    }
}