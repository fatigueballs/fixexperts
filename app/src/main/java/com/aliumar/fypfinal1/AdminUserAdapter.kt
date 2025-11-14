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

class AdminUserAdapter(
    private val context: Context,
    private val users: List<User>,
    private val onViewHistoryClick: (User, String) -> Unit, // Passes user and their ID (username)
    private val onDeleteClick: (User, String) -> Unit // NEW: Delete click listener
) : RecyclerView.Adapter<AdminUserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textUserName)
        val email: TextView = view.findViewById(R.id.textUserEmail)
        val location: TextView = view.findViewById(R.id.textUserLocation)
        val viewHistoryBtn: Button = view.findViewById(R.id.buttonViewHistory)
        val deleteBtn: Button = view.findViewById(R.id.buttonDeleteUser) // NEW: Find delete button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.name.text = user.username
        holder.email.text = "Email: ${user.email}"

        holder.location.text = "Location: Loading..."
        Thread {
            val city = getCityFromCoordinates(user.latitude, user.longitude)
            Handler(Looper.getMainLooper()).post {
                if (holder.adapterPosition == position) {
                    holder.location.text = "Location: $city"
                }
            }
        }.start()

        holder.viewHistoryBtn.setOnClickListener {
            onViewHistoryClick(user, user.username)
        }

        // NEW: Set delete listener
        holder.deleteBtn.setOnClickListener {
            onDeleteClick(user, user.username)
        }
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