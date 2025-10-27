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

        when {
            request.paymentConfirmedByUser && request.userRated -> {
                holder.textStatusUser.text = "Status: Fully Completed and Rated"
            }
            request.paymentConfirmedByUser && !request.userRated -> {
                holder.textStatusUser.text = "Status: Payment Confirmed - Rate Now!"
                holder.buttonRateRepairman.visibility = View.VISIBLE
            }
            request.jobCompletedByRepairman -> {
                holder.textStatusUser.text = "Status: Job Done - Tap to Confirm Payment"
                holder.buttonConfirmPayment.visibility = View.VISIBLE
            }
            else -> {
                holder.textStatusUser.text = "Status: ${request.status}"
            }
        }

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