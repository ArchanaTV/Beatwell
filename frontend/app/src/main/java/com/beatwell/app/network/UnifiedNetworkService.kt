package com.beatwell.app.network

import android.content.Context
import android.util.Log
import com.beatwell.app.database.DatabaseHelper
import com.beatwell.app.models.User
import com.beatwell.app.models.MealLog
import com.beatwell.app.models.MealOption
import com.beatwell.app.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unified Network Service - Single entry point for all network operations
 * Works seamlessly regardless of internet connectivity
 * No more hybrid approach - just unified, reliable service
 */
class UnifiedNetworkService(private val context: Context) {
    
    private val databaseHelper = DatabaseHelper(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        private const val TAG = "UnifiedNetworkService"
        private const val TIMEOUT_MS = 15000 // 15 seconds for better reliability
    }
    
    /**
     * Register user - Unified approach
     * Always tries to save locally first, then syncs in background if possible
     */
    suspend fun registerUser(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        firstName: String,
        lastName: String,
        phone: String,
        dateOfBirth: String,
        gender: String,
        address: String,
        city: String,
        state: String,
        zipCode: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        medicalConditions: String,
        allergies: String
    ): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            // Check if user already exists
            if (databaseHelper.checkUserExists(username, email)) {
                return@withContext Result.failure(Exception("User already exists"))
            }
            
            // Create user object
            val passwordHash = PasswordUtils.hashPassword(password)
            val user = User(
                username = username,
                email = email,
                passwordHash = passwordHash,
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                dateOfBirth = convertDateFormat(dateOfBirth),
                gender = gender,
                address = address,
                city = city,
                state = state,
                zipCode = zipCode,
                emergencyContactName = emergencyContactName,
                emergencyContactPhone = emergencyContactPhone,
                medicalConditions = medicalConditions,
                allergies = allergies
            )
            
            // Save user locally (primary storage)
            val userId = databaseHelper.insertUser(user)
            if (userId <= 0) {
                return@withContext Result.failure(Exception("Failed to create user"))
            }
            
            // Create session
            val sessionToken = PasswordUtils.generateSessionToken()
            val expiresAt = getExpirationDate()
            databaseHelper.createSession(userId, sessionToken, expiresAt)
            
            val userData = user.copy(id = userId).toMap().toMutableMap()
            userData["session_token"] = sessionToken
            userData["expires_at"] = expiresAt
            
            val result = mapOf(
                "success" to true,
                "message" to "User registered successfully",
                "data" to userData
            )
            
            // Background sync (non-blocking)
            syncUserToServer(userData)
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Login user - Unified approach
     */
    suspend fun loginUser(username: String, password: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            val user = databaseHelper.getUserByUsername(username) ?: databaseHelper.getUserByEmail(username)
            if (user == null) {
                return@withContext Result.failure(Exception("User not found"))
            }
            
            if (!PasswordUtils.verifyPassword(password, user.passwordHash)) {
                return@withContext Result.failure(Exception("Invalid password"))
            }
            
            // Create new session
            val sessionToken = PasswordUtils.generateSessionToken()
            val expiresAt = getExpirationDate()
            databaseHelper.deleteAllSessionsForUser(user.id)
            databaseHelper.createSession(user.id, sessionToken, expiresAt)
            
            val userData = user.toMap().toMutableMap()
            userData["session_token"] = sessionToken
            userData["expires_at"] = expiresAt
            
            val result = mapOf(
                "success" to true,
                "message" to "Login successful",
                "data" to userData
            )
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Verify session - Unified approach
     */
    suspend fun verifySession(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            val sessionData = databaseHelper.getValidSession(sessionToken)
            if (sessionData == null) {
                return@withContext Result.failure(Exception("Invalid or expired session"))
            }
            
            val (user, expiresAt) = sessionData
            if (user == null) {
                return@withContext Result.failure(Exception("Invalid user session"))
            }
            
            val userData = user.toMap().toMutableMap()
            userData["expires_at"] = expiresAt ?: ""
            
            val result = mapOf(
                "success" to true,
                "message" to "Session valid",
                "data" to userData
            )
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Session verification failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Logout user - Unified approach
     */
    suspend fun logoutUser(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            val success = databaseHelper.deleteSession(sessionToken)
            val result = mapOf(
                "success" to success,
                "message" to if (success) "Logout successful" else "Logout failed"
            )
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get dashboard data - Unified approach
     */
    suspend fun getDashboardData(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            val sessionData = databaseHelper.getValidSession(sessionToken)
            if (sessionData == null) {
                return@withContext Result.failure(Exception("Invalid or expired session"))
            }
            
            val (user, _) = sessionData
            if (user == null) {
                return@withContext Result.failure(Exception("Invalid user session"))
            }
            
            // Get real data from local database
            val todayMeals = databaseHelper.getTodayMeals(user.id)
            val waterIntake = databaseHelper.getTodayWaterIntake(user.id)
            
            // Calculate totals
            val totalCalories = todayMeals.sumOf { (it["calories"] as? Number)?.toInt() ?: 0 }
            val mealsCompleted = todayMeals.size
            val mealsByType = todayMeals.groupBy { it["meal_type"] as? String ?: "" }
            
            val result = mapOf(
                "success" to true,
                "message" to "Dashboard data retrieved",
                "data" to mapOf(
                    "user" to mapOf(
                        "id" to user.id,
                        "username" to user.username,
                        "first_name" to user.firstName,
                        "last_name" to user.lastName
                    ),
                    "meal_status" to mapOf(
                        "breakfast" to (mealsByType["breakfast"]?.isNotEmpty() == true),
                        "lunch" to (mealsByType["lunch"]?.isNotEmpty() == true),
                        "dinner" to (mealsByType["dinner"]?.isNotEmpty() == true)
                    ),
                    "progress" to mapOf(
                        "calories_consumed" to totalCalories,
                        "calories_goal" to 2000,
                        "water_intake" to waterIntake,
                        "water_goal" to 8,
                        "meals_completed" to mealsCompleted,
                        "meals_total" to 3
                    ),
                    "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
            )
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get dashboard data: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Save meal data - Unified approach
     */
    suspend fun saveMealData(
        sessionToken: String,
        mealType: String,
        mealOption: MealOption,
        portionSize: Float,
        calories: Int
    ): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            // Get user from session
            val sessionData = databaseHelper.getValidSession(sessionToken)
            if (sessionData == null) {
                return@withContext Result.failure(Exception("Invalid or expired session"))
            }
            
            val (user, _) = sessionData
            if (user == null) {
                return@withContext Result.failure(Exception("Invalid user session"))
            }
            
            // Save meal to local database
            val mealId = databaseHelper.insertMealLog(
                userId = user.id,
                mealType = mealType,
                mealOptionId = mealOption.id,
                mealOptionName = mealOption.name,
                mealOptionDescription = mealOption.description,
                portionSize = portionSize,
                calories = calories,
                isCustom = false
            )
            
            if (mealId <= 0) {
                return@withContext Result.failure(Exception("Failed to save meal"))
            }
            
            val result = mapOf(
                "success" to true,
                "message" to "Meal saved successfully!",
                "data" to mapOf(
                    "meal_id" to mealId,
                    "meal_type" to mealType,
                    "meal_option" to mealOption.name,
                    "portion_size" to portionSize,
                    "calories" to calories,
                    "timestamp" to dateFormat.format(Date())
                )
            )
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save meal: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Save custom food - Unified approach
     */
    suspend fun saveCustomFood(
        sessionToken: String,
        mealType: String,
        foodName: String,
        notes: String,
        portionSize: Float,
        calories: Int
    ): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            // Get user from session
            val sessionData = databaseHelper.getValidSession(sessionToken)
            if (sessionData == null) {
                return@withContext Result.failure(Exception("Invalid or expired session"))
            }
            
            val (user, _) = sessionData
            if (user == null) {
                return@withContext Result.failure(Exception("Invalid user session"))
            }
            
            // Save custom food to local database
            val mealId = databaseHelper.insertMealLog(
                userId = user.id,
                mealType = mealType,
                mealOptionId = -1, // Custom food ID
                mealOptionName = foodName,
                mealOptionDescription = notes,
                portionSize = portionSize,
                calories = calories,
                isCustom = true
            )
            
            if (mealId <= 0) {
                return@withContext Result.failure(Exception("Failed to save custom food"))
            }
            
            val result = mapOf(
                "success" to true,
                "message" to "Food saved successfully!",
                "data" to mapOf(
                    "meal_id" to mealId,
                    "meal_type" to mealType,
                    "food_name" to foodName,
                    "notes" to notes,
                    "portion_size" to portionSize,
                    "calories" to calories,
                    "timestamp" to dateFormat.format(Date())
                )
            )
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom food: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get today's meals - Unified approach
     */
    suspend fun getTodayMeals(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            val sessionData = databaseHelper.getValidSession(sessionToken)
            if (sessionData == null) {
                return@withContext Result.failure(Exception("Invalid or expired session"))
            }
            
            val (user, _) = sessionData
            if (user == null) {
                return@withContext Result.failure(Exception("Invalid user session"))
            }
            
            // Get today's meals from local database
            val meals = databaseHelper.getTodayMeals(user.id)
            
            // Group meals by type
            val breakfastMeals = meals.filter { it["meal_type"] == "breakfast" }
            val lunchMeals = meals.filter { it["meal_type"] == "lunch" }
            val dinnerMeals = meals.filter { it["meal_type"] == "dinner" }
            
            // Calculate total calories
            val totalCalories = meals.sumOf { (it["calories"] as? Number)?.toInt() ?: 0 }
            
            val result = mapOf(
                "success" to true,
                "message" to "Today's meals retrieved",
                "data" to mapOf(
                    "meals" to meals,
                    "summary" to mapOf(
                        "total_calories" to totalCalories,
                        "meals_count" to meals.size,
                        "breakfast" to breakfastMeals,
                        "lunch" to lunchMeals,
                        "dinner" to dinnerMeals
                    )
                )
            )
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get today's meals: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Save water intake - Unified approach
     */
    suspend fun saveWaterIntake(sessionToken: String, glasses: Int): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            val sessionData = databaseHelper.getValidSession(sessionToken)
            if (sessionData == null) {
                return@withContext Result.failure(Exception("Invalid or expired session"))
            }
            
            val (user, _) = sessionData
            if (user == null) {
                return@withContext Result.failure(Exception("Invalid user session"))
            }
            
            val waterId = databaseHelper.saveWaterIntake(user.id, glasses)
            
            val result = mapOf(
                "success" to true,
                "message" to "Water intake saved successfully",
                "data" to mapOf(
                    "water_id" to waterId,
                    "user_id" to user.id,
                    "glasses" to glasses,
                    "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    "timestamp" to dateFormat.format(Date())
                )
            )
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save water intake: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get user profile - Unified approach
     */
    suspend fun getUserProfile(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            val sessionData = databaseHelper.getValidSession(sessionToken)
            if (sessionData == null) {
                return@withContext Result.failure(Exception("Invalid or expired session"))
            }
            
            val (user, _) = sessionData
            if (user == null) {
                return@withContext Result.failure(Exception("Invalid user session"))
            }
            
            val profileData = user.toMap().toMutableMap()
            profileData["age"] = user.age
            
            val result = mapOf(
                "success" to true,
                "message" to "Profile retrieved successfully",
                "data" to profileData
            )
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user profile: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Update user profile - Unified approach
     */
    suspend fun updateUserProfile(sessionToken: String, profileData: Map<String, Any>): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            val sessionData = databaseHelper.getValidSession(sessionToken)
            if (sessionData == null) {
                return@withContext Result.failure(Exception("Invalid or expired session"))
            }
            
            val (user, _) = sessionData
            if (user == null) {
                return@withContext Result.failure(Exception("Invalid user session"))
            }
            
            // Update user locally
            val updatedUser = user.copy(
                firstName = profileData["first_name"] as? String ?: user.firstName,
                lastName = profileData["last_name"] as? String ?: user.lastName,
                phone = profileData["phone"] as? String ?: user.phone,
                dateOfBirth = profileData["date_of_birth"] as? String ?: user.dateOfBirth,
                gender = profileData["gender"] as? String ?: user.gender,
                address = profileData["address"] as? String ?: user.address,
                city = profileData["city"] as? String ?: user.city,
                state = profileData["state"] as? String ?: user.state,
                zipCode = profileData["zip_code"] as? String ?: user.zipCode,
                emergencyContactName = profileData["emergency_contact_name"] as? String ?: user.emergencyContactName,
                emergencyContactPhone = profileData["emergency_contact_phone"] as? String ?: user.emergencyContactPhone,
                medicalConditions = profileData["medical_conditions"] as? String ?: user.medicalConditions,
                allergies = profileData["allergies"] as? String ?: user.allergies
            )
            
            val success = databaseHelper.updateUser(updatedUser)
            if (success) {
                val result = mapOf(
                    "success" to true,
                    "message" to "Profile updated successfully",
                    "data" to updatedUser.toMap()
                )
                return@withContext Result.success(result)
            } else {
                return@withContext Result.failure(Exception("Failed to update profile"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    // Private helper methods
    
    private fun getExpirationDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 30) // 30 days from now
        return dateFormat.format(calendar.time)
    }
    
    private fun convertDateFormat(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }
    
    /**
     * Background sync to server (non-blocking)
     */
    private fun syncUserToServer(userData: Map<String, Any?>) {
        // This would sync to server in background if needed
        // For now, we focus on local-first approach
        Log.d(TAG, "Background sync would happen here if server is available")
    }
}
