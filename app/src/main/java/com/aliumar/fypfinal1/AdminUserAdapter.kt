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
    private val onViewHistoryClick: (User, String) -> Unit // Passes user and their ID
) : RecyclerView.Adapter<AdminUserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textUserName)
        val email: TextView = view.findViewById(R.id.textUserEmail)
        val location: TextView = view.findViewById(R.id.textUserLocation)
        val viewHistoryBtn: Button = view.findViewById(R.id.buttonViewHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        // We need the key, which isn't in the User object. We'll handle this in the Activity.
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
            // The key (ID) will be passed from the activity's map
            onViewHistoryClick(user, user.username) // Assuming username is the key
        }
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