package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserRequestAdapter(
    private val requests: List<ServiceRequest>,
    private val onConfirmPayment: (ServiceRequest) -> Unit, // Legacy param, can remain
    private val onRate: (ServiceRequest) -> Unit,
    private val onChat: (ServiceRequest) -> Unit,
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

        // Logic
        when {
            // Case 1: All done
            request.userRated -> {
                holder.textStatusUser.text = "Status: Completed & Rated"
            }
            // Case 2: Paid & Confirmed by Repairman -> Ready to Rate
            request.repairmanConfirmedPayment -> {
                holder.textStatusUser.text = "Job Done! Please Rate."
                holder.buttonRateRepairman.visibility = View.VISIBLE
            }
            // Case 3: Receipt Uploaded -> Waiting for Repairman
            request.paymentProofImage.isNotEmpty() -> {
                holder.textStatusUser.text = "Receipt Sent. Waiting for Confirmation."
            }
            // Case 4: Work Proof Sent by Repairman -> User needs to upload receipt
            request.workProofImage.isNotEmpty() -> {
                holder.textStatusUser.text = "Work Done. Please Upload Receipt."
                holder.buttonConfirmPayment.visibility = View.VISIBLE
                holder.buttonConfirmPayment.text = "Upload Receipt"

                // Clicking this triggers the upload flow
                holder.buttonConfirmPayment.setOnClickListener { onUploadPayment(request) }
            }
            // Case 5: Accepted/Ongoing
            request.status == "Accepted" || request.status == "Ongoing" -> {
                holder.textStatusUser.text = "Status: Ongoing"
                holder.buttonChatUser.visibility = View.VISIBLE
            }
            else -> {
                holder.textStatusUser.text = "Status: ${request.status}"
            }
        }

        holder.buttonRateRepairman.setOnClickListener { onRate(request) }
        holder.buttonChatUser.setOnClickListener { onChat(request) }
    }

    override fun getItemCount(): Int = requests.size

    class UserRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textRepairmanName: TextView = itemView.findViewById(R.id.textRepairmanName)
        val textServiceType: TextView = itemView.findViewById(R.id.textServiceType)
        val textStatusUser: TextView = itemView.findViewById(R.id.textStatusUser)

        // This button handles the Upload Receipt action
        val buttonConfirmPayment: Button = itemView.findViewById(R.id.buttonConfirmPayment)
        val buttonRateRepairman: Button = itemView.findViewById(R.id.buttonRateRepairman)
        val buttonChatUser: Button = itemView.findViewById(R.id.buttonChatUser)
    }
}