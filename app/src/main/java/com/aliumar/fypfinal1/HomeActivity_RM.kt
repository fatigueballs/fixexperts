package com.aliumar.fypfinal1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton // IMPORT ADDED
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity_RM : AppCompatActivity() {

    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var repairmenRef: DatabaseReference
    private var loggedInEmail: String? = null
    private var repairmanId: String? = null

    // Calendar vars
    private lateinit var tvMonthYear: TextView
    private lateinit var recyclerCalendar: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private val calendar = Calendar.getInstance()

    // List to hold requests for highlighting
    private val allRequests = mutableListOf<ServiceRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_rm)

        repairmenRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("repairmen")

        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        loggedInEmail = sharedPref.getString("email", null)

        val tvGreeting = findViewById<TextView>(R.id.tvWelcomeGreeting)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        recyclerCalendar = findViewById(R.id.recyclerCalendar)

        // Setup Recycler Grid (7 columns for days of week)
        recyclerCalendar.layoutManager = GridLayoutManager(this, 7)

        if (loggedInEmail == null) {
            Toast.makeText(this, "Authentication error. Please re-login.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        fetchAndSetGreeting(loggedInEmail!!, "repairmen", tvGreeting)

        // 1. Find Repairman ID first, then load requests
        findRepairmanIdAndLoadRequests()

        // --- UPDATED: Calendar Navigation (Changed to ImageButton) ---
        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendarUI()
        }
        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendarUI()
        }

        // Initialize Calendar UI
        updateCalendarUI()

        // Button Listeners

        // This remains a standard Button
        findViewById<Button>(R.id.buttonViewRequests).setOnClickListener {
            startActivity(Intent(this, RepairmanRequestsActivity::class.java))
        }

        // --- UPDATED: Edit Profile (Now an Icon in Header) ---
        // Changed ID to btnEditProfile and type to ImageButton
        findViewById<ImageButton>(R.id.buttonEditProfile).setOnClickListener {
            startActivity(Intent(this, RepairmanSetupActivity::class.java))
        }

        // --- UPDATED: Logout (Now an Icon in Header) ---
        // Changed type to ImageButton
        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun findRepairmanIdAndLoadRequests() {
        repairmenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children) {
                    val r = snap.getValue(Repairman::class.java)
                    if (r != null && r.email.equals(loggedInEmail, ignoreCase = true)) {
                        repairmanId = snap.key
                        listenForRequests() // Start fetching requests
                        return
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForRequests() {
        if (repairmanId == null) return
        val requestsRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("serviceRequests")

        // Fetch all requests for this repairman
        requestsRef.orderByChild("repairmanId").equalTo(repairmanId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allRequests.clear()
                    for (snap in snapshot.children) {
                        val req = snap.getValue(ServiceRequest::class.java)
                        if (req != null) {
                            allRequests.add(req)
                        }
                    }
                    updateCalendarUI() // Refresh calendar with new data
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateCalendarUI() {
        // 1. Update Month Text
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthYear.text = sdf.format(calendar.time)

        // 2. Generate days for the grid
        val daysInMonth = ArrayList<CalendarAdapter.CalendarDay>()
        val tempCal = calendar.clone() as Calendar

        // Go to 1st day of month
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        // Get day of week (Sunday = 1, Monday = 2...)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val emptySlots = firstDayOfWeek - 1

        for (i in 0 until emptySlots) {
            daysInMonth.add(CalendarAdapter.CalendarDay("", null))
        }

        val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Loop through days 1..30/31
        for (i in 1..maxDay) {
            val dayString = i.toString()
            var statusForDay: String? = null

            tempCal.set(Calendar.DAY_OF_MONTH, i)

            val currentDayStart = startOfDay(tempCal)
            val currentDayEnd = endOfDay(tempCal)

            for (req in allRequests) {
                if (req.dateMillis >= currentDayStart && req.dateMillis <= currentDayEnd) {
                    if (req.status == "Accepted") {
                        statusForDay = "Accepted"
                        break
                    } else if (req.status == "Pending") {
                        statusForDay = "Pending"
                    }
                }
            }

            daysInMonth.add(CalendarAdapter.CalendarDay(dayString, statusForDay))
        }

        calendarAdapter = CalendarAdapter(daysInMonth)
        recyclerCalendar.adapter = calendarAdapter
    }

    private fun startOfDay(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun endOfDay(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    private fun fetchAndSetGreeting(email: String, node: String, textView: TextView) {
        val dbRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference(node)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children) {
                    val repairman = snap.getValue(Repairman::class.java)
                    if (repairman?.email.equals(email, ignoreCase = true)) {
                        textView.text = "Welcome, ${repairman?.username}!"
                        return
                    }
                }
                textView.text = "Welcome, Repairman!"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}