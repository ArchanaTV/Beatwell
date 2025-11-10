package com.beatwell.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.beatwell.app.auth.AuthManager
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

class NetworkManager(private val context: Context) {
    
    val databaseHelper = DatabaseHelper(context)
    private val baseUrl = ApiConfig.USERS_API
    private val homeApiUrl = ApiConfig.HOME_API
    private val mealsApiUrl = ApiConfig.MEALS_API
    private val calendarApiUrl = ApiConfig.CALENDAR_API
    
    companion object {
        private const val TAG = "NetworkManager"
        private val TIMEOUT_MS = ApiConfig.TIMEOUT_MS
    }

    /**
     * Test API connectivity
     */
    suspend fun testApiConnectivity(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConfig.BASE_URL.replace("/api", "")}/health.php")
            Log.d(TAG, "Testing API connectivity to: $url")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                "HTTP $responseCode"
            }
            
            Log.d(TAG, "API connectivity test result: $responseCode - $response")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Result.success("API is reachable: $response")
            } else {
                Result.failure(Exception("API returned HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API connectivity test failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Check if device has internet connectivity
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        val isAvailable = when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        Log.d(TAG, "Network availability check: $isAvailable")
        return isAvailable
    }

    /**
     * Register user - ONLINE-ONLY MODE
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
        
        // Check network availability
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
        }
        
        try {
            Log.d(TAG, "Attempting API registration for user: $username")
            val apiResult = registerUserViaAPI(
                username, email, password, confirmPassword,
                firstName, lastName, phone, dateOfBirth, gender,
                address, city, state, zipCode,
                emergencyContactName, emergencyContactPhone,
                medicalConditions, allergies
            )
            
            if (apiResult.isSuccess) {
                Log.d(TAG, "API registration successful")
                return@withContext apiResult
            } else {
                Log.e(TAG, "API registration failed: ${apiResult.exceptionOrNull()?.message}")
                return@withContext apiResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Login user - ONLINE-ONLY MODE
     */
    suspend fun loginUser(username: String, password: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Check network availability
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
        }
        
        try {
            Log.d(TAG, "Attempting API login for user: $username")
            val apiResult = loginUserViaAPI(username, password)
            
            if (apiResult.isSuccess) {
                Log.d(TAG, "API login successful")
                return@withContext apiResult
            } else {
                Log.e(TAG, "API login failed: ${apiResult.exceptionOrNull()?.message}")
                return@withContext apiResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Verify session with hybrid approach
     */
    suspend fun verifySession(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Try API first if network is available
        if (isNetworkAvailable()) {
            try {
                val apiResult = verifySessionViaAPI(sessionToken)
                if (apiResult.isSuccess) {
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "API session verification failed, falling back to local storage: ${e.message}")
            }
        }

        // Fallback to local storage
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
                "message" to "Session valid (offline mode)",
                "data" to userData
            )
            return@withContext Result.success(result)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Logout user
     */
    suspend fun logoutUser(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Try API first if network is available
        if (isNetworkAvailable()) {
            try {
                val apiResult = logoutUserViaAPI(sessionToken)
                // Also delete locally regardless of API result
                databaseHelper.deleteSession(sessionToken)
                return@withContext apiResult
            } catch (e: Exception) {
                Log.w(TAG, "API logout failed, using local storage: ${e.message}")
            }
        }

        // Fallback to local storage
        try {
            val success = databaseHelper.deleteSession(sessionToken)
            val result = mapOf(
                "success" to success,
                "message" to if (success) "Logout successful (offline mode)" else "Logout failed"
            )
            return@withContext Result.success(result)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get dashboard data for home screen - ONLINE-ONLY MODE
     */
    suspend fun getDashboardData(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Check network availability
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
        }
        
        try {
            Log.d(TAG, "Getting dashboard data from API")
            val apiResult = getDashboardDataViaAPI(sessionToken)
            
            if (apiResult.isSuccess) {
                Log.d(TAG, "Successfully retrieved dashboard data from API")
                return@withContext apiResult
            } else {
                Log.e(TAG, "Failed to get dashboard data from API: ${apiResult.exceptionOrNull()?.message}")
                return@withContext apiResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting dashboard data: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Save meal data - ONLINE-ONLY MODE
     */
    suspend fun saveMealData(
        mealType: String,
        mealOption: com.beatwell.app.models.MealOption,
        portionSize: Float,
        calories: Int
    ): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Check network availability
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
        }
        
        try {
            Log.d(TAG, "Saving meal data to server: $mealType - ${mealOption.name}")
            val apiResult = saveMealDataViaAPI(mealType, mealOption, portionSize, calories)
            
            if (apiResult.isSuccess) {
                Log.d(TAG, "Meal saved to server successfully")
                return@withContext apiResult
            } else {
                Log.e(TAG, "Failed to save meal to server: ${apiResult.exceptionOrNull()?.message}")
                return@withContext apiResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving meal: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get today's meals
     */
    suspend fun getTodayMeals(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Try API first if network is available
        if (isNetworkAvailable()) {
            try {
                val apiResult = getTodayMealsViaAPI(sessionToken)
                if (apiResult.isSuccess) {
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "API get today's meals failed, using local data: ${e.message}")
            }
        }

        // Fallback to local data
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
                "message" to "Today's meals retrieved (offline mode)",
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
            return@withContext Result.failure(e)
        }
    }

    /**
     * Save custom food data - LOCAL-FIRST APPROACH
     * Saves to local SQLite immediately, syncs with backend in background if online
     */
    suspend fun saveCustomFood(
        mealType: String,
        foodName: String,
        notes: String,
        portionSize: Float,
        calories: Int
    ): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        try {
            // STEP 1: Save to local database immediately (primary storage)
            val mealId = storeCustomMealLocally(0, mealType, foodName, notes, portionSize, calories)
            
            if (mealId <= 0) {
                Log.e(TAG, "Failed to save custom food locally - invalid meal ID: $mealId")
                return@withContext Result.failure(Exception("Failed to save custom food locally"))
            }
            
            // STEP 2: Return success immediately to user
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
                    "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
            )
            
            // STEP 3: Try to sync with backend in background (optional, non-blocking)
            if (isNetworkAvailable()) {
                try {
                    saveCustomFoodViaAPI(mealType, foodName, notes, portionSize, calories)
                    Log.d(TAG, "Custom food synced with backend successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Background sync failed (data still saved locally): ${e.message}")
                    // Data is still in local DB, so no problem!
                }
            }
            
            return@withContext Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom food: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get meals for a specific month - ONLINE-ONLY MODE
     */
    suspend fun getMealsForMonth(sessionToken: String, year: Int, month: Int): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Check network availability
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
        }
        
        // Get user ID from stored session info
        val authManager = AuthManager(context)
        val userId = authManager.getCurrentUserId()
        
        if (userId <= 0) {
            return@withContext Result.failure(Exception("Invalid user session"))
        }
        
        try {
            Log.d(TAG, "Getting meals for month: $year-$month for user: $userId")
            val apiResult = getMealsForMonthViaAPI(userId, year, month)
            
            if (apiResult.isSuccess) {
                Log.d(TAG, "Successfully retrieved meals for month from API")
                return@withContext apiResult
            } else {
                Log.e(TAG, "Failed to get meals for month from API: ${apiResult.exceptionOrNull()?.message}")
                return@withContext apiResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting meals for month: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get meals for a specific date - ONLINE-ONLY MODE
     */
    suspend fun getMealsForDate(sessionToken: String, date: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Check network availability
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
        }
        
        // Get user ID from stored session info
        val authManager = AuthManager(context)
        val userId = authManager.getCurrentUserId()
        
        if (userId <= 0) {
            return@withContext Result.failure(Exception("Invalid user session"))
        }
        
        try {
            Log.d(TAG, "Getting meals for date: $date for user: $userId")
            val apiResult = getMealsForDateViaAPI(userId, date)
            
            if (apiResult.isSuccess) {
                Log.d(TAG, "Successfully retrieved meals for date from API")
                return@withContext apiResult
            } else {
                Log.e(TAG, "Failed to get meals for date from API: ${apiResult.exceptionOrNull()?.message}")
                return@withContext apiResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting meals for date: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get user profile - ONLINE-ONLY MODE
     */
    suspend fun getUserProfile(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Check network availability
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
        }
        
        try {
            Log.d(TAG, "Getting user profile from API")
            val apiResult = getUserProfileViaAPI(sessionToken)
            
            if (apiResult.isSuccess) {
                Log.d(TAG, "Successfully retrieved user profile from API")
                return@withContext apiResult
            } else {
                Log.e(TAG, "Failed to get user profile from API: ${apiResult.exceptionOrNull()?.message}")
                return@withContext apiResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get all meals history - hybrid approach
     */
    suspend fun getAllMealsHistory(sessionToken: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Get user from session
        val sessionData = databaseHelper.getValidSession(sessionToken)
        if (sessionData == null) {
            return@withContext Result.failure(Exception("Invalid or expired session"))
        }

        val (user, _) = sessionData
        if (user == null) {
            return@withContext Result.failure(Exception("Invalid user session"))
        }
        
        // Try API first if network is available
        if (isNetworkAvailable()) {
            try {
                val apiResult = getAllMealsHistoryViaAPI(sessionToken)
                if (apiResult.isSuccess) {
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "API get all meals history failed, using local data: ${e.message}")
            }
        }

        // Fallback to local data - get last 50 meals
        try {
            val meals = databaseHelper.getAllMealsHistory(user.id, limit = 50)
            
            val result = mapOf(
                "success" to true,
                "message" to "Meal history retrieved successfully (offline mode)",
                "data" to meals
            )
            return@withContext Result.success(result)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(sessionToken: String, profileData: Map<String, Any>): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Try API first if network is available
        if (isNetworkAvailable()) {
            try {
                val apiResult = updateUserProfileViaAPI(sessionToken, profileData)
                if (apiResult.isSuccess) {
                    // Also update locally
                    updateUserProfileLocally(profileData)
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "API update profile failed, updating locally: ${e.message}")
            }
        }

        // Fallback to local storage
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
                    "message" to "Profile updated successfully (offline mode)",
                    "data" to updatedUser.toMap()
                )
                return@withContext Result.success(result)
            } else {
                return@withContext Result.failure(Exception("Failed to update profile locally"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Save water intake - ONLINE-ONLY MODE
     */
    suspend fun saveWaterIntake(sessionToken: String, userId: Int, glasses: Int): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
        
        // Check network availability
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
        }
        
        try {
            Log.d(TAG, "Saving water intake to API: $glasses glasses for user $userId")
            val apiResult = saveWaterIntakeViaAPI(sessionToken, userId, glasses)
            
            if (apiResult.isSuccess) {
                Log.d(TAG, "Successfully saved water intake to API")
                return@withContext apiResult
            } else {
                Log.e(TAG, "Failed to save water intake to API: ${apiResult.exceptionOrNull()?.message}")
                return@withContext apiResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving water intake: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    // Private helper methods for home API

    private suspend fun registerUserViaAPI(
        username: String, email: String, password: String, confirmPassword: String,
        firstName: String, lastName: String, phone: String, dateOfBirth: String, gender: String,
        address: String, city: String, state: String, zipCode: String,
        emergencyContactName: String, emergencyContactPhone: String,
        medicalConditions: String, allergies: String
    ): Result<Map<String, Any?>> {
        return try {
            val url = URL(baseUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val jsonData = JSONObject().apply {
                put("action", "register")
                put("username", username)
                put("email", email)
                put("password", password)
                put("confirm_password", confirmPassword)
                put("first_name", firstName)
                put("last_name", lastName)
                put("phone", phone)
                put("date_of_birth", dateOfBirth)
                put("gender", gender)
                put("address", address)
                put("city", city)
                put("state", state)
                put("zip_code", zipCode)
                put("emergency_contact_name", emergencyContactName)
                put("emergency_contact_phone", emergencyContactPhone)
                put("medical_conditions", medicalConditions)
                put("allergies", allergies)
            }
            
            val outputStream = connection.outputStream
            outputStream.write(jsonData.toString().toByteArray())
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to jsonResponse.optBoolean("success", false),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Registration failed"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loginUserViaAPI(username: String, password: String): Result<Map<String, Any?>> {
        return try {
            val url = URL(baseUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val jsonData = JSONObject().apply {
                put("action", "login")
                put("username", username)
                put("password", password)
            }
            
            Log.d(TAG, "Login request: ${jsonData.toString()}")
            
            val outputStream = connection.outputStream
            outputStream.write(jsonData.toString().toByteArray())
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            Log.d(TAG, "Login response code: $responseCode")
            Log.d(TAG, "Login response body: $response")
            
            val jsonResponse = JSONObject(response)
            Log.d(TAG, "Parsed JSON status: ${jsonResponse.optString("status")}")
            Log.d(TAG, "Parsed JSON message: ${jsonResponse.optString("message")}")
            Log.d(TAG, "Parsed JSON data: ${jsonResponse.optJSONObject("data")}")
            
            val dataObject = jsonResponse.optJSONObject("data")
            val dataMap = if (dataObject != null) {
                mapOf(
                    "user_id" to dataObject.optInt("user_id"),
                    "username" to dataObject.optString("username"),
                    "email" to dataObject.optString("email"),
                    "session_token" to dataObject.optString("session_token"),
                    "expires_at" to dataObject.optString("expires_at")
                )
            } else null
            
            Log.d(TAG, "Converted data map: $dataMap")
            
            val result = mapOf(
                "success" to (jsonResponse.optString("status") == "success"),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to dataMap
            )
            
            Log.d(TAG, "Final result success: ${result["success"]}")
            Log.d(TAG, "Final result data: ${result["data"]}")
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Login failed"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Login API error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun verifySessionViaAPI(sessionToken: String): Result<Map<String, Any?>> {
        return try {
            val url = URL("$baseUrl?action=verify&session_token=$sessionToken")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to (jsonResponse.optString("status") == "success"),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Session verification failed"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun logoutUserViaAPI(sessionToken: String): Result<Map<String, Any?>> {
        return try {
            val url = URL(baseUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val jsonData = JSONObject().apply {
                put("action", "logout")
                put("session_token", sessionToken)
            }
            
            val outputStream = connection.outputStream
            outputStream.write(jsonData.toString().toByteArray())
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to (jsonResponse.optString("status") == "success"),
                "message" to jsonResponse.optString("message", "Unknown error")
            )
            
            Result.success(result)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun storeUserLocally(result: Map<String, Any?>) {
        try {
            val userData = result["data"] as? Map<String, Any> ?: return
            val sessionToken = userData["session_token"] as? String ?: return
            val username = userData["username"] as? String ?: return
            
            // Get full user profile from API
            val profileResult = getUserProfileViaAPI(sessionToken)
            if (profileResult.isSuccess) {
                val profileData = profileResult.getOrThrow()
                val fullUserData = profileData["data"] as? Map<String, Any> ?: return
                
                // Create user with full profile data but we need to handle password_hash
                val user = User.fromMap(fullUserData).copy(
                    // We don't have the password hash from API, so we'll use a placeholder
                    // This is okay because when offline, we'll use the stored session token
                    passwordHash = "api_authenticated_user"
                )
                
                // Check if user already exists locally to avoid duplicates
                val existingUser = databaseHelper.getUserByUsername(user.username)
                if (existingUser == null) {
                    val userId = databaseHelper.insertUser(user)
                    Log.d(TAG, "User stored locally with ID: $userId")
                } else {
                    // Update existing user but keep the original password hash if it exists
                    val updatedUser = user.copy(
                        id = existingUser.id,
                        passwordHash = if (existingUser.passwordHash.isNotEmpty() && existingUser.passwordHash != "api_authenticated_user") {
                            existingUser.passwordHash
                        } else {
                            "api_authenticated_user"
                        }
                    )
                    databaseHelper.updateUser(updatedUser)
                    Log.d(TAG, "User updated locally: ${user.username}")
                }
            } else {
                Log.w(TAG, "Failed to get full user profile, storing basic info only")
                // Fallback: store basic user info from login response
                val basicUser = User(
                    id = (userData["user_id"] as? Number)?.toLong() ?: 0,
                    username = username,
                    email = userData["email"] as? String ?: "",
                    passwordHash = "api_authenticated_user",
                    firstName = "",
                    lastName = "",
                    phone = "",
                    dateOfBirth = "",
                    gender = ""
                )
                
                val existingUser = databaseHelper.getUserByUsername(username)
                if (existingUser == null) {
                    databaseHelper.insertUser(basicUser)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store user locally: ${e.message}")
        }
    }

    private fun storeMealLocally(
        mealType: String,
        mealOption: com.beatwell.app.models.MealOption,
        portionSize: Float,
        calories: Int
    ): Long {
        // Get the current user ID from session token
        val sessionToken = AuthManager(context).getCurrentSessionToken()
        if (sessionToken == null) {
            Log.e(TAG, "No session token available for saving meal")
            return -1
        }
        
        val sessionData = databaseHelper.getValidSession(sessionToken)
        if (sessionData == null) {
            Log.e(TAG, "Invalid session for saving meal")
            return -1
        }
        
        val (user, _) = sessionData
        if (user == null) {
            Log.e(TAG, "No user found in session for saving meal")
            return -1
        }
        
        return databaseHelper.insertMealLog(
            userId = user.id,
            mealType = mealType,
            mealOptionId = mealOption.id,
            mealOptionName = mealOption.name,
            mealOptionDescription = mealOption.description,
            portionSize = portionSize,
            calories = calories,
            isCustom = false
        )
    }
    
    private fun storeCustomMealLocally(
        userId: Long,
        mealType: String,
        foodName: String,
        notes: String,
        portionSize: Float,
        calories: Int
    ): Long {
        // Get the current user ID from session token (more reliable than parameter)
        val sessionToken = AuthManager(context).getCurrentSessionToken()
        if (sessionToken == null) {
            Log.e(TAG, "No session token available for saving custom meal")
            return -1
        }
        
        val sessionData = databaseHelper.getValidSession(sessionToken)
        if (sessionData == null) {
            Log.e(TAG, "Invalid session for saving custom meal")
            return -1
        }
        
        val (user, _) = sessionData
        if (user == null) {
            Log.e(TAG, "No user found in session for saving custom meal")
            return -1
        }
        
        return databaseHelper.insertMealLog(
            userId = user.id,
            mealType = mealType,
            mealOptionId = -1, // Custom food ID
            mealOptionName = foodName,
            mealOptionDescription = notes,
            portionSize = portionSize,
            calories = calories,
            isCustom = true
        )
    }


    private fun getExpirationDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 30) // 30 days from now
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(calendar.time)
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

    private suspend fun getDashboardDataViaAPI(sessionToken: String): Result<Map<String, Any?>> {
        return try {
            val url = URL("$homeApiUrl?action=dashboard_data&session_token=$sessionToken")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to (jsonResponse.optString("status") == "success"),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to get dashboard data"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun trackMealViaAPI(
        sessionToken: String,
        mealType: String,
        calories: Int,
        waterIntake: Int,
        notes: String
    ): Result<Map<String, Any?>> {
        return try {
            val url = URL(homeApiUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val jsonData = JSONObject().apply {
                put("action", "track_meal")
                put("session_token", sessionToken)
                put("meal_type", mealType)
                put("calories", calories)
                put("water_intake", waterIntake)
                put("notes", notes)
            }
            
            val outputStream = connection.outputStream
            outputStream.write(jsonData.toString().toByteArray())
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to (jsonResponse.optString("status") == "success"),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to track meal"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveMealDataViaAPI(
        mealType: String,
        mealOption: com.beatwell.app.models.MealOption,
        portionSize: Float,
        calories: Int
    ): Result<Map<String, Any?>> {
        return try {
            val url = URL("$mealsApiUrl/save")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val jsonData = JSONObject().apply {
                // Get the current user ID
                val authManager = AuthManager(context)
                val userId = authManager.getCurrentUserId()
                put("user_id", userId)
                put("meal_type", mealType)
                put("meal_option", JSONObject().apply {
                    put("id", mealOption.id)
                    put("name", mealOption.name)
                    put("description", mealOption.description)
                })
                put("portion_size", portionSize)
                put("calories", calories)
            }
            
            val outputStream = connection.outputStream
            outputStream.write(jsonData.toString().toByteArray())
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to jsonResponse.optBoolean("success", false),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to save meal"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getTodayMealsViaAPI(sessionToken: String): Result<Map<String, Any?>> {
        return try {
            val url = URL("$mealsApiUrl/today?user_id=1") // TODO: Get user_id from session
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to jsonResponse.optBoolean("success", false),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to get today's meals"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveCustomFoodViaAPI(
        mealType: String,
        foodName: String,
        notes: String,
        portionSize: Float,
        calories: Int
    ): Result<Map<String, Any?>> {
        return try {
            val authManager = AuthManager(context)
            val userId = authManager.getCurrentUserId()
            
            val url = URL("$mealsApiUrl/save")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val jsonData = JSONObject().apply {
                put("user_id", userId)
                put("meal_type", mealType)
                put("meal_option", JSONObject().apply {
                    put("id", -1) // Custom food ID
                    put("name", foodName)
                    put("description", notes)
                })
                put("portion_size", portionSize)
                put("calories", calories)
                put("is_custom", true)
            }
            
            val outputStream = connection.outputStream
            outputStream.write(jsonData.toString().toByteArray())
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to jsonResponse.optBoolean("success", false),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to save custom food"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getMealsForMonthViaAPI(userId: Long, year: Int, month: Int): Result<Map<String, Any?>> {
        return try {
            val url = URL("$calendarApiUrl?action=month_meals&user_id=$userId&year=$year&month=$month")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to jsonResponse.optBoolean("success", false),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONArray("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to get meals for month"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveWaterIntakeViaAPI(sessionToken: String, userId: Int, glasses: Int): Result<Map<String, Any?>> {
        return try {
            val url = URL(calendarApiUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val jsonData = JSONObject().apply {
                put("action", "save_water")
                put("session_token", sessionToken)
                put("user_id", userId)
                put("glasses", glasses)
            }
            
            val outputStream = connection.outputStream
            outputStream.write(jsonData.toString().toByteArray())
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to jsonResponse.optBoolean("success", false),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to save water intake"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get meals for a specific date via API
     */
    private suspend fun getMealsForDateViaAPI(userId: Long, date: String): Result<Map<String, Any?>> {
        return try {
            val url = URL("$calendarApiUrl?action=get_date_meals&user_id=$userId&date=$date")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val mealsList = jsonResponse.optJSONArray("meals")?.let { array ->
                (0 until array.length()).map { i ->
                    val meal = array.getJSONObject(i)
                    MealLog(
                        id = meal.optInt("id", 0),
                        mealType = meal.optString("meal_type", ""),
                        mealName = meal.optString("meal_option_name", ""),
                        calories = meal.optInt("calories", 0),
                        portionSize = meal.optDouble("portion_size", 1.0).toFloat(),
                        loggedAt = java.util.Date()
                    )
                }
            } ?: emptyList<MealLog>()
            
            val result = mapOf(
                "success" to jsonResponse.optBoolean("success", false),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "meals" to mealsList
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to get meals for date"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getUserProfileViaAPI(sessionToken: String): Result<Map<String, Any?>> {
        return try {
            val url = URL("$baseUrl?action=profile&session_token=$sessionToken")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to (jsonResponse.optString("status") == "success"),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to get profile"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateUserProfileViaAPI(sessionToken: String, profileData: Map<String, Any>): Result<Map<String, Any?>> {
        return try {
            val url = URL(baseUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val jsonData = JSONObject().apply {
                put("action", "update_profile")
                put("session_token", sessionToken)
                profileData.forEach { (key, value) ->
                    put(key, value)
                }
            }
            
            val outputStream = connection.outputStream
            outputStream.write(jsonData.toString().toByteArray())
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            val result = mapOf(
                "success" to (jsonResponse.optString("status") == "success"),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to jsonResponse.optJSONObject("data")
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to update profile"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun updateUserProfileLocally(profileData: Map<String, Any>) {
        try {
            // This would update the local database with the new profile data
            // Implementation depends on the specific database structure
            Log.d(TAG, "Profile updated locally")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile locally: ${e.message}")
        }
    }

    /**
     * Get all meals history via API
     */
    private suspend fun getAllMealsHistoryViaAPI(sessionToken: String): Result<Map<String, Any?>> {
        return try {
            val url = URL("$homeApiUrl?action=meal_history&session_token=$sessionToken&limit=50&offset=0")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }
            
            val jsonResponse = JSONObject(response)
            
            val mealsData = jsonResponse.optJSONObject("data")?.optJSONArray("meals")?.let { array ->
                (0 until array.length()).map { i ->
                    val meal = array.getJSONObject(i)
                    mapOf(
                        "id" to meal.optInt("id", 0),
                        "meal_type" to meal.optString("meal_type", ""),
                        "meal_name" to meal.optString("meal_name", ""),
                        "description" to meal.optString("description", ""),
                        "portion_size" to meal.optDouble("portion_size", 1.0),
                        "calories" to meal.optInt("calories", 0),
                        "is_custom" to meal.optBoolean("is_custom", false),
                        "logged_at" to meal.optString("logged_at", ""),
                        "date" to meal.optString("date", ""),
                        "time" to meal.optString("time", "")
                    )
                }
            } ?: emptyList<Map<String, Any>>()
            
            val result = mapOf(
                "success" to (jsonResponse.optString("status") == "success"),
                "message" to jsonResponse.optString("message", "Unknown error"),
                "data" to mealsData
            )
            
            if (result["success"] == true) {
                Result.success(result)
            } else {
                Result.failure(Exception(result["message"]?.toString() ?: "Failed to get meal history"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
