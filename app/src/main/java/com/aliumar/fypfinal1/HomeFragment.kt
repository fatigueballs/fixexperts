package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var tvGreeting: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvGreeting = view.findViewById(R.id.tvGreeting)

        val sharedPref = activity?.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val loggedInEmail = sharedPref?.getString("email", null)

        if (loggedInEmail != null) {
            fetchAndSetGreeting(loggedInEmail, "users", tvGreeting)
        } else {
            tvGreeting.text = "Welcome, Guest!"
        }

        // --- Set click listeners for the grid ---

        // Helper function to create the specific intent
        fun getServiceIntent(serviceName: String): Intent {
            return Intent(activity, ChooseRepairmanActivity::class.java).apply {
                putExtra("SERVICE_TYPE", serviceName)
            }
        }

        // --- UPDATED CLICK LISTENERS ---

        view.findViewById<LinearLayout>(R.id.grid_plumbing).setOnClickListener {
            startActivity(getServiceIntent("Plumbing"))
        }

        view.findViewById<LinearLayout>(R.id.grid_aircond).setOnClickListener {
            startActivity(getServiceIntent("Air Conditioning Fix"))
        }

        view.findViewById<LinearLayout>(R.id.grid_electrical).setOnClickListener {
            startActivity(getServiceIntent("Electrician / Wiring"))
        }

        view.findViewById<LinearLayout>(R.id.grid_gas).setOnClickListener {
            startActivity(getServiceIntent("Gas Tank Replacement"))
        }

        view.findViewById<LinearLayout>(R.id.grid_appliance).setOnClickListener {
            startActivity(getServiceIntent("Kitchen Appliance Fix"))
        }

        // "View All" still goes to the main category selection page
        view.findViewById<LinearLayout>(R.id.grid_view_all).setOnClickListener {
            startActivity(Intent(activity, UserServiceSelectionActivity::class.java))
        }
    }

    // Copied directly from HomeActivity_User.kt
    private fun fetchAndSetGreeting(email: String, node: String, textView: TextView) {
        val dbRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference(node)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children) {
                    val userEmail: String?
                    val username: String?

                    if (node == "users") {
                        val user = snap.getValue(User::class.java)
                        userEmail = user?.email
                        username = user?.username
                    } else {
                        val repairman = snap.getValue(Repairman::class.java)
                        userEmail = repairman?.email
                        username = repairman?.username
                    }

                    if (userEmail.equals(email, ignoreCase = true) && username != null) {
                        textView.text = "Welcome, $username!"
                        return
                    }
                }
                textView.text = "Welcome, User!" // Fallback
            }

            override fun onCancelled(error: DatabaseError) {
                textView.text = "Welcome!" // Fallback on error
            }
        })
    }
}