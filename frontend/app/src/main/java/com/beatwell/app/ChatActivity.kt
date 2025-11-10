package com.beatwell.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beatwell.app.databinding.ActivityChatBinding
import com.beatwell.app.utils.BottomNavigationHelper
import com.beatwell.app.chat.ChatManager
import com.beatwell.app.network.GeminiApiService
import kotlinx.coroutines.*

class ChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChatBinding
    private lateinit var bottomNavigationHelper: BottomNavigationHelper
    private lateinit var chatManager: ChatManager
    private lateinit var geminiApiService: GeminiApiService
    private var isGeneratingResponse = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Initialize bottom navigation
        bottomNavigationHelper = BottomNavigationHelper(this)
        bottomNavigationHelper.setupBottomNavigation()
        
        // Initialize chat manager and Gemini API service
        chatManager = ChatManager(this)
        geminiApiService = GeminiApiService(this)
        
        // Set up click listeners
        setupClickListeners()
        
        // Initialize chat
        initializeChat()
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // Send button
        binding.ivSendButton.setOnClickListener {
            if (!isGeneratingResponse) {
                sendMessage()
            }
        }
        
        // Text input listener
        binding.etMessageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Enable/disable send button based on text
                binding.ivSendButton.isEnabled = !s.isNullOrBlank()
                binding.ivSendButton.alpha = if (s.isNullOrBlank()) 0.5f else 1.0f
            }
        })
        
        // Bottom navigation is handled by BottomNavigationHelper
    }
    
    private fun initializeChat() {
        // Load existing messages from storage
        loadExistingMessages()
        
        // Add initial AI message if chat is empty
        if (binding.llChatContainer.childCount == 0) {
            addAIMessageToUI("Hi! I'm your personal nutrition assistant. I can help you with meal planning, calorie tracking, healthy recipes, and dietary advice. What would you like to know about nutrition today?")
        }
    }
    
    private fun loadExistingMessages() {
        try {
            val messages = chatManager.getLocalMessages()
            for (message in messages) {
                if (message.senderType == "user") {
                    addUserMessageToUI(message.message)
                } else {
                    addAIMessageToUI(message.message)
                }
            }
        } catch (e: Exception) {
            // Handle error silently, start with empty chat
        }
    }
    
    private fun sendMessage() {
        val message = binding.etMessageInput.text.toString().trim()
        if (message.isNotEmpty() && !isGeneratingResponse) {
            // Check if question is nutrition-related
            if (!geminiApiService.isNutritionRelatedQuestion(message)) {
                showScopeError()
                return
            }
            
            // Add user message to UI
            addUserMessageToUI(message)
            
            // Save user message to storage
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    chatManager.sendMessage(message, "user")
                } catch (e: Exception) {
                    // Handle error
                }
            }
            
            // Clear input
            binding.etMessageInput.setText("")
            
            // Generate and add AI response
            generateAndAddAIResponse(message)
        }
    }
    
    private fun addUserMessageToUI(message: String) {
        val messageLayout = createUserMessageLayout(message)
        binding.llChatContainer.addView(messageLayout)
        addEntranceAnimation(messageLayout)
        scrollToBottom()
    }
    
    private fun addAIMessageToUI(message: String) {
        val messageLayout = createAIMessageLayout(message)
        binding.llChatContainer.addView(messageLayout)
        scrollToBottom()
    }
    
    private fun generateAndAddAIResponse(userMessage: String) {
        if (isGeneratingResponse) return
        
        isGeneratingResponse = true
        binding.ivSendButton.isEnabled = false
        binding.ivSendButton.alpha = 0.5f
        
        // Show thinking animation
        val thinkingLayout = showThinkingAnimation()
        
        // Generate AI response using Gemini API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = geminiApiService.generateResponse(userMessage)
                
                withContext(Dispatchers.Main) {
                    // Remove thinking animation
                    binding.llChatContainer.removeView(thinkingLayout)
                    
                    // Always show response (unified approach handles connectivity)
                    val aiResponse = result.getOrThrow()
                    addAIMessageToUI(aiResponse)
                    
                    // Save AI response to storage
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            chatManager.sendMessage(aiResponse, "ai")
                        } catch (e: Exception) {
                            // Handle error silently
                        }
                    }
                    
                    // Re-enable send button
                    isGeneratingResponse = false
                    binding.ivSendButton.isEnabled = true
                    binding.ivSendButton.alpha = 1.0f
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Remove thinking animation
                    binding.llChatContainer.removeView(thinkingLayout)
                    
                    // Show fallback response (unified approach)
                    val fallbackResponse = geminiApiService.getFallbackResponse()
                    addAIMessageToUI(fallbackResponse)
                    
                    // Re-enable send button
                    isGeneratingResponse = false
                    binding.ivSendButton.isEnabled = true
                    binding.ivSendButton.alpha = 1.0f
                }
            }
        }
    }
    
    private fun createUserMessageLayout(message: String): LinearLayout {
        val layout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }
        
        // Message bubble with proper width constraints
        val bubbleLayout = LinearLayout(this).apply {
            val screenWidth = resources.displayMetrics.widthPixels
            val maxWidth = (screenWidth * 0.75).toInt() // Max 75% of screen width
            layoutParams = LinearLayout.LayoutParams(
                maxWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.chat_bubble_user)
            setPadding(48, 32, 48, 32)
        }
        
        val messageText = TextView(this).apply {
            text = message
            setTextColor(resources.getColor(R.color.white, null))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        bubbleLayout.addView(messageText)
        layout.addView(bubbleLayout)
        
        // User avatar
        val avatar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(64, 64).apply {
                setMargins(24, 0, 0, 0)
            }
            setBackgroundResource(R.drawable.circle_background_user)
        }
        
        layout.addView(avatar)
        return layout
    }
    
    private fun createAIMessageLayout(message: String): LinearLayout {
        val layout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.START
        }
        
        // AI avatar
        val avatar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(64, 64).apply {
                setMargins(0, 0, 24, 0)
            }
            setBackgroundResource(R.drawable.circle_background_ai)
        }
        
        layout.addView(avatar)
        
        // Message bubble with proper width constraints
        val bubbleLayout = LinearLayout(this).apply {
            val screenWidth = resources.displayMetrics.widthPixels
            val maxWidth = (screenWidth * 0.75).toInt() // Max 75% of screen width
            layoutParams = LinearLayout.LayoutParams(
                maxWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.chat_bubble_ai)
            setPadding(48, 32, 48, 32)
        }
        
        // Parse and format the message with bullet points and formatting
        val formattedMessage = formatMessage(message)
        val messageText = TextView(this).apply {
            text = formattedMessage
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        bubbleLayout.addView(messageText)
        layout.addView(bubbleLayout)
        
        // Add entrance animation
        addEntranceAnimation(layout)
        
        return layout
    }
    
    
    /**
     * Show thinking animation while AI is generating response
     */
    private fun showThinkingAnimation(): LinearLayout {
        val thinkingLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.START
        }
        
        // AI avatar
        val avatar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(64, 64).apply {
                setMargins(0, 0, 24, 0)
            }
            setBackgroundResource(R.drawable.circle_background_ai)
        }
        
        thinkingLayout.addView(avatar)
        
        // Thinking bubble
        val bubbleLayout = LinearLayout(this).apply {
            val screenWidth = resources.displayMetrics.widthPixels
            val maxWidth = (screenWidth * 0.75).toInt()
            layoutParams = LinearLayout.LayoutParams(
                maxWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.chat_bubble_ai)
            setPadding(48, 32, 48, 32)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val thinkingText = TextView(this).apply {
            text = "Thinking..."
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        // Add typing dots animation
        val typingDots = TextView(this).apply {
            text = "..."
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        bubbleLayout.addView(thinkingText)
        bubbleLayout.addView(typingDots)
        
        thinkingLayout.addView(bubbleLayout)
        
        // Add to chat container
        binding.llChatContainer.addView(thinkingLayout)
        scrollToBottom()
        
        // Add pulsing animation
        addPulsingAnimation(thinkingText)
        addTypingDotsAnimation(typingDots)
        
        return thinkingLayout
    }
    
    /**
     * Show scope error when question is not nutrition-related
     */
    private fun showScopeError() {
        val errorMessage = "I'm a nutrition specialist. Please ask me about nutrition, diet, meal planning, or health-related questions instead."
        addAIMessageToUI(errorMessage)
    }
    
    /**
     * Format message with proper bullet points and structure
     */
    private fun formatMessage(message: String): String {
        return message
            .replace("•", "•") // Ensure proper bullet points
            .replace("- ", "• ") // Convert dashes to bullet points
            .replace("\n\n", "\n") // Clean up extra line breaks
            .replace("**", "") // Remove markdown bold formatting
            .trim()
    }
    
    /**
     * Add entrance animation to message
     */
    private fun addEntranceAnimation(view: View) {
        view.alpha = 0f
        view.translationY = 50f
        
        val animatorSet = AnimatorSet()
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val slideUp = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
        
        animatorSet.playTogether(fadeIn, slideUp)
        animatorSet.duration = 300
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }
    
    /**
     * Add pulsing animation to thinking text
     */
    private fun addPulsingAnimation(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.7f, 1f, 0.7f)
        
        scaleX.duration = 1000
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.duration = 1000
        scaleY.repeatCount = ObjectAnimator.INFINITE
        alpha.duration = 1000
        alpha.repeatCount = ObjectAnimator.INFINITE
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.start()
    }
    
    /**
     * Add typing dots animation
     */
    private fun addTypingDotsAnimation(view: TextView) {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1f, 0.3f)
        
        alpha.duration = 800
        alpha.repeatCount = ObjectAnimator.INFINITE
        alpha.start()
    }
    
    private fun scrollToBottom() {
        binding.svChatMessages.post {
            binding.svChatMessages.fullScroll(View.FOCUS_DOWN)
        }
    }
    
}