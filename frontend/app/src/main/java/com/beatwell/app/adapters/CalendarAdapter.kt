package com.beatwell.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.beatwell.app.R
import com.beatwell.app.models.CalendarDay
import java.util.*

class CalendarAdapter(
    private val onDateClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var calendarDays = mutableListOf<CalendarDay>()
    private var selectedDate: CalendarDay? = null

    fun updateCalendar(days: List<CalendarDay>) {
        calendarDays.clear()
        calendarDays.addAll(days)
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: CalendarDay) {
        val oldSelected = selectedDate
        selectedDate = date
        oldSelected?.let { notifyItemChanged(calendarDays.indexOf(it)) }
        selectedDate?.let { notifyItemChanged(calendarDays.indexOf(it)) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = calendarDays[position]
        holder.bind(day, day == selectedDate)
    }

    override fun getItemCount(): Int = calendarDays.size

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val vMealIndicator: View = itemView.findViewById(R.id.vMealIndicator)

        fun bind(day: CalendarDay, isSelected: Boolean) {
            tvDay.text = day.dayNumber.toString()
            
            // Set text color based on day type
            when {
                day.isCurrentMonth -> {
                    tvDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                }
                else -> {
                    tvDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                }
            }

            // Set background based on selection and today
            when {
                isSelected -> {
                    itemView.setBackgroundResource(R.drawable.calendar_day_selected)
                }
                day.isToday -> {
                    itemView.setBackgroundResource(R.drawable.calendar_day_today)
                }
                else -> {
                    itemView.background = null
                }
            }

            // Show meal indicator if day has meals
            vMealIndicator.visibility = if (day.hasMeals) View.VISIBLE else View.GONE

            // Set click listener
            itemView.setOnClickListener {
                if (day.isCurrentMonth) {
                    onDateClick(day)
                }
            }
        }
    }
}
