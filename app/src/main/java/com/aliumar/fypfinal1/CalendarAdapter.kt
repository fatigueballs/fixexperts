package com.aliumar.fypfinal1

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(
    private val days: List<CalendarDay>
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    // Data class to hold day info and status
    data class CalendarDay(
        val dayString: String, // The number "1", "2", etc. or "" for padding
        val status: String? = null // "Pending", "Accepted", or null
    )

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvCalendarDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        // Set height dynamically to fill grid if needed, or keep fixed 60dp
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = days[position]
        holder.tvDay.text = day.dayString

        if (day.dayString.isEmpty()) {
            holder.tvDay.visibility = View.INVISIBLE
            holder.tvDay.setBackgroundColor(Color.TRANSPARENT)
        } else {
            holder.tvDay.visibility = View.VISIBLE

            // Highlight logic
            when (day.status) {
                "Accepted" -> {
                    holder.tvDay.setBackgroundColor(Color.RED)
                    holder.tvDay.textColor = Color.WHITE // White text on Red
                }
                "Pending" -> {
                    holder.tvDay.setBackgroundColor(Color.YELLOW)
                    holder.tvDay.textColor = Color.BLACK
                }
                else -> {
                    holder.tvDay.setBackgroundColor(Color.TRANSPARENT) // Or default background
                    holder.tvDay.textColor = Color.BLACK
                }
            }
        }
    }

    // Extension property for easier text color setting
    private var TextView.textColor: Int
        get() = currentTextColor
        set(v) = setTextColor(v)

    override fun getItemCount(): Int = days.size
}