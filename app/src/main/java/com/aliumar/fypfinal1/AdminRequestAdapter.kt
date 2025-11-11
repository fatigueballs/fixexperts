package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class AdminRequestAdapter(
    private val requests: List<ServiceRequest>
) : RecyclerView.Adapter<AdminRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val requestInfo: TextView = view.findViewById(R.id.textRequestInfo)
        val serviceType: TextView = view.findViewById(R.id.textServiceType)
        val customerAndRepairman: TextView = view.findViewById(R.id.textCustomerAndRepairman)
        val requestStatus: TextView = view.findViewById(R.id.textRequestStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_request, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]

        val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
        val dateTimeString = dateFormat.format(request.dateMillis)

        holder.requestInfo.text = "ID: ${request.id.substring(request.id.length - 4)} | Date: $dateTimeString"
        holder.serviceType.text = "Service: ${request.serviceType}"
        holder.customerAndRepairman.text = "From: ${request.userName} | To: ${request.repairmanName}"

        val statusText = when {
            request.userConfirmedJobDone && request.repairmanConfirmedPayment -> "Fully Completed"
            request.status == "Declined" -> "Declined"
            request.status == "Pending" -> "Pending"
            request.status == "Accepted" -> "Accepted (In Progress)"
            request.userConfirmedJobDone -> "Job Confirmed by User (Awaiting RM Payment)"
            else -> request.status
        }
        holder.requestStatus.text = "Status: $statusText"
    }
}