package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
// ADDED
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// END ADDED
import androidx.recyclerview.widget.RecyclerView

class RequestAdapter(
    private val requests: List<ServiceRequest>,
    private val onAction: (ServiceRequest, String) -> Unit
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        holder.textUserName.text = "From: ${request.userName}"
        holder.textDescription.text = "Service: ${request.serviceType}"

        // Logic to determine button visibility and status text for Repairman
        holder.buttonAccept.visibility = View.GONE
        holder.buttonDecline.visibility = View.GONE
        holder.buttonDone.visibility = View.GONE
        holder.buttonDone.text = "Confirm Payment" // Renamed button text for new flow

        when {
            request.status == "Pending" -> {
                holder.textStatus.text = "Status: Pending"
                holder.buttonAccept.visibility = View.VISIBLE
                holder.buttonDecline.visibility = View.VISIBLE
            }
            request.status == "Accepted" || request.userConfirmedJobDone -> {
                if (request.userConfirmedJobDone && request.repairmanConfirmedPayment) {
                    holder.textStatus.text = "Status: Fully Completed (Payment Confirmed)"
                } else if (request.userConfirmedJobDone) {
                    // NEW FLOW: Job done is confirmed by User, now RM confirms payment
                    holder.textStatus.text = "Status: Job Done by User - Await Payment Confirmation"
                    if (!request.repairmanConfirmedPayment) {
                        holder.buttonDone.visibility = View.VISIBLE
                    }
                } else if (request.status == "Accepted") {
                    holder.textStatus.text = "Status: Accepted (Awaiting User Job Confirmation)"
                } else {
                    holder.textStatus.text = "Status: Accepted (In Progress)"
                }
            }
            else -> {
                holder.textStatus.text = "Status: ${request.status}"
            }
        }

        // ADDED: View Details Button Listener
        holder.buttonViewDetails.setOnClickListener {
            showDetailsPopup(request, holder.itemView.context)
        }
        // END ADDED

        holder.buttonAccept.setOnClickListener { onAction(request, "accept") }
        holder.buttonDecline.setOnClickListener { onAction(request, "decline") }
        // The action for buttonDone is now "confirm_payment"
        holder.buttonDone.setOnClickListener { onAction(request, "confirm_payment") }
    }

    // ADDED: Function to show a popup with request details
    private fun showDetailsPopup(request: ServiceRequest, context: android.content.Context) {
        // Format date and time (e.g., "01 January 2025 (04:30 PM)")
        val dateFormat = SimpleDateFormat("dd MMMM yyyy (hh:mm a)", Locale.getDefault())
        val dateTimeString = dateFormat.format(Date(request.dateMillis))

        val message = """
            Customer: ${request.userName}
            Service: ${request.serviceType}
            
            Scheduled For: $dateTimeString
            
            Problem Description:
            ${request.problemDescription.ifEmpty { "No description provided." }}
        """.trimIndent()

        AlertDialog.Builder(context)
            .setTitle("Service Request Details")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    // END ADDED

    override fun getItemCount(): Int = requests.size

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textUserName: TextView = itemView.findViewById(R.id.textUserName)
        val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        val buttonAccept: Button = itemView.findViewById(R.id.buttonAccept)
        val buttonDecline: Button = itemView.findViewById(R.id.buttonDecline)
        val buttonDone: Button = itemView.findViewById(R.id.buttonDone)
        // ADDED
        val buttonViewDetails: Button = itemView.findViewById(R.id.buttonViewDetails)
        // END ADDED
    }
}