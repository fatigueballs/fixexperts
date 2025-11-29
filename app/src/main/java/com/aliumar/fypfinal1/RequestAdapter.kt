package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

        // Reset visibility
        holder.buttonAccept.visibility = View.GONE
        holder.buttonDecline.visibility = View.GONE
        holder.buttonDone.visibility = View.GONE
        holder.buttonChat.visibility = View.GONE // Default to GONE
        holder.buttonUploadProof.visibility = View.GONE
        holder.buttonDone.text = "Confirm Payment"

        if (request.status == "Accepted" && !request.userConfirmedJobDone) {
            holder.buttonUploadProof.visibility = View.VISIBLE
            if (request.repairmanProofUrl.isNotEmpty()) {
                holder.buttonUploadProof.text = "Work Proof Uploaded (Update?)"
            } else {
                holder.buttonUploadProof.text = "Upload Work Proof"
            }
        }

        holder.buttonUploadProof.setOnClickListener { onAction(request, "upload_proof") }


        when {
            request.status == "Pending" -> {
                holder.textStatus.text = "Status: Pending"
                holder.buttonAccept.visibility = View.VISIBLE
                holder.buttonDecline.visibility = View.VISIBLE
                // Chat is usually not allowed until accepted
            }
            request.status == "Accepted" || request.userConfirmedJobDone -> {
                // Show chat once accepted
                holder.buttonChat.visibility = View.VISIBLE

                if (request.userConfirmedJobDone && request.repairmanConfirmedPayment) {
                    holder.textStatus.text = "Status: Fully Completed (Payment Confirmed)"
                } else if (request.userConfirmedJobDone) {
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
            request.status == "Declined" -> {
                holder.textStatus.text = "Status: Declined"
            }
            else -> {
                // For completed or other statuses, we might still want chat visible for history
                holder.textStatus.text = "Status: ${request.status}"
                if (request.status != "Declined") {
                    holder.buttonChat.visibility = View.VISIBLE
                }
            }
        }

        holder.buttonViewDetails.setOnClickListener {
            showDetailsPopup(request, holder.itemView.context)
        }

        holder.buttonAccept.setOnClickListener { onAction(request, "accept") }
        holder.buttonDecline.setOnClickListener { onAction(request, "decline") }
        holder.buttonDone.setOnClickListener { onAction(request, "confirm_payment") }

        // Chat Action
        holder.buttonChat.setOnClickListener { onAction(request, "chat") }
    }

    private fun showDetailsPopup(request: ServiceRequest, context: android.content.Context) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy (hh:mm a)", Locale.getDefault())
        val dateTimeString = try {
            dateFormat.format(Date(request.dateMillis))
        } catch (e: Exception) {
            request.date
        }

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

    override fun getItemCount(): Int = requests.size

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textUserName: TextView = itemView.findViewById(R.id.textUserName)
        val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        val buttonAccept: Button = itemView.findViewById(R.id.buttonAccept)
        val buttonDecline: Button = itemView.findViewById(R.id.buttonDecline)
        val buttonDone: Button = itemView.findViewById(R.id.buttonDone)
        val buttonViewDetails: Button = itemView.findViewById(R.id.buttonViewDetails)
        // NEW Chat Button
        val buttonChat: Button = itemView.findViewById(R.id.buttonChat)
        val buttonUploadProof: Button = itemView.findViewById(R.id.buttonUploadProof)
    }
}