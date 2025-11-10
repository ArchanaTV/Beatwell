package com.beatwell.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.beatwell.app.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class GeminiApiService(private val context: Context) {
    
    companion object {
        private const val TAG = "GeminiApiService"
        // Backend Chat API endpoint
        private val CHAT_POST_URL = ApiConfig.CHAT_API
        private val CHAT_GET_URL = ApiConfig.CHAT_API
        private const val TIMEOUT_MS = 30000 // 30 seconds
        
        // Dietitian-focused system prompt
        private const val SYSTEM_PROMPT = """
You are a professional dietitian and nutrition expert. Your role is to provide helpful, accurate, and personalized nutrition advice to users of the BeatWell nutrition app.

IMPORTANT GUIDELINES:
1. ONLY answer questions related to nutrition, diet, food, health, meal planning, calorie counting, weight management, and dietary concerns
2. If asked about topics outside nutrition/diet, respond with: "I'm a nutrition specialist. Please ask me about nutrition, diet, meal planning, or health-related questions instead."
3. Always format your responses with clear bullet points and proper structure
4. Be encouraging, supportive, and professional
5. Provide practical, actionable advice
6. When discussing calories, always mention the importance of balanced nutrition
7. For meal suggestions, include nutritional benefits
8. Keep responses concise but informative
9. Use emojis sparingly and appropriately
10. Always end with a follow-up question to encourage engagement

RESPONSE FORMAT:
- Use bullet points (•) for lists
- Use numbered lists (1., 2., 3.) for steps
- Use **bold** for important points
- Keep paragraphs short and readable
- Always include practical tips

Remember: You are helping people make better nutrition choices, so be encouraging and focus on sustainable, healthy habits.
"""
    }
    
    /**
     * Generate AI response by calling the backend Chat API (which uses Gemini).
     * Falls back gracefully when offline.
     */
    suspend fun generateResponse(userMessage: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check connectivity first
            if (!isNetworkAvailable()) {
                Log.w(TAG, "No network connectivity available")
                return@withContext Result.success(getOfflineResponse(userMessage))
            }
            // Build POST to backend chat API
            val authManager = AuthManager(context)
            val sessionToken = authManager.getCurrentSessionToken()

            val postUrl = URL(CHAT_POST_URL)
            val postConn = postUrl.openConnection() as HttpURLConnection
            postConn.requestMethod = "POST"
            postConn.setRequestProperty("Content-Type", "application/json")
            postConn.doOutput = true
            postConn.connectTimeout = TIMEOUT_MS
            postConn.readTimeout = TIMEOUT_MS

            // Request body expected by backend: message, sender_type, session_token
            val postBody = JSONObject().apply {
                put("message", userMessage)
                put("sender_type", "user")
                if (!sessionToken.isNullOrEmpty()) put("session_token", sessionToken)
            }.toString()

            postConn.outputStream.use { os ->
                os.write(postBody.toByteArray())
                os.flush()
            }

            val postCode = postConn.responseCode
            if (postCode != HttpURLConnection.HTTP_OK) {
                val errorResponse = postConn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "Chat POST error: $postCode - $errorResponse")
                return@withContext Result.success(getOfflineResponse(userMessage))
            }

            // After POST succeeds, fetch latest AI message
            val latest = fetchLatestAIMessage(sessionToken)
            if (!latest.isNullOrBlank()) {
                return@withContext Result.success(latest)
            }

            // Fallback if nothing fetched
            return@withContext Result.success(getOfflineResponse(userMessage))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI response, using offline response", e)
            return@withContext Result.success(getOfflineResponse(userMessage))
        }
    }
    
    /**
     * Fetch the latest AI message from backend for current user/session
     */
    private fun fetchLatestAIMessage(sessionToken: String?): String? {
        return try {
            val getUrl = if (!sessionToken.isNullOrEmpty())
                URL("$CHAT_GET_URL?action=messages&session_token=$sessionToken")
            else URL("$CHAT_GET_URL?action=messages")

            val conn = getUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS

            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) return null

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.optString("status") != "success") return null

            val arr = json.optJSONArray("data") ?: return null
            var latest: String? = null
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.optString("sender_type") == "ai") {
                    latest = obj.optString("message")
                }
            }
            latest
        } catch (e: Exception) {
            Log.e(TAG, "fetchLatestAIMessage error", e)
            null
        }
    }
    
    /**
     * Check if the user's question is nutrition-related
     */
    fun isNutritionRelatedQuestion(question: String): Boolean {
        val nutritionKeywords = listOf(
            "nutrition", "diet", "food", "meal", "calorie", "calories", "weight", "health",
            "protein", "carb", "fat", "vitamin", "mineral", "fiber", "sugar", "sodium",
            "breakfast", "lunch", "dinner", "snack", "recipe", "cooking", "eating",
            "healthy", "unhealthy", "lose weight", "gain weight", "muscle", "fitness",
            "diabetes", "cholesterol", "blood pressure", "allergy", "intolerance",
            "vegetarian", "vegan", "keto", "paleo", "mediterranean", "dietary",
            "supplement", "vitamin", "mineral", "water", "hydration", "exercise",
            "eat", "drink", "hungry", "thirsty", "appetite", "craving", "taste",
            "flavor", "ingredient", "cook", "bake", "grill", "steam", "boil",
            "nutritional", "nutrient", "macro", "micro", "balance", "portion",
            "serving", "plate", "bowl", "cup", "gram", "ounce", "pound", "kg"
        )
        
        val lowerQuestion = question.lowercase()
        return nutritionKeywords.any { keyword -> lowerQuestion.contains(keyword) }
    }
    
    /**
     * Check if device has internet connectivity
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
    
    /**
     * Get offline response based on user question
     */
    private fun getOfflineResponse(userMessage: String): String {
        val lowerMessage = userMessage.lowercase()
        
        return when {
            lowerMessage.contains("breakfast") -> {
                "Here are some healthy breakfast ideas:\n\n• Oatmeal with berries and nuts\n• Greek yogurt with granola\n• Whole grain toast with avocado\n• Scrambled eggs with vegetables\n\nThese options provide sustained energy and essential nutrients to start your day right!"
            }
            lowerMessage.contains("lunch") -> {
                "Great lunch options include:\n\n• Grilled chicken salad with mixed greens\n• Quinoa bowl with vegetables and lean protein\n• Whole grain wrap with turkey and vegetables\n• Lentil soup with whole grain bread\n\nFocus on balanced portions with protein, vegetables, and whole grains!"
            }
            lowerMessage.contains("dinner") -> {
                "Healthy dinner suggestions:\n\n• Baked salmon with roasted vegetables\n• Stir-fried tofu with brown rice\n• Grilled chicken with sweet potato\n• Vegetable curry with quinoa\n\nInclude lean protein, colorful vegetables, and whole grains for a complete meal!"
            }
            lowerMessage.contains("calorie") || lowerMessage.contains("calories") -> {
                "Calorie needs vary by individual, but here's a general guide:\n\n• Women: 1,800-2,200 calories/day\n• Men: 2,200-2,800 calories/day\n• Focus on nutrient-dense foods\n• Include lean proteins, whole grains, and vegetables\n• Stay hydrated throughout the day"
            }
            lowerMessage.contains("weight") || lowerMessage.contains("lose") -> {
                "Sustainable weight management tips:\n\n• Create a moderate calorie deficit\n• Focus on whole, unprocessed foods\n• Include regular physical activity\n• Get adequate sleep (7-9 hours)\n• Stay hydrated and manage stress\n• Build healthy habits gradually"
            }
            lowerMessage.contains("healthy") || lowerMessage.contains("diet") -> {
                "A balanced diet includes:\n\n• Fill half your plate with vegetables\n• Choose whole grains over refined\n• Include lean proteins (chicken, fish, beans)\n• Add healthy fats (avocado, nuts, olive oil)\n• Stay hydrated with water\n• Limit processed foods and added sugars"
            }
            lowerMessage.contains("water") || lowerMessage.contains("hydration") -> {
                "Hydration is essential for health:\n\n• Aim for 8-10 glasses of water daily\n• Drink water throughout the day\n• Include water-rich foods (fruits, vegetables)\n• Monitor urine color (pale yellow is ideal)\n• Increase intake during exercise or hot weather"
            }
            else -> {
                "I'm here to help with your nutrition questions! I can assist with:\n\n• Meal planning and healthy recipes\n• Calorie and nutrient information\n• Weight management strategies\n• Dietary concerns and goals\n• Hydration and lifestyle tips\n\nWhat specific nutrition topic would you like to discuss?"
            }
        }
    }
    
    /**
     * Get a fallback response for when API fails
     */
    fun getFallbackResponse(): String {
        return "I'm here to help with your nutrition questions! I can assist with meal planning, healthy recipes, calorie information, and dietary advice. What would you like to know about nutrition?"
    }
}
