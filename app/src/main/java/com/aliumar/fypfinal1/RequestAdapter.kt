package com.aliumar.fypfinal1

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        // 1. Reset Visibility
        holder.buttonAccept.visibility = View.GONE
        holder.buttonDecline.visibility = View.GONE
        holder.buttonDone.visibility = View.GONE
        holder.buttonChat.visibility = View.GONE

        // Default Details Text
        holder.buttonViewDetails.text = "View Details"
        holder.buttonViewDetails.visibility = View.VISIBLE

        // 2. Status Logic
        when {
            request.status == "Pending" -> {
                holder.textStatus.text = "Status: Pending"
                holder.buttonAccept.visibility = View.VISIBLE
                holder.buttonDecline.visibility = View.VISIBLE
            }
            request.status == "Declined" -> {
                holder.textStatus.text = "Status: Declined"
            }
            else -> {
                // Accepted / Ongoing / Completed
                holder.buttonChat.visibility = View.VISIBLE

                if (request.repairmanConfirmedPayment) {
                    holder.textStatus.text = "Status: Completed & Paid"
                }
                else if (request.paymentProofImage.isNotEmpty()) {
                    // User uploaded receipt -> Repairman needs to confirm
                    holder.textStatus.text = "Status: Payment Receipt Uploaded"
                    holder.buttonDone.visibility = View.VISIBLE
                    holder.buttonDone.text = "Confirm Payment"

                    // Allow viewing the receipt
                    holder.buttonViewDetails.text = "View Receipt"
                    holder.buttonViewDetails.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.paymentProofImage))
                        holder.itemView.context.startActivity(intent)
                    }
                }
                else if (request.workProofImage.isNotEmpty()) {
                    holder.textStatus.text = "Status: Work Proof Sent - Waiting for Payment"
                }
                else if (request.status == "Accepted") {
                    holder.textStatus.text = "Status: Accepted - Work in Progress"
                }
            }
        }

        // 3. Click Listeners
        holder.buttonAccept.setOnClickListener { onAction(request, "accept") }
        holder.buttonDecline.setOnClickListener { onAction(request, "decline") }
        holder.buttonDone.setOnClickListener { onAction(request, "confirm_payment") }
        holder.buttonChat.setOnClickListener { onAction(request, "chat") }

        // Standard details popup if not viewing receipt
        if (holder.buttonViewDetails.text == "View Details") {
            holder.buttonViewDetails.setOnClickListener {
                showDetailsPopup(request, holder.itemView.context)
            }
        }
    }

    private fun showDetailsPopup(request: ServiceRequest, context: android.content.Context) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy (hh:mm a)", Locale.getDefault())
        val dateTimeString = try { dateFormat.format(Date(request.dateMillis)) } catch (e: Exception) { request.date }

        AlertDialog.Builder(context)
            .setTitle("Details")
            .setMessage("Problem: ${request.problemDescription}\nDate: $dateTimeString")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun getItemCount(): Int = requests.size

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textUserName: TextView = itemView.findViewById(R.id.textUserName)
        val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        val buttonAccept: Button = itemView.findViewById(R.id.buttonAccept)
        val buttonDecline: Button = itemView.findViewById(R.id.buttonDecline)
        val buttonChat: Button = itemView.findViewById(R.id.buttonChat)

        // These match item_request.xml
        val buttonDone: Button = itemView.findViewById(R.id.buttonDone)
        val buttonViewDetails: Button = itemView.findViewById(R.id.buttonViewDetails)
    }
}