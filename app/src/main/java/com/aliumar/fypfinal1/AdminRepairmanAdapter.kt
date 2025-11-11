package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.location.Geocoder
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.Locale

class AdminRepairmanAdapter(
    private val context: Context,
    private val repairmen: List<Repairman>,
    private val onApproveClick: (Repairman) -> Unit
) : RecyclerView.Adapter<AdminRepairmanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textRepairmanName)
        val email: TextView = view.findViewById(R.id.textRepairmanEmail)
        val location: TextView = view.findViewById(R.id.textRepairmanLocation)
        val approveBtn: Button = view.findViewById(R.id.buttonApprove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_repairman, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = repairmen.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repairman = repairmen[position]
        holder.name.text = "Name: ${repairman.username}"
        holder.email.text = "Email: ${repairman.email}"

        // Fetch location using reverse geocoding on a background thread (similar to RepairmanAdapter)
        holder.location.text = "Location: Loading..."
        Thread {
            val city = getCityFromCoordinates(repairman.latitude, repairman.longitude)
            Handler(Looper.getMainLooper()).post {
                if (holder.adapterPosition == position) {
                    holder.location.text = "Location: $city (Lat: ${String.format("%.4f", repairman.latitude)}, Lng: ${String.format("%.4f", repairman.longitude)})"
                }
            }
        }.start()

        holder.approveBtn.setOnClickListener { onApproveClick(repairman) }
    }

    private fun getCityFromCoordinates(lat: Double, lng: Double): String {
        if (lat == 0.0 && lng == 0.0) {
            return "Location Not Set"
        }
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