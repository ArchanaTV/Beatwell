package com.beatwell.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.beatwell.app.auth.AuthManager
import com.beatwell.app.auth.AuthResult
import com.beatwell.app.databinding.ActivityMainBinding
import com.beatwell.app.network.NetworkManager
import com.beatwell.app.utils.BottomNavigationHelper
import kotlinx.coroutines.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager
    private lateinit var networkManager: NetworkManager
    private lateinit var bottomNavigationHelper: BottomNavigationHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize auth manager
        authManager = AuthManager(this)
        
        // Initialize network manager
        networkManager = NetworkManager(this)
        
        // Initialize bottom navigation
        bottomNavigationHelper = BottomNavigationHelper(this)
        bottomNavigationHelper.setupBottomNavigation()
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Load logo from assets
        loadLogoFromAssets()
        
        // Set up click listeners
        setupClickListeners()
        
        // Initialize UI
        initializeUI()
        
        // Check authentication and load user data
        checkAuthentication()
    }

    override fun onResume() {
        super.onResume()
        // Refresh dashboard data when returning from other activities
        loadDashboardData()
    }
    
    private fun loadLogoFromAssets() {
        try {
            val inputStream = assets.open("images/logo.png")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.ivAppBarLogo.setImageBitmap(bitmap)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            // Fallback to default icon if logo loading fails
            binding.ivAppBarLogo.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
    
    private fun setupClickListeners() {
        // Meal card click listeners
        binding.cardBreakfast.setOnClickListener {
            navigateToMealScreen(MealActivity.MEAL_BREAKFAST)
        }
        
        binding.cardLunch.setOnClickListener {
            navigateToMealScreen(MealActivity.MEAL_LUNCH)
        }
        
        binding.cardDinner.setOnClickListener {
            navigateToMealScreen(MealActivity.MEAL_DINNER)
        }
        
        // User profile click listener
        binding.ivUserProfile.setOnClickListener {
            Toast.makeText(this, "Profile settings coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Debug access - long press on logo
        binding.ivAppBarLogo.setOnLongClickListener {
            val intent = Intent(this, DebugActivity::class.java)
            startActivity(intent)
            true
        }
        
        // Bottom navigation click listeners
        // Bottom navigation is handled by BottomNavigationHelper
    }
    
    
    private fun navigateToMealScreen(mealType: String) {
        val intent = Intent(this, MealActivity::class.java)
        intent.putExtra(MealActivity.EXTRA_MEAL_TYPE, mealType)
        startActivity(intent)
    }
    
    private fun initializeUI() {
        // Set current time and date
        updateTimeAndDate()
        
        // Set up a timer to update time every minute
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateTimeAndDate()
                }
            }
        }, 0, 60000) // Update every minute
    }
    
    private fun updateTimeAndDate() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
        
        binding.tvCurrentTime.text = currentTime
        binding.tvCurrentDate.text = currentDate
    }
    
    private fun performLogout() {
        // Logout functionality will be handled in profile screen
        Toast.makeText(this, "Logout functionality will be available in profile screen", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkAuthentication() {
        // Check if user is logged in locally (synchronous check only)
        if (!authManager.isLoggedInSync()) {
            Toast.makeText(this, "Please log in to continue", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        // Load user data from local storage (no API verification needed on launch)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = authManager.getCurrentUser()
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        displayUserInfo(user)
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Authentication error: ${e.message}", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
    
    private fun displayUserInfo(user: com.beatwell.app.models.User) {
        // Display user information in the UI
        binding.tvUserName.text = "Welcome back, ${user.firstName}!"
        
        // Load dashboard data from API
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionToken = authManager.getCurrentSessionToken()
                if (sessionToken != null) {
                    val result = networkManager.getDashboardData(sessionToken)
                    
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            val data = result.getOrThrow()["data"] as? Map<String, Any>
                            if (data != null) {
                                updateProgressStats(data)
                                android.util.Log.d("MainActivity", "Dashboard data loaded successfully from API")
                            } else {
                                android.util.Log.e("MainActivity", "Dashboard API returned null data")
                                Toast.makeText(this@MainActivity, "Failed to load dashboard data", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.util.Log.e("MainActivity", "Dashboard API failed: ${result.exceptionOrNull()?.message}")
                            Toast.makeText(this@MainActivity, "Failed to load dashboard: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("MainActivity", "No session token available")
                        Toast.makeText(this@MainActivity, "No session token - please login again", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("MainActivity", "Dashboard loading exception: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Dashboard error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadLocalDashboardData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = authManager.getCurrentUser()
                if (user != null) {
                    android.util.Log.d("MainActivity", "Loading data for user ID: ${user.id}")
                    
                    val todayMeals = networkManager.databaseHelper.getTodayMeals(user.id)
                    val waterIntake = networkManager.databaseHelper.getTodayWaterIntake(user.id)
                    
                    android.util.Log.d("MainActivity", "Today's meals count: ${todayMeals.size}")
                    android.util.Log.d("MainActivity", "Water intake: $waterIntake")
                    
                    // Debug: Log each meal
                    todayMeals.forEach { meal ->
                        android.util.Log.d("MainActivity", "Meal: ${meal["meal_type"]} - ${meal["calories"]} calories")
                    }
                    
                    // Calculate totals
                    val totalCalories = todayMeals.sumOf { (it["calories"] as? Number)?.toInt() ?: 0 }
                    val mealsCompleted = todayMeals.size
                    val mealsByType = todayMeals.groupBy { it["meal_type"] as? String ?: "" }
                    
                    android.util.Log.d("MainActivity", "Total calories: $totalCalories, Meals completed: $mealsCompleted")
                    
                    val progressData = mapOf(
                        "calories_consumed" to totalCalories,
                        "water_intake" to waterIntake,
                        "meals_completed" to mealsCompleted,
                        "meals_total" to 3,
                        "breakfast" to (mealsByType["breakfast"]?.isNotEmpty() == true),
                        "lunch" to (mealsByType["lunch"]?.isNotEmpty() == true),
                        "dinner" to (mealsByType["dinner"]?.isNotEmpty() == true)
                    )
                    
                    withContext(Dispatchers.Main) {
                        updateProgressStats(mapOf("progress" to progressData))
                    }
                } else {
                    android.util.Log.e("MainActivity", "No user found in session")
                    withContext(Dispatchers.Main) {
                        updateProgressStats(null)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error loading local dashboard data: ${e.message}")
                withContext(Dispatchers.Main) {
                    updateProgressStats(null)
                }
            }
        }
    }
    
    private fun updateProgressStats(data: Map<String, Any>?) {
        if (data != null) {
            val progress = data["progress"] as? Map<String, Any>
            if (progress != null) {
                updateDynamicProgressCard(progress)
            }
        } else {
            // Show empty state
            updateDynamicProgressCard(emptyMap())
        }
    }
    
    private fun updateDynamicProgressCard(progress: Map<String, Any>) {
        // Extract data with defaults
        val caloriesConsumed = (progress["calories_consumed"] as? Number)?.toInt() ?: 0
        val waterIntake = (progress["water_intake"] as? Number)?.toInt() ?: 0
        val mealsCompleted = (progress["meals_completed"] as? Number)?.toInt() ?: 0
        val mealsTotal = (progress["meals_total"] as? Number)?.toInt() ?: 3
        
        // Set goals
        val caloriesGoal = 2000
        val waterGoal = 8
        
        // Update main stats
        binding.tvCaloriesConsumed.text = caloriesConsumed.toString()
        binding.tvWaterIntake.text = waterIntake.toString()
        binding.tvMealsCompleted.text = mealsCompleted.toString()
        
        // Update goal displays
        binding.tvCaloriesGoal.text = "/ $caloriesGoal"
        binding.tvWaterGoal.text = "/ $waterGoal"
        binding.tvMealsGoal.text = "/ $mealsTotal"
        
        // Calculate progress percentages
        val caloriesProgress = if (caloriesGoal > 0) (caloriesConsumed * 100 / caloriesGoal).coerceAtMost(100) else 0
        val waterProgress = if (waterGoal > 0) (waterIntake * 100 / waterGoal).coerceAtMost(100) else 0
        val mealsProgress = if (mealsTotal > 0) (mealsCompleted * 100 / mealsTotal).coerceAtMost(100) else 0
        
        // Update progress bars
        binding.progressCalories.progress = caloriesProgress
        binding.progressWater.progress = waterProgress
        binding.progressMeals.progress = mealsProgress
        
        // Update progress percentages
        binding.tvCaloriesProgress.text = "$caloriesProgress%"
        binding.tvWaterProgress.text = "$waterProgress%"
        binding.tvMealsProgress.text = "$mealsProgress%"
        
        // Update status and motivational message
        updateStatusAndMotivation(caloriesProgress, waterProgress, mealsProgress, mealsCompleted)
    }
    
    private fun updateStatusAndMotivation(caloriesProgress: Int, waterProgress: Int, mealsProgress: Int, mealsCompleted: Int) {
        val overallProgress = (caloriesProgress + waterProgress + mealsProgress) / 3
        
        when {
            overallProgress >= 80 -> {
                binding.tvProgressStatus.text = "Excellent!"
                binding.tvProgressStatus.setTextColor(ContextCompat.getColor(this, R.color.status_excellent))
                binding.tvProgressStatus.setBackgroundResource(R.drawable.status_badge_background)
                binding.tvMotivationalMessage.text = "Amazing progress! You're crushing your health goals! 🎉"
            }
            overallProgress >= 60 -> {
                binding.tvProgressStatus.text = "Good Progress!"
                binding.tvProgressStatus.setTextColor(ContextCompat.getColor(this, R.color.status_good))
                binding.tvProgressStatus.setBackgroundResource(R.drawable.status_badge_background)
                binding.tvMotivationalMessage.text = "Great job! Keep up the healthy habits! 💪"
            }
            overallProgress >= 30 -> {
                binding.tvProgressStatus.text = "Getting There"
                binding.tvProgressStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                binding.tvProgressStatus.setBackgroundResource(R.drawable.status_badge_background)
                binding.tvMotivationalMessage.text = "You're making progress! Every step counts! 🌟"
            }
            else -> {
                binding.tvProgressStatus.text = "Let's Start!"
                binding.tvProgressStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                binding.tvProgressStatus.setBackgroundResource(R.drawable.status_badge_background)
                when {
                    mealsCompleted == 0 -> binding.tvMotivationalMessage.text = "Start your day with a healthy breakfast! 🌅"
                    waterProgress < 30 -> binding.tvMotivationalMessage.text = "Don't forget to stay hydrated! 💧"
                    caloriesProgress < 30 -> binding.tvMotivationalMessage.text = "Fuel your body with nutritious meals! 🥗"
                    else -> binding.tvMotivationalMessage.text = "Every healthy choice makes a difference! ✨"
                }
            }
        }
    }
    
    // Removed connectivity status notifications for unified approach
}

