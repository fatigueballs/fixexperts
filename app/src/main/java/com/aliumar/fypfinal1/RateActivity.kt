package com.aliumar.fypfinal1

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class RateActivity : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var btnSubmit: Button

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

            val db = FirebaseDatabase.getInstance().getReference("ratings")
            val ratingId = db.push().key ?: return@setOnClickListener

            val ratingData = mapOf(
                "userId" to userId,
                "repairmanId" to repairmanId,
                "rating" to rating
            )

            db.child(ratingId).setValue(ratingData)
                .addOnSuccessListener {
                    FirebaseDatabase.getInstance().getReference("serviceRequests")
                        .child(requestId).child("repairmanRated").setValue(true)

                    Toast.makeText(this, "Rating submitted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }
}
