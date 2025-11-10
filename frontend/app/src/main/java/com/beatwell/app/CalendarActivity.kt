package com.beatwell.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.beatwell.app.auth.AuthManager
import com.beatwell.app.databinding.ActivityCalendarBinding
import com.beatwell.app.models.MealLog
import com.beatwell.app.network.NetworkManager
import com.beatwell.app.utils.BottomNavigationHelper
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCalendarBinding
    private lateinit var bottomNavigationHelper: BottomNavigationHelper
    private lateinit var authManager: AuthManager
    private lateinit var networkManager: NetworkManager
    
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDate: Calendar? = null
    private var waterIntake = 4
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val calendarDays = mutableListOf<CalendarDay>()
    
    data class CalendarDay(
        val day: Int,
        val isCurrentMonth: Boolean,
        val isToday: Boolean,
        val hasMeals: Boolean,
        val date: Calendar
    )
    
    private fun isSameDate(date1: Calendar?, date2: Calendar?): Boolean {
        if (date1 == null || date2 == null) return false
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh calendar data when returning from meal activities
        loadCalendarData()
        // Refresh selected date meals
        selectedDate?.let { loadMealsForDate(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Set status bar color
        window.statusBarColor = getColor(R.color.primary_color)
        
        // Initialize managers
        authManager = AuthManager(this)
        networkManager = NetworkManager(this)
        
        // Initialize bottom navigation
        bottomNavigationHelper = BottomNavigationHelper(this)
        bottomNavigationHelper.setupBottomNavigation()
        
        // Setup calendar
        setupCalendar()
        
        // Setup click listeners
        setupClickListeners()
        
        // Load initial data
        loadCalendarData()
    }
    
    private fun setupCalendar() {
        generateCalendarDays()
        populateCalendarGrid()
        
        // Set today as selected by default
        val today = Calendar.getInstance()
        selectedDate = today
        updateSelectedDateInfo()
        loadMealsForDate(today)
    }
    
    private fun generateCalendarDays() {
        calendarDays.clear()
        
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        
        // Set to first day of current month
        calendar.set(currentYear, currentMonth, 1)
        
        // Get the first day of the month and adjust to Monday start
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
        val mondayOffset = if (firstDayOfMonth == Calendar.SUNDAY) 6 else firstDayOfMonth - Calendar.MONDAY
        
        // Add previous month's trailing days
        calendar.add(Calendar.DAY_OF_MONTH, -mondayOffset)
        for (i in 0 until mondayOffset) {
            calendarDays.add(CalendarDay(
                day = calendar.get(Calendar.DAY_OF_MONTH),
                isCurrentMonth = false,
                isToday = false,
                hasMeals = false,
                date = calendar.clone() as Calendar
            ))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Reset to first day of current month
        calendar.set(currentYear, currentMonth, 1)
        
        // Add current month's days
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        for (day in 1..daysInMonth) {
            calendar.set(currentYear, currentMonth, day)
            val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            
            calendarDays.add(CalendarDay(
                day = day,
                isCurrentMonth = true,
                isToday = isToday,
                hasMeals = false,
                date = calendar.clone() as Calendar
            ))
        }
        
        // Add next month's leading days to complete the grid (6 weeks = 42 days)
        val remainingDays = 42 - calendarDays.size
        calendar.set(currentYear, currentMonth + 1, 1) // Move to next month
        
        for (i in 0 until remainingDays) {
            calendarDays.add(CalendarDay(
                day = calendar.get(Calendar.DAY_OF_MONTH),
                isCurrentMonth = false,
                isToday = false,
                hasMeals = false,
                date = calendar.clone() as Calendar
            ))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Ensure we have exactly 42 days (6 weeks)
        while (calendarDays.size < 42) {
            calendarDays.add(CalendarDay(
                day = calendar.get(Calendar.DAY_OF_MONTH),
                isCurrentMonth = false,
                isToday = false,
                hasMeals = false,
                date = calendar.clone() as Calendar
            ))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    private fun populateCalendarGrid() {
        binding.gridCalendar.removeAllViews()
        
        calendarDays.forEach { calendarDay ->
            val dayView = createDayView(calendarDay)
            binding.gridCalendar.addView(dayView)
        }
    }
    
    private fun createDayView(calendarDay: CalendarDay): View {
        // Create a container layout for the day and indicator
        val containerLayout = android.widget.LinearLayout(this)
        containerLayout.orientation = android.widget.LinearLayout.VERTICAL
        containerLayout.gravity = Gravity.CENTER
        
        // Set layout parameters
        val layoutParams = GridLayout.LayoutParams()
        layoutParams.width = 0
        layoutParams.height = GridLayout.LayoutParams.WRAP_CONTENT
        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        layoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        layoutParams.setMargins(8, 12, 8, 12)
        containerLayout.layoutParams = layoutParams
        
        // Create day text view
        val dayView = TextView(this)
        dayView.text = calendarDay.day.toString()
        dayView.gravity = Gravity.CENTER
        dayView.textSize = 18f
        dayView.setPadding(16, 20, 16, 20)
        dayView.minHeight = 60
        
        // Set text color based on day type
        when {
            calendarDay.isCurrentMonth -> {
                dayView.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
            else -> {
                dayView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
        
        // Set background based on selection and today
        when {
            isSameDate(calendarDay.date, selectedDate) -> {
                dayView.setBackgroundResource(R.drawable.calendar_day_selected)
            }
            calendarDay.isToday -> {
                dayView.setBackgroundResource(R.drawable.calendar_day_today)
            }
            else -> {
                dayView.background = null
            }
        }
        
        // Add the day view to container
        containerLayout.addView(dayView)
        
        // Add meal indicator if day has meals
        if (calendarDay.hasMeals) {
            val mealIndicator = View(this)
            val indicatorParams = android.widget.LinearLayout.LayoutParams(12, 12)
            indicatorParams.gravity = Gravity.CENTER
            indicatorParams.setMargins(0, 4, 0, 0)
            mealIndicator.layoutParams = indicatorParams
            mealIndicator.setBackgroundResource(R.drawable.circle_background_green)
            containerLayout.addView(mealIndicator)
        }
        
        // Set click listener on container
        containerLayout.setOnClickListener {
            if (calendarDay.isCurrentMonth) {
                selectedDate = calendarDay.date
                updateSelectedDateInfo()
                loadMealsForDate(calendarDay.date)
                populateCalendarGrid() // Refresh to show selection
            }
        }
        
        return containerLayout
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // Month navigation
        binding.ivPrevMonth.setOnClickListener {
            navigateMonth(-1)
        }
        
        binding.ivNextMonth.setOnClickListener {
            navigateMonth(1)
        }
        
        // Water intake controls
        binding.seekBarWater.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                waterIntake = progress
                binding.tvWaterIntake.text = "$waterIntake/8 glasses"
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        binding.btnAddWater.setOnClickListener {
            saveWaterIntake()
        }
        
        binding.btnResetWater.setOnClickListener {
            waterIntake = 0
            binding.seekBarWater.progress = waterIntake
            binding.tvWaterIntake.text = "$waterIntake/8 glasses"
            saveWaterIntake()
        }
    }
    
    private fun navigateMonth(direction: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth, 1)
        calendar.add(Calendar.MONTH, direction)
        
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH)
        
        updateMonthYearDisplay()
        generateCalendarDays()
        populateCalendarGrid()
        loadCalendarData()
    }
    
    private fun updateMonthYearDisplay() {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        binding.tvMonthYear.text = "${monthNames[currentMonth]} $currentYear"
    }
    
    private fun loadCalendarData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = authManager.getCurrentUser()
                val sessionToken = authManager.getCurrentSessionToken()
                
                if (user != null && sessionToken != null) {
                    val result = networkManager.getMealsForMonth(sessionToken, currentYear, currentMonth)
                    
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            val data = result.getOrThrow()
                            val mealsList = data["meals"] as? List<Map<String, Any>> ?: emptyList()
                            updateCalendarWithMeals(mealsList)
                        } else {
                            Toast.makeText(this@CalendarActivity, "Failed to load calendar data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CalendarActivity, "Error loading calendar data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateCalendarWithMeals(mealsList: List<Map<String, Any>>) {
        // Update calendar days with meal information
        // Group meals by date
        val mealsByDate = mealsList.groupBy { it["date"] as? String ?: "" }
        
        // Update each calendar day with meal information
        for (i in calendarDays.indices) {
            val calendarDay = calendarDays[i]
            val dateStr = String.format("%04d-%02d-%02d", 
                calendarDay.date.get(Calendar.YEAR),
                calendarDay.date.get(Calendar.MONTH) + 1,
                calendarDay.date.get(Calendar.DAY_OF_MONTH)
            )
            val hasMeals = mealsByDate[dateStr]?.isNotEmpty() ?: false
            
            // Update the calendar day
            calendarDays[i] = calendarDay.copy(hasMeals = hasMeals)
        }
        
        // Refresh the calendar grid
        populateCalendarGrid()
    }
    
    private fun loadMealsForDate(date: Calendar) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = authManager.getCurrentUser()
                val sessionToken = authManager.getCurrentSessionToken()
                
                if (user != null && sessionToken != null) {
                    val dateStr = String.format("%04d-%02d-%02d", 
                        date.get(Calendar.YEAR), 
                        date.get(Calendar.MONTH) + 1, 
                        date.get(Calendar.DAY_OF_MONTH)
                    )
                    val result = networkManager.getMealsForDate(sessionToken, dateStr)
                    
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            val data = result.getOrThrow()
                            val meals = data["meals"] as? List<MealLog> ?: emptyList()
                            updateMealDisplay(meals)
                        } else {
                            updateMealDisplay(emptyList())
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        updateMealDisplay(emptyList())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateMealDisplay(emptyList())
                }
            }
        }
    }
    
    private fun updateMealDisplay(meals: List<MealLog>) {
        val breakfastMeals = meals.filter { it.mealType.equals("breakfast", ignoreCase = true) }
        val lunchMeals = meals.filter { it.mealType.equals("lunch", ignoreCase = true) }
        val dinnerMeals = meals.filter { it.mealType.equals("dinner", ignoreCase = true) }
        
        binding.tvBreakfastMeal.text = if (breakfastMeals.isNotEmpty()) {
            breakfastMeals.joinToString(", ") { it.mealName }
        } else {
            "No meal logged"
        }
        
        binding.tvLunchMeal.text = if (lunchMeals.isNotEmpty()) {
            lunchMeals.joinToString(", ") { it.mealName }
        } else {
            "No meal logged"
        }
        
        binding.tvDinnerMeal.text = if (dinnerMeals.isNotEmpty()) {
            dinnerMeals.joinToString(", ") { it.mealName }
        } else {
            "No meal logged"
        }
    }
    
    private fun updateSelectedDateInfo() {
        selectedDate?.let { date ->
            val dayOfWeek = date.get(Calendar.DAY_OF_WEEK)
            
            val dayNames = arrayOf(
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
            )
            
            binding.tvSelectedDate.text = dayNames[dayOfWeek - 1]
        }
    }
    
    private fun saveWaterIntake() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = authManager.getCurrentUser()
                val sessionToken = authManager.getCurrentSessionToken()
                
                if (user != null && sessionToken != null) {
                    val result = networkManager.saveWaterIntake(sessionToken, user.id.toInt(), waterIntake)
                    
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            Toast.makeText(this@CalendarActivity, "Water intake saved", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CalendarActivity, "Failed to save water intake", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CalendarActivity, "Error saving water intake: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}