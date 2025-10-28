package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.location.Geocoder
import java.util.Locale

class RepairmanAdapter(
    // MODIFIED: Added Context to the constructor
    private val context: Context,
    private val repairmen: List<Repairman>,
    private val onRequestClick: (Repairman) -> Unit
) : RecyclerView.Adapter<RepairmanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textName)
        val specialties: TextView = view.findViewById(R.id.textSpecialties)
        val rating: TextView = view.findViewById(R.id.textRating)
        val location: TextView = view.findViewById(R.id.textLocation)
        val requestBtn: Button = view.findViewById(R.id.buttonRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_repairman, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = repairmen.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repairman = repairmen[position]
        holder.name.text = repairman.username
        holder.specialties.text = "Specialties: ${repairman.specialties.joinToString(", ")}"

        // FIX: Display Rating Logic
        val ratingText = if (repairman.ratingCount > 0) {
            // Format to one decimal place
            String.format("Rating: %.1f (%d reviews)", repairman.avgRating, repairman.ratingCount)
        } else {
            "Rating: Unrated"
        }
        holder.rating.text = ratingText
        // END FIX

        holder.requestBtn.setOnClickListener { onRequestClick(repairman) }
    }

    // NEW: Function to perform real reverse geocoding
    private fun getCityFromCoordinates(lat: Double, lng: Double): String {
        // Handle uninitialized location from registration
        if (lat == 0.0 && lng == 0.0) {
            return "Location Not Set"
        }

        // IMPORTANT: Geocoding is slow and should ideally run off the main thread (e.g., in a Coroutine).
        // For demonstration, we perform a synchronous call, which may cause jank with a large list.
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            // Fetch one address
            val addresses = geocoder.getFromLocation(lat, lng, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                // Prioritize locality (city), then general administrative area (state), then country
                return address.locality ?: address.adminArea ?: address.countryName ?: "Unknown City"
            }
        } catch (e: Exception) {
            // Catch IO and service exceptions
            return "Geocoding Error"
        }
        return "Unknown City"
    }
}