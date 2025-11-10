package com.beatwell.app.utils

import com.beatwell.app.models.CalendarDay
import java.util.*

object CalendarUtils {
    
    fun generateCalendarDays(year: Int, month: Int): List<CalendarDay> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        val days = mutableListOf<CalendarDay>()
        
        // Get the first day of the month and adjust to Monday start
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
        val mondayOffset = if (firstDayOfMonth == Calendar.SUNDAY) 6 else firstDayOfMonth - Calendar.MONDAY
        
        // Add previous month's trailing days
        calendar.add(Calendar.DAY_OF_MONTH, -mondayOffset)
        for (i in 0 until mondayOffset) {
            days.add(CalendarDay(
                dayNumber = calendar.get(Calendar.DAY_OF_MONTH),
                date = calendar.time.clone() as Date,
                isCurrentMonth = false
            ))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Reset to first day of current month
        calendar.set(year, month, 1)
        
        // Add current month's days
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()
        
        for (day in 1..daysInMonth) {
            calendar.set(year, month, day)
            val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            
            days.add(CalendarDay(
                dayNumber = day,
                date = calendar.time.clone() as Date,
                isCurrentMonth = true,
                isToday = isToday
            ))
        }
        
        // Add next month's leading days to complete the grid
        val remainingDays = 42 - days.size // 6 weeks * 7 days
        calendar.set(year, month + 1, 1) // Move to next month
        
        for (i in 0 until remainingDays) {
            days.add(CalendarDay(
                dayNumber = calendar.get(Calendar.DAY_OF_MONTH),
                date = calendar.time.clone() as Date,
                isCurrentMonth = false
            ))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return days
    }
    
    fun getMonthName(month: Int): String {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return monthNames[month]
    }
    
    fun getDayName(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val dayNames = arrayOf(
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        )
        return dayNames[dayOfWeek - 1]
    }
    
    fun formatDateForApi(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
