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
    // NEW: Chat Listener
    private val onChat: (ServiceRequest) -> Unit
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

        holder.buttonConfirmPayment.visibility = View.GONE
        holder.buttonRateRepairman.visibility = View.GONE
        holder.buttonChatUser.visibility = View.GONE // Reset

        holder.buttonConfirmPayment.text = "Confirm Job Done"

        when {
            request.userConfirmedJobDone && request.repairmanConfirmedPayment -> {
                // Job and Payment confirmed
                if (request.userRated) {
                    holder.textStatusUser.text = "Status: Fully Completed and Rated"
                    holder.buttonChatUser.visibility = View.VISIBLE // Optional: Can chat in history
                } else {
                    holder.textStatusUser.text = "Status: Completed - Rate Now!"
                    holder.buttonRateRepairman.visibility = View.VISIBLE
                    holder.buttonChatUser.visibility = View.VISIBLE
                }
            }
            request.userConfirmedJobDone -> {
                holder.textStatusUser.text = "Status: Job Done - Awaiting Payment Confirmation"
                holder.buttonChatUser.visibility = View.VISIBLE
            }
            request.status == "Accepted" -> {
                holder.textStatusUser.text = "Status: Accepted (Tap when job is complete)"
                holder.buttonConfirmPayment.visibility = View.VISIBLE
                holder.buttonChatUser.visibility = View.VISIBLE // Allow chat
            }
            request.status == "Pending" -> {
                holder.textStatusUser.text = "Status: Pending"
                // Chat usually disabled while pending
            }
            request.status == "Declined" -> {
                holder.textStatusUser.text = "Status: Declined"
            }
            else -> {
                holder.textStatusUser.text = "Status: ${request.status}"
                // Default show chat if not declined
                if (request.status != "Declined") holder.buttonChatUser.visibility = View.VISIBLE
            }
        }

        holder.buttonConfirmPayment.setOnClickListener { onConfirmPayment(request) }
        holder.buttonRateRepairman.setOnClickListener { onRate(request) }
        holder.buttonChatUser.setOnClickListener { onChat(request) }
    }

    override fun getItemCount(): Int = requests.size

    class UserRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textRepairmanName: TextView = itemView.findViewById(R.id.textRepairmanName)
        val textServiceType: TextView = itemView.findViewById(R.id.textServiceType)
        val textStatusUser: TextView = itemView.findViewById(R.id.textStatusUser)
        val buttonConfirmPayment: Button = itemView.findViewById(R.id.buttonConfirmPayment)
        val buttonRateRepairman: Button = itemView.findViewById(R.id.buttonRateRepairman)
        val buttonChatUser: Button = itemView.findViewById(R.id.buttonChatUser) // NEW
    }
}