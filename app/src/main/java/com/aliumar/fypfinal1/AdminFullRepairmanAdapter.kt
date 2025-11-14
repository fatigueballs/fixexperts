package com.aliumar.fypfinal1

import android.content.Context
import android.graphics.Color // Import Color for the button tint
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat // Import ContextCompat for color resource
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton // Use MaterialButton for tinting
import java.util.Locale

class AdminFullRepairmanAdapter(
    private val context: Context,
    private val repairmen: List<Repairman>,
    private val onApproveClick: (Repairman) -> Unit,
    private val onUnapproveClick: (Repairman) -> Unit,
    private val onChangeRatingClick: (Repairman) -> Unit,
    private val onViewHistoryClick: (Repairman) -> Unit,
    private val onDeleteClick: (Repairman) -> Unit // NEW: Delete click listener
) : RecyclerView.Adapter<AdminFullRepairmanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textRepairmanName)
        val email: TextView = view.findViewById(R.id.textRepairmanEmail)
        val location: TextView = view.findViewById(R.id.textRepairmanLocation)
        val rating: TextView = view.findViewById(R.id.textRepairmanRating)
        val approveBtn: MaterialButton = view.findViewById(R.id.buttonApprove)
        val changeRatingBtn: Button = view.findViewById(R.id.buttonChangeRating)
        val viewHistoryBtn: Button = view.findViewById(R.id.buttonViewHistory)
        val deleteBtn: Button = view.findViewById(R.id.buttonDelete) // NEW: Find delete button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_repairman_full, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = repairmen.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repairman = repairmen[position]
        holder.name.text = repairman.username
        holder.email.text = "Email: ${repairman.email}"

        // ... (Existing rating and geocoding logic) ...
        val ratingText = if (repairman.ratingCount > 0) {
            String.format("Rating: %.1f (%d reviews)", repairman.avgRating, repairman.ratingCount)
        } else {
            "Rating: Unrated"
        }
        holder.rating.text = ratingText

        holder.location.text = "Location: Loading..."
        Thread {
            val city = getCityFromCoordinates(repairman.latitude, repairman.longitude)
            Handler(Looper.getMainLooper()).post {
                if (holder.adapterPosition == position) {
                    holder.location.text = "Location: $city"
                }
            }
        }.start()

        // --- MODIFIED LOGIC FOR BUTTON ---
        if (repairman.isApprovedByAdmin) {
            holder.approveBtn.visibility = View.VISIBLE
            holder.approveBtn.text = "Unapprove"
            holder.approveBtn.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_red_dark)

            holder.changeRatingBtn.visibility = View.VISIBLE
            holder.viewHistoryBtn.visibility = View.VISIBLE

            holder.approveBtn.setOnClickListener { onUnapproveClick(repairman) }

        } else {
            holder.approveBtn.visibility = View.VISIBLE
            holder.approveBtn.text = "Approve"
            holder.approveBtn.backgroundTintList = ContextCompat.getColorStateList(context, R.color.primary_blue)

            holder.changeRatingBtn.visibility = View.GONE
            holder.viewHistoryBtn.visibility = View.GONE

            holder.approveBtn.setOnClickListener { onApproveClick(repairman) }
        }

        // --- END MODIFIED LOGIC ---

        // Click Listeners
        holder.changeRatingBtn.setOnClickListener { onChangeRatingClick(repairman) }
        holder.viewHistoryBtn.setOnClickListener { onViewHistoryClick(repairman) }
        holder.deleteBtn.setOnClickListener { onDeleteClick(repairman) } // NEW: Set delete listener
    }

    // ... (Existing getCityFromCoordinates function) ...
    private fun getCityFromCoordinates(lat: Double, lng: Double): String {
        if (lat == 0.0 && lng == 0.0) return "Location Not Set"
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                address.locality ?: address.adminArea ?: address.countryName ?: "Unknown City"
            } else {
                "Unknown City"
            }
        } catch (e: Exception) {
            "Geocoding Error"
        }
    }
}