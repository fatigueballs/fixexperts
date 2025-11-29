package com.aliumar.fypfinal1

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

        // Reset visibility
        holder.buttonAccept.visibility = View.GONE
        holder.buttonDecline.visibility = View.GONE
        holder.buttonDone.visibility = View.GONE
        holder.buttonChat.visibility = View.GONE
        holder.buttonUploadProof.visibility = View.GONE // Reset

        holder.buttonDone.text = "Confirm Payment"

        when {
            request.status == "Pending" -> {
                holder.textStatus.text = "Status: Pending"
                holder.buttonAccept.visibility = View.VISIBLE
                holder.buttonDecline.visibility = View.VISIBLE
            }
            request.status == "Accepted" || request.userConfirmedJobDone -> {
                holder.buttonChat.visibility = View.VISIBLE

                if (request.userConfirmedJobDone && request.repairmanConfirmedPayment) {
                    holder.textStatus.text = "Status: Fully Completed"
                } else if (request.userConfirmedJobDone) {
                    // Job is done by user, check for payment proof
                    if (request.userPaymentProofUrl.isNotEmpty()) {
                        holder.textStatus.text = "Status: Payment Proof Received - Confirm?"
                        holder.buttonDone.visibility = View.VISIBLE // ONLY visible if proof exists
                    } else {
                        holder.textStatus.text = "Status: Job Done - Waiting for User Payment Proof"
                        // Button remains GONE
                    }
                } else if (request.status == "Accepted") {
                    // Logic: Check if Repairman has uploaded proof
                    if (request.repairmanProofUrl.isNotEmpty()) {
                        holder.textStatus.text = "Status: Work Proof Uploaded (Waiting User Confirmation)"
                        holder.buttonUploadProof.text = "Update Work Proof"
                        holder.buttonUploadProof.visibility = View.VISIBLE
                    } else {
                        holder.textStatus.text = "Status: Accepted - Please Upload Work Proof"
                        holder.buttonUploadProof.text = "Upload Work Proof"
                        holder.buttonUploadProof.visibility = View.VISIBLE
                    }
                }
            }
            request.status == "Declined" -> holder.textStatus.text = "Status: Declined"
            else -> {
                holder.textStatus.text = "Status: ${request.status}"
                if (request.status != "Declined") holder.buttonChat.visibility = View.VISIBLE
            }
        }

        holder.buttonViewDetails.setOnClickListener { showDetailsPopup(request, holder.itemView.context) }
        holder.buttonAccept.setOnClickListener { onAction(request, "accept") }
        holder.buttonDecline.setOnClickListener { onAction(request, "decline") }
        holder.buttonDone.setOnClickListener { onAction(request, "confirm_payment") }
        holder.buttonChat.setOnClickListener { onAction(request, "chat") }
        holder.buttonUploadProof.setOnClickListener { onAction(request, "upload_proof") }
    }

    private fun showDetailsPopup(request: ServiceRequest, context: android.content.Context) {
        // ... (Keep existing implementation) ...
        val message = "Problem: ${request.problemDescription}"
        AlertDialog.Builder(context).setMessage(message).setPositiveButton("OK", null).show()
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
        val buttonChat: Button = itemView.findViewById(R.id.buttonChat)
        // NEW Button
        val buttonUploadProof: Button = itemView.findViewById(R.id.buttonUploadProof)
    }
}