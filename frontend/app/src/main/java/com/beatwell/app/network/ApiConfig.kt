package com.beatwell.app.network

/**
 * Centralized API configuration for BeatWell app
 * Handles development vs production URL configuration
 */
object ApiConfig {
    
    // Development URLs - Auto-detect emulator vs device
    private const val EMULATOR_BASE_URL = "http://10.0.2.2/BeatWell/backend/api"
    
    // TODO: Update this IP address to match your XAMPP server
    // To find your IP: Open Command Prompt -> type "ipconfig" -> look for IPv4 Address
    private const val DEVICE_BASE_URL = "http://192.168.29.236/BeatWell/backend/api"
    
    // Use emulator URL for emulator, device URL for physical device
    private val DEV_BASE_URL = if (android.os.Build.FINGERPRINT.contains("generic")) 
        EMULATOR_BASE_URL else DEVICE_BASE_URL
    
    // Production URLs (to be configured when backend is deployed)
    private const val PROD_BASE_URL = "https://your-domain.com/api"
    
    // Current environment (change to false for production)
    private const val IS_DEVELOPMENT = true
    
    // Base URL selection
    val BASE_URL = if (IS_DEVELOPMENT) DEV_BASE_URL else PROD_BASE_URL
    
    // Timeout settings
    const val TIMEOUT_MS = 30000
    
    // API Endpoints
    val USERS_API = "$BASE_URL/users.php"
    val MEALS_API = "$BASE_URL/meals.php"
    val CALENDAR_API = "$BASE_URL/calendar.php"
    val CHAT_API = "$BASE_URL/chat.php"
    val HOME_API = "$BASE_URL/home.php"
}
