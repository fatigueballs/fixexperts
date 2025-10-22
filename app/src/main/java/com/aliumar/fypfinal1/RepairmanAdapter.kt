package com.aliumar.fypfinal1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RepairmanAdapter(
    private val repairmen: List<Repairman>,
    private val onRequestClick: (Repairman) -> Unit
) : RecyclerView.Adapter<RepairmanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textName)
        val specialties: TextView = view.findViewById(R.id.textSpecialties)
        val requestBtn: Button = view.findViewById(R.id.buttonRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_repairman, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = repairmen.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repairman = repairmen[position]
        holder.name.text = repairman.username
        holder.specialties.text = repairman.specialties.joinToString(", ")
        holder.requestBtn.setOnClickListener { onRequestClick(repairman) }
    }
}
