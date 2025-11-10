package com.beatwell.app.auth

import android.content.Context
import android.content.SharedPreferences
import com.beatwell.app.database.DatabaseHelper
import com.beatwell.app.models.User
import com.beatwell.app.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthManager(private val context: Context) {
    
    private val networkManager = NetworkManager(context)
    private val databaseHelper = DatabaseHelper(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("beatwell_auth", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    /**
     * Register a new user
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
        address: String = "",
        city: String = "",
        state: String = "",
        zipCode: String = "",
        emergencyContactName: String = "",
        emergencyContactPhone: String = "",
        medicalConditions: String = "",
        allergies: String = ""
    ): AuthResult = withContext(Dispatchers.IO) {
        
        try {
            val result = networkManager.registerUser(
                username, email, password, confirmPassword,
                firstName, lastName, phone, dateOfBirth, gender,
                address, city, state, zipCode,
                emergencyContactName, emergencyContactPhone,
                medicalConditions, allergies
            )
            
            if (result.isSuccess) {
                val data = result.getOrThrow()
                val userData = data["data"] as? Map<String, Any>
                val sessionToken = userData?.get("session_token") as? String
                
                if (sessionToken != null) {
                    // Store session info
                    saveSessionInfo(
                        sessionToken = sessionToken,
                        userId = (userData["user_id"] as? Number)?.toLong() ?: 0,
                        username = userData["username"] as? String ?: username,
                        email = userData["email"] as? String ?: email
                    )
                    
                    AuthResult.Success(
                        message = data["message"] as? String ?: "Registration successful",
                        user = User.fromMap(userData)
                    )
                } else {
                    AuthResult.Error("Invalid response from server")
                }
            } else {
                AuthResult.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        } catch (e: Exception) {
            AuthResult.Error("Registration failed: ${e.message}")
        }
    }

    /**
     * Login user
     */
    suspend fun loginUser(username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        
        try {
            val result = networkManager.loginUser(username, password)
            
            android.util.Log.d("AuthManager", "Login result success: ${result.isSuccess}")
            
            if (result.isSuccess) {
                val data = result.getOrThrow()
                android.util.Log.d("AuthManager", "Login data: $data")
                
                val userData = data["data"] as? Map<String, Any>
                android.util.Log.d("AuthManager", "User data: $userData")
                
                val sessionToken = userData?.get("session_token") as? String
                android.util.Log.d("AuthManager", "Session token: $sessionToken")
                
                if (sessionToken != null) {
                    // Store session info
                    saveSessionInfo(
                        sessionToken = sessionToken,
                        userId = (userData["user_id"] as? Number)?.toLong() ?: 0,
                        username = userData["username"] as? String ?: username,
                        email = userData["email"] as? String ?: "",
                        firstName = userData["first_name"] as? String ?: "",
                        lastName = userData["last_name"] as? String ?: ""
                    )
                    
                    AuthResult.Success(
                        message = data["message"] as? String ?: "Login successful",
                        user = User.fromMap(userData)
                    )
                } else {
                    android.util.Log.e("AuthManager", "Session token is null - Invalid response from server")
                    android.util.Log.e("AuthManager", "Data structure: $data")
                    android.util.Log.e("AuthManager", "UserData structure: $userData")
                    AuthResult.Error("Invalid response from server")
                }
            } else {
                android.util.Log.e("AuthManager", "Login result failed: ${result.exceptionOrNull()?.message}")
                AuthResult.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Login exception: ${e.message}", e)
            AuthResult.Error("Login failed: ${e.message}")
        }
    }

    /**
     * Logout user
     */
    suspend fun logoutUser(): AuthResult = withContext(Dispatchers.IO) {
        
        try {
            val sessionToken = getCurrentSessionToken()
            if (sessionToken != null) {
                val result = networkManager.logoutUser(sessionToken)
                // Clear session regardless of API result
                clearSession()
                
                if (result.isSuccess) {
                    AuthResult.Success("Logout successful")
                } else {
                    AuthResult.Success("Logout successful (offline)")
                }
            } else {
                clearSession()
                AuthResult.Success("Logout successful")
            }
        } catch (e: Exception) {
            clearSession()
            AuthResult.Success("Logout successful")
        }
    }

    /**
     * Logout user (synchronous)
     */
    fun logout() {
        val sessionToken = getCurrentSessionToken()
        val userId = getCurrentUserId()
        
        // Clear all sessions for this user from database
        if (userId > 0) {
            databaseHelper.deleteAllSessionsForUser(userId)
        }
        
        // Clear specific session if token exists
        if (sessionToken != null) {
            databaseHelper.deleteSession(sessionToken)
        }
        
        // Clear ALL session data from SharedPreferences
        prefs.edit().clear().apply()
        
        // Log the logout action
        android.util.Log.d("AuthManager", "User logged out - all session data cleared")
    }

    /**
     * Check if user is currently logged in
     */
    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        
        val sessionToken = getCurrentSessionToken()
        if (sessionToken == null) return@withContext false
        
        try {
            val result = networkManager.verifySession(sessionToken)
            if (result.isSuccess) {
                val data = result.getOrThrow()
                val userData = data["data"] as? Map<String, Any>
                
                if (userData != null) {
                    // Update stored user info
                    saveSessionInfo(
                        sessionToken = sessionToken,
                        userId = (userData["user_id"] as? Number)?.toLong() ?: 0,
                        username = userData["username"] as? String ?: "",
                        email = userData["email"] as? String ?: ""
                    )
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            // Session verification failed, clear session
            clearSession()
        }
        
        false
    }

    /**
     * Get current user
     */
    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        
        val sessionToken = getCurrentSessionToken()
        if (sessionToken == null) {
            android.util.Log.d("AuthManager", "No session token found")
            return@withContext null
        }
        
        // First try to create user from stored session info (faster)
        val userId = getCurrentUserId()
        val username = getCurrentUsername()
        val email = getCurrentEmail()
        val firstName = getCurrentFirstName()
        val lastName = getCurrentLastName()
        
        if (userId > 0 && !username.isNullOrEmpty() && !email.isNullOrEmpty()) {
            android.util.Log.d("AuthManager", "Creating user from stored session info")
            android.util.Log.d("AuthManager", "Stored user data - ID: $userId, Username: $username, FirstName: $firstName")
            // Create basic user object from stored session data
            val user = User(
                id = userId,
                username = username,
                email = email,
                passwordHash = "", // Not needed for display
                firstName = firstName ?: username, // Use stored first name or fallback to username
                lastName = lastName ?: "",
                phone = "",
                dateOfBirth = "",
                gender = ""
            )
            return@withContext user
        }
        
        // If no stored session info, try API verification
        try {
            android.util.Log.d("AuthManager", "Verifying session with API")
            val result = networkManager.verifySession(sessionToken)
            if (result.isSuccess) {
                val data = result.getOrThrow()
                val userData = data["data"] as? Map<String, Any>
                if (userData != null) {
                    android.util.Log.d("AuthManager", "Session verified, creating user from API data")
                    return@withContext User.fromMap(userData)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Session verification failed: ${e.message}")
        }
        
        android.util.Log.e("AuthManager", "Failed to get current user")
        null
    }

    /**
     * Get current session token
     */
    fun getCurrentSessionToken(): String? {
        return prefs.getString(KEY_SESSION_TOKEN, null)
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): Long {
        return prefs.getLong(KEY_USER_ID, 0)
    }

    /**
     * Get current username
     */
    fun getCurrentUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    /**
     * Get current email
     */
    fun getCurrentEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    /**
     * Check if user is logged in (synchronous check)
     */
    fun isLoggedInSync(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getCurrentSessionToken() != null
    }

    /**
     * Clear all session data
     */
    private fun clearSession() {
        val sessionToken = getCurrentSessionToken()
        if (sessionToken != null) {
            databaseHelper.deleteSession(sessionToken)
        }
        
        prefs.edit().apply {
            remove(KEY_SESSION_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    /**
     * Save session information
     */
    private fun saveSessionInfo(sessionToken: String, userId: Long, username: String, email: String, firstName: String = "", lastName: String = "") {
        prefs.edit().apply {
            putString(KEY_SESSION_TOKEN, sessionToken)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    /**
     * Get current first name
     */
    fun getCurrentFirstName(): String? {
        return prefs.getString(KEY_FIRST_NAME, null)
    }
    
    /**
     * Get current last name
     */
    fun getCurrentLastName(): String? {
        return prefs.getString(KEY_LAST_NAME, null)
    }

    // Removed connectivity status methods for unified approach
}

/**
 * Authentication result sealed class
 */
sealed class AuthResult {
    data class Success(
        val message: String,
        val user: User? = null
    ) : AuthResult()
    
    data class Error(
        val message: String
    ) : AuthResult()
}
