package com.beatwell.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beatwell.app.auth.AuthManager
import com.beatwell.app.databinding.ActivityMealLogHistoryBinding
import com.beatwell.app.databinding.MealLogHistoryItemBinding
import com.beatwell.app.models.MealLog
import com.beatwell.app.network.NetworkManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MealLogHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMealLogHistoryBinding
    private lateinit var networkManager: NetworkManager
    private lateinit var authManager: AuthManager
    private val adapter = MealHistoryAdapter()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealLogHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize managers
        networkManager = NetworkManager(this)
        authManager = AuthManager(this)
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Setup RecyclerView
        binding.recyclerViewMealHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMealHistory.adapter = adapter
        
        // Setup back button
        binding.ivBackButton.setOnClickListener {
            finish()
        }
        
        // Load meal history
        loadMealHistory()
    }
    
    private fun loadMealHistory() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                binding.recyclerViewMealHistory.visibility = View.GONE
                
                val sessionToken = authManager.getCurrentSessionToken()
                if (sessionToken != null) {
                    val result = networkManager.getAllMealsHistory(sessionToken)
                    
                    if (result.isSuccess) {
                        val data = result.getOrThrow()["data"] as? List<Map<String, Any>> ?: emptyList()
                        val mealLogs = data.map { mealMap ->
                            MealLog(
                                id = (mealMap["id"] as? Number)?.toInt() ?: 0,
                                mealType = mealMap["meal_type"] as? String ?: "",
                                mealName = mealMap["meal_option_name"] as? String ?: "",
                                calories = (mealMap["calories"] as? Number)?.toInt() ?: 0,
                                portionSize = (mealMap["portion_size"] as? Number)?.toFloat() ?: 0f,
                                loggedAt = try {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    sdf.parse(mealMap["logged_at"] as? String ?: "") ?: Date()
                                } catch (e: Exception) {
                                    Date()
                                }
                            )
                        }
                        
                        adapter.submitList(groupMealsByDate(mealLogs))
                    } else {
                        // Fallback to local data
                        loadMealHistoryFromLocal()
                    }
                } else {
                    loadMealHistoryFromLocal()
                }
            } catch (e: Exception) {
                // Fallback to local data on error
                loadMealHistoryFromLocal()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.recyclerViewMealHistory.visibility = View.VISIBLE
            }
        }
    }
    
    private fun loadMealHistoryFromLocal() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = authManager.getCurrentUserId()
                val meals = networkManager.databaseHelper.getMealsForMonth(userId, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH))
                
                val mealLogs = meals.map { mealMap ->
                    MealLog(
                        id = when (val idValue = mealMap["id"]) {
                            is Int -> idValue
                            is Long -> idValue.toInt()
                            is Number -> idValue.toInt()
                            else -> (idValue as? Number)?.toInt() ?: 0
                        },
                        mealType = mealMap["meal_type"] as? String ?: "",
                        mealName = mealMap["meal_option_name"] as? String ?: "",
                        calories = (mealMap["calories"] as? Number)?.toInt() ?: 0,
                        portionSize = (mealMap["portion_size"] as? Number)?.toFloat() ?: 0f,
                        loggedAt = try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            sdf.parse(mealMap["logged_at"] as? String ?: "") ?: Date()
                        } catch (e: Exception) {
                            Date()
                        }
                    )
                }
                
                withContext(Dispatchers.Main) {
                    adapter.submitList(groupMealsByDate(mealLogs))
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private fun groupMealsByDate(meals: List<MealLog>): List<MealLogGrouped> {
        val groupedMeals = meals.groupBy { meal ->
            val calendar = Calendar.getInstance().apply {
                time = meal.loggedAt
            }
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        }
        
        return groupedMeals.entries.map { (date, meals) ->
            MealLogGrouped(date, meals.sortedBy { it.loggedAt })
        }.sortedByDescending { it.date }
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            
            // Format as "Day, Month DD"
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
            val monthDay = SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            
            "$dayOfWeek, $monthDay"
        } catch (e: Exception) {
            dateString
        }
    }
    
    private fun isToday(dateString: String): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return dateString == today
    }
    
    private fun isYesterday(dateString: String): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        return dateString == yesterday
    }
    
    private inner class MealHistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var items = emptyList<MealLogGrouped>()
        
        fun submitList(items: List<MealLogGrouped>) {
            this.items = items
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding = MealLogHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MealLogViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is MealLogViewHolder) {
                holder.bind(items[position])
            }
        }
        
        override fun getItemCount() = items.size
    }
    
    private inner class MealLogViewHolder(private val binding: MealLogHistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(groupedMeal: MealLogGrouped) {
            // Set date header
            val dateLabel = when {
                isToday(groupedMeal.date) -> {
                    val calendar = Calendar.getInstance()
                    val monthDay = SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
                    "Today, $monthDay"
                }
                isYesterday(groupedMeal.date) -> {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                    val monthDay = SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
                    "Yesterday, $monthDay"
                }
                else -> formatDate(groupedMeal.date)
            }
            binding.tvDateHeader.text = dateLabel
            
            // Find and set meal types
            val breakfast = groupedMeal.meals.find { it.mealType.equals("breakfast", ignoreCase = true) }
            val lunch = groupedMeal.meals.find { it.mealType.equals("lunch", ignoreCase = true) }
            val dinner = groupedMeal.meals.find { it.mealType.equals("dinner", ignoreCase = true) }
            
            // Set meal type icons
            binding.ivBreakfast.visibility = if (breakfast != null) View.VISIBLE else View.GONE
            binding.ivLunch.visibility = if (lunch != null) View.VISIBLE else View.GONE
            binding.ivDinner.visibility = if (dinner != null) View.VISIBLE else View.GONE
            
            // Set meal type labels
            binding.tvBreakfast.visibility = if (breakfast != null) View.VISIBLE else View.GONE
            binding.tvLunch.visibility = if (lunch != null) View.VISIBLE else View.GONE
            binding.tvDinner.visibility = if (dinner != null) View.VISIBLE else View.GONE
        }
    }
    
    data class MealLogGrouped(
        val date: String,
        val meals: List<MealLog>
    )
}

