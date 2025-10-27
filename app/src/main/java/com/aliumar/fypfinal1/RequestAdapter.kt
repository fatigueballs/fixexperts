package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
        holder.buttonDone.text = "Done" // Reset button text

        when {
            request.status == "Pending" -> {
                holder.textStatus.text = "Status: Pending"
                holder.buttonAccept.visibility = View.VISIBLE
                holder.buttonDecline.visibility = View.VISIBLE
            }
            request.status == "Accepted" || request.jobCompletedByRepairman -> {
                if (request.jobCompletedByRepairman && request.paymentConfirmedByUser) {
                    holder.textStatus.text = "Status: Fully Completed (Payment Confirmed)"
                } else if (request.jobCompletedByRepairman) {
                    holder.textStatus.text = "Status: Job Done - Awaiting User Payment"
                } else {
                    holder.textStatus.text = "Status: Accepted (In Progress)"
                    holder.buttonDone.visibility = View.VISIBLE
                    holder.buttonDone.text = "Mark Job Done"
                }
            }
            else -> {
                holder.textStatus.text = "Status: ${request.status}"
            }
        }

        holder.buttonAccept.setOnClickListener { onAction(request, "accept") }
        holder.buttonDecline.setOnClickListener { onAction(request, "decline") }
        holder.buttonDone.setOnClickListener { onAction(request, "done") }
    }

    override fun getItemCount(): Int = requests.size

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textUserName: TextView = itemView.findViewById(R.id.textUserName)
        val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        val buttonAccept: Button = itemView.findViewById(R.id.buttonAccept)
        val buttonDecline: Button = itemView.findViewById(R.id.buttonDecline)
        val buttonDone: Button = itemView.findViewById(R.id.buttonDone)
    }
}
