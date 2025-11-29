package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserRequestAdapter(
    private val requests: List<ServiceRequest>,
    private val onConfirmPayment: (ServiceRequest) -> Unit,
    private val onRate: (ServiceRequest) -> Unit,
    private val onChat: (ServiceRequest) -> Unit,
    // NEW: Upload Payment Callback
    private val onUploadPayment: (ServiceRequest) -> Unit
) : RecyclerView.Adapter<UserRequestAdapter.UserRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_request, parent, false)
        return UserRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserRequestViewHolder, position: Int) {
        val request = requests[position]
        holder.textRepairmanName.text = "To: ${request.repairmanName}"
        holder.textServiceType.text = "Service: ${request.serviceType}"

        // Reset
        holder.buttonConfirmPayment.visibility = View.GONE
        holder.buttonRateRepairman.visibility = View.GONE
        holder.buttonChatUser.visibility = View.GONE
        holder.buttonUploadPayment.visibility = View.GONE // Reset

        holder.buttonConfirmPayment.text = "Confirm Job Done"

        when {
            request.userConfirmedJobDone && request.repairmanConfirmedPayment -> {
                if (request.userRated) {
                    holder.textStatusUser.text = "Status: Fully Completed and Rated"
                    holder.buttonChatUser.visibility = View.VISIBLE
                } else {
                    holder.textStatusUser.text = "Status: Completed - Rate Now!"
                    holder.buttonRateRepairman.visibility = View.VISIBLE
                    holder.buttonChatUser.visibility = View.VISIBLE
                }
            }
            request.userConfirmedJobDone -> {
                // Job confirmed by user, now check for payment proof
                if (request.userPaymentProofUrl.isNotEmpty()) {
                    holder.textStatusUser.text = "Status: Payment Proof Uploaded (Waiting Repairman)"
                    holder.buttonUploadPayment.text = "Update Payment Proof"
                } else {
                    holder.textStatusUser.text = "Status: Job Done - Please Upload Payment Proof"
                    holder.buttonUploadPayment.text = "Upload Payment Proof"
                }
                holder.buttonUploadPayment.visibility = View.VISIBLE // Show upload button
                holder.buttonChatUser.visibility = View.VISIBLE
            }
            request.status == "Accepted" -> {
                // Logic: Check if repairman uploaded proof
                if (request.repairmanProofUrl.isNotEmpty()) {
                    holder.textStatusUser.text = "Status: Work Proof Received - Please Confirm Job"
                    holder.buttonConfirmPayment.visibility = View.VISIBLE // Only visible if proof exists
                } else {
                    holder.textStatusUser.text = "Status: Accepted (Waiting for Repairman Work Proof)"
                    holder.buttonConfirmPayment.visibility = View.GONE
                }
                holder.buttonChatUser.visibility = View.VISIBLE
            }
            request.status == "Pending" -> holder.textStatusUser.text = "Status: Pending"
            request.status == "Declined" -> holder.textStatusUser.text = "Status: Declined"
            else -> {
                holder.textStatusUser.text = "Status: ${request.status}"
                if (request.status != "Declined") holder.buttonChatUser.visibility = View.VISIBLE
            }
        }

        holder.buttonConfirmPayment.setOnClickListener { onConfirmPayment(request) }
        holder.buttonRateRepairman.setOnClickListener { onRate(request) }
        holder.buttonChatUser.setOnClickListener { onChat(request) }
        holder.buttonUploadPayment.setOnClickListener { onUploadPayment(request) }
    }

    override fun getItemCount(): Int = requests.size

    class UserRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textRepairmanName: TextView = itemView.findViewById(R.id.textRepairmanName)
        val textServiceType: TextView = itemView.findViewById(R.id.textServiceType)
        val textStatusUser: TextView = itemView.findViewById(R.id.textStatusUser)
        val buttonConfirmPayment: Button = itemView.findViewById(R.id.buttonConfirmPayment)
        val buttonRateRepairman: Button = itemView.findViewById(R.id.buttonRateRepairman)
        val buttonChatUser: Button = itemView.findViewById(R.id.buttonChatUser)
        // NEW Button
        val buttonUploadPayment: Button = itemView.findViewById(R.id.buttonUploadPayment)
    }
}