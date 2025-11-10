package com.beatwell.app.chat

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.beatwell.app.network.NetworkManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("chat_messages", Context.MODE_PRIVATE)
    private val networkManager = NetworkManager(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        private const val TAG = "ChatManager"
        private const val MESSAGES_KEY = "chat_messages"
        private const val BASE_URL = "http://10.0.2.2/beatwell/backend/api/chat.php"
    }
    
    data class ChatMessage(
        val id: String,
        val message: String,
        val senderType: String, // "user" or "ai"
        val timestamp: Long,
        val isLocal: Boolean = false
    )
    
    /**
     * Save message to local storage
     */
    fun saveMessageLocally(message: ChatMessage) {
        try {
            val messages = getLocalMessages().toMutableList()
            messages.add(message)
            saveMessagesToLocal(messages)
            Log.d(TAG, "Message saved locally: ${message.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save message locally", e)
        }
    }
    
    /**
     * Get all messages from local storage
     */
    fun getLocalMessages(): List<ChatMessage> {
        return try {
            val messagesJson = prefs.getString(MESSAGES_KEY, "[]") ?: "[]"
            val jsonArray = JSONArray(messagesJson)
            val messages = mutableListOf<ChatMessage>()
            
            for (i in 0 until jsonArray.length()) {
                val messageObj = jsonArray.getJSONObject(i)
                messages.add(
                    ChatMessage(
                        id = messageObj.getString("id"),
                        message = messageObj.getString("message"),
                        senderType = messageObj.getString("senderType"),
                        timestamp = messageObj.getLong("timestamp"),
                        isLocal = messageObj.optBoolean("isLocal", false)
                    )
                )
            }
            messages.sortedBy { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local messages", e)
            emptyList()
        }
    }
    
    /**
     * Sync messages with server
     */
    suspend fun syncMessagesWithServer(): Boolean {
        return try {
            if (!networkManager.isNetworkAvailable()) {
                Log.w(TAG, "No network available for sync")
                return false
            }
            
            // Get local messages that haven't been synced
            val localMessages = getLocalMessages().filter { it.isLocal }
            if (localMessages.isEmpty()) {
                Log.d(TAG, "No local messages to sync")
                return true
            }
            
            // Send each local message to server
            for (message in localMessages) {
                sendMessageToServer(message)
            }
            
            // Fetch latest messages from server
            fetchMessagesFromServer()
            
            Log.d(TAG, "Messages synced with server")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync messages with server", e)
            false
        }
    }
    
    /**
     * Send message to server
     */
    private suspend fun sendMessageToServer(message: ChatMessage) {
        try {
            // For now, we'll just log that we would send to server
            // In a real implementation, you would use HttpURLConnection or Retrofit
            Log.d(TAG, "Would send message to server: ${message.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message to server", e)
        }
    }
    
    /**
     * Fetch messages from server
     */
    private suspend fun fetchMessagesFromServer() {
        try {
            // For now, we'll just log that we would fetch from server
            // In a real implementation, you would use HttpURLConnection or Retrofit
            Log.d(TAG, "Would fetch messages from server")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch messages from server", e)
        }
    }
    
    /**
     * Send a new message (user or AI)
     */
    suspend fun sendMessage(message: String, senderType: String): ChatMessage {
        val chatMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            message = message,
            senderType = senderType,
            timestamp = System.currentTimeMillis(),
            isLocal = true
        )
        
        // Save locally first
        saveMessageLocally(chatMessage)
        
        // Try to sync with server
        if (networkManager.isNetworkAvailable()) {
            try {
                sendMessageToServer(chatMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message to server, will retry later", e)
            }
        }
        
        return chatMessage
    }
    
    /**
     * Clear all messages
     */
    fun clearAllMessages() {
        try {
            prefs.edit().remove(MESSAGES_KEY).apply()
            Log.d(TAG, "All messages cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear messages", e)
        }
    }
    
    /**
     * Save messages to local storage
     */
    private fun saveMessagesToLocal(messages: List<ChatMessage>) {
        try {
            val jsonArray = JSONArray()
            for (message in messages) {
                val messageObj = JSONObject().apply {
                    put("id", message.id)
                    put("message", message.message)
                    put("senderType", message.senderType)
                    put("timestamp", message.timestamp)
                    put("isLocal", message.isLocal)
                }
                jsonArray.put(messageObj)
            }
            
            prefs.edit().putString(MESSAGES_KEY, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save messages to local storage", e)
        }
    }
    
    /**
     * Parse server timestamp
     */
    private fun parseServerTimestamp(timestamp: String): Long {
        return try {
            dateFormat.parse(timestamp)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
