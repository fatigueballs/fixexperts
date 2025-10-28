package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserRequestAdapter(
    // The lambda name will remain the same but the action is now Confirm Job Done
    private val requests: List<ServiceRequest>,
    private val onConfirmPayment: (ServiceRequest) -> Unit,
    private val onRate: (ServiceRequest) -> Unit
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

        // Logic to determine button visibility and status text
        holder.buttonConfirmPayment.visibility = View.GONE
        holder.buttonRateRepairman.visibility = View.GONE

        // Change button text to reflect new role (User confirms job is done)
        holder.buttonConfirmPayment.text = "Confirm Job Done"

        when {
            request.userConfirmedJobDone && request.repairmanConfirmedPayment -> {
                // Job and Payment confirmed
                if (request.userRated) {
                    holder.textStatusUser.text = "Status: Fully Completed and Rated"
                } else {
                    holder.textStatusUser.text = "Status: Completed - Rate Now!"
                    holder.buttonRateRepairman.visibility = View.VISIBLE
                }
            }
            request.userConfirmedJobDone -> {
                // User confirmed job done, awaiting RM payment confirmation
                holder.textStatusUser.text = "Status: Job Done - Awaiting Payment Confirmation"
            }
            request.status == "Accepted" -> {
                // RM accepted, User needs to confirm job done (which means service is physically completed)
                holder.textStatusUser.text = "Status: Accepted (Tap when job is complete)"
                holder.buttonConfirmPayment.visibility = View.VISIBLE
            }
            else -> {
                holder.textStatusUser.text = "Status: ${request.status}"
            }
        }

        // The click handler's name remains `onConfirmPayment` but it performs "Confirm Job Done" logic
        holder.buttonConfirmPayment.setOnClickListener { onConfirmPayment(request) }
        holder.buttonRateRepairman.setOnClickListener { onRate(request) }
    }

    override fun getItemCount(): Int = requests.size

    class UserRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textRepairmanName: TextView = itemView.findViewById(R.id.textRepairmanName)
        val textServiceType: TextView = itemView.findViewById(R.id.textServiceType)
        val textStatusUser: TextView = itemView.findViewById(R.id.textStatusUser)
        val buttonConfirmPayment: Button = itemView.findViewById(R.id.buttonConfirmPayment)
        val buttonRateRepairman: Button = itemView.findViewById(R.id.buttonRateRepairman)
    }
}