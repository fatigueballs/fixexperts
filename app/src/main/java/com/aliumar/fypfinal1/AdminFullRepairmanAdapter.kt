package com.aliumar.fypfinal1

import android.content.Context
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AdminFullRepairmanAdapter(
    private val context: Context,
    private val repairmen: List<Repairman>,
    private val onApproveClick: (Repairman) -> Unit,
    private val onChangeRatingClick: (Repairman) -> Unit,
    private val onViewHistoryClick: (Repairman) -> Unit
) : RecyclerView.Adapter<AdminFullRepairmanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textRepairmanName)
        val email: TextView = view.findViewById(R.id.textRepairmanEmail)
        val location: TextView = view.findViewById(R.id.textRepairmanLocation)
        val rating: TextView = view.findViewById(R.id.textRepairmanRating)
        val approveBtn: Button = view.findViewById(R.id.buttonApprove)
        val changeRatingBtn: Button = view.findViewById(R.id.buttonChangeRating)
        val viewHistoryBtn: Button = view.findViewById(R.id.buttonViewHistory)
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

        // Format Rating
        val ratingText = if (repairman.ratingCount > 0) {
            String.format("Rating: %.1f (%d reviews)", repairman.avgRating, repairman.ratingCount)
        } else {
            "Rating: Unrated"
        }
        holder.rating.text = ratingText

        // Geocoding for location
        holder.location.text = "Location: Loading..."
        Thread {
            val city = getCityFromCoordinates(repairman.latitude, repairman.longitude)
            Handler(Looper.getMainLooper()).post {
                if (holder.adapterPosition == position) {
                    holder.location.text = "Location: $city"
                }
            }
        }.start()

        // Conditional Button Visibility
        if (repairman.isApprovedByAdmin) {
            holder.approveBtn.visibility = View.GONE
            holder.changeRatingBtn.visibility = View.VISIBLE
            holder.viewHistoryBtn.visibility = View.VISIBLE
        } else {
            holder.approveBtn.visibility = View.VISIBLE
            holder.changeRatingBtn.visibility = View.GONE
            holder.viewHistoryBtn.visibility = View.GONE
        }

        // Click Listeners
        holder.approveBtn.setOnClickListener { onApproveClick(repairman) }
        holder.changeRatingBtn.setOnClickListener { onChangeRatingClick(repairman) }
        holder.viewHistoryBtn.setOnClickListener { onViewHistoryClick(repairman) }
    }

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