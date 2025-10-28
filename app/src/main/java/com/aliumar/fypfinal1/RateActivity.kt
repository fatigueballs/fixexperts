package com.aliumar.fypfinal1

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// FIX: Add missing Firebase imports
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RateActivity : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var btnSubmit: Button

    // Define the database URL for consistent use
    private val DATABASE_URL = "https://fixexperts-database-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate)

        ratingBar = findViewById(R.id.ratingBar)
        btnSubmit = findViewById(R.id.btnSubmit)

        val userId = intent.getStringExtra("userId") ?: return
        val repairmanId = intent.getStringExtra("repairmanId") ?: return
        val requestId = intent.getStringExtra("requestId") ?: return

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            if (rating == 0f) {
                Toast.makeText(this, "Please give a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseDatabase.getInstance(DATABASE_URL).getReference("ratings")
            val ratingId = db.push().key ?: return@setOnClickListener

            val ratingData = mapOf(
                "userId" to userId,
                "repairmanId" to repairmanId,
                "rating" to rating
            )

            db.child(ratingId).setValue(ratingData)
                .addOnSuccessListener {
                    // FIX: Use the URL-specific instance for consistency
                    FirebaseDatabase.getInstance(DATABASE_URL).getReference("serviceRequests")
                        .child(requestId).child("userRated").setValue(true)

                    // NEW LOGIC: Calculate and update Repairman's average rating
                    updateRepairmanRating(repairmanId, rating)

                    Toast.makeText(this, "Rating submitted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to submit rating: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // NEW FUNCTION: Logic to fetch and update the repairman's average rating
    private fun updateRepairmanRating(repairmanId: String, newRating: Float) {
        // FIX: Use the URL-specific instance for consistency
        val repairmanRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("repairmen").child(repairmanId)

        repairmanRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val repairman = snapshot.getValue(Repairman::class.java)
                if (repairman != null) {
                    // FIX: Use optional chaining to safely get the correct fields, though
                    // Repairman data class is updated to include avgRating and ratingCount fields
                    val currentAvg = repairman.avgRating.toFloat()
                    val currentCount = repairman.ratingCount

                    // Calculate new average
                    val newRatingCount = currentCount + 1
                    val newAvgRating = ((currentAvg * currentCount) + newRating) / newRatingCount

                    // Update the repairman node
                    repairmanRef.child("avgRating").setValue(newAvgRating.toDouble())
                    repairmanRef.child("ratingCount").setValue(newRatingCount)
                        .addOnFailureListener {
                            Toast.makeText(this@RateActivity, "Failed to update repairman rating.", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this@RateActivity, "Error: Repairman data not found for ID: $repairmanId", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RateActivity, "DB error fetching repairman data: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}