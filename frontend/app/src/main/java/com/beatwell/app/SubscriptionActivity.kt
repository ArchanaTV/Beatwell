package com.beatwell.app

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import com.beatwell.app.databinding.ActivitySubscriptionBinding
import java.io.IOException

class SubscriptionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var prefs: SharedPreferences
    private var selectedSubscription: String? = null
    
    companion object {
        const val SUBSCRIPTION_FREE_TRIAL = "free_trial"
        const val SUBSCRIPTION_MONTHLY = "monthly"
        const val SUBSCRIPTION_YEARLY = "yearly"
        const val KEY_SELECTED_SUBSCRIPTION = "selected_subscription"
        const val PREFS_NAME = "beatwell_subscription"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Load logo from assets
        loadLogoFromAssets()
        
        // Set up subscription cards
        setupSubscriptionCards()
        
        // Set up continue button
        setupContinueButton()
    }
    
    private fun loadLogoFromAssets() {
        try {
            val inputStream = assets.open("images/logo.png")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.ivLogo.setImageBitmap(bitmap)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            // Fallback to default icon if logo loading fails
            binding.ivLogo.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
    
    private fun setupSubscriptionCards() {
        // Free Trial Card
        binding.cardFreeTrial.setOnClickListener {
            selectSubscription(SUBSCRIPTION_FREE_TRIAL)
        }
        
        // Monthly Subscription Card
        binding.cardMonthly.setOnClickListener {
            selectSubscription(SUBSCRIPTION_MONTHLY)
        }
        
        // Yearly Subscription Card
        binding.cardYearly.setOnClickListener {
            selectSubscription(SUBSCRIPTION_YEARLY)
        }
        
        // Set default selection to Free Trial
        selectSubscription(SUBSCRIPTION_FREE_TRIAL)
    }
    
    private fun selectSubscription(subscriptionType: String) {
        selectedSubscription = subscriptionType
        
        // Convert 4dp to pixels
        val strokeWidthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            resources.displayMetrics
        ).toInt()
        
        // Reset all cards
        binding.cardFreeTrial.alpha = 0.7f
        binding.cardMonthly.alpha = 0.7f
        binding.cardYearly.alpha = 0.7f
        
        binding.cardFreeTrial.strokeWidth = 0
        binding.cardMonthly.strokeWidth = 0
        binding.cardYearly.strokeWidth = 0
        
        // Highlight selected card
        when (subscriptionType) {
            SUBSCRIPTION_FREE_TRIAL -> {
                binding.cardFreeTrial.alpha = 1.0f
                binding.cardFreeTrial.strokeWidth = strokeWidthPx
                binding.cardFreeTrial.strokeColor = getColor(R.color.beatwell_blue)
            }
            SUBSCRIPTION_MONTHLY -> {
                binding.cardMonthly.alpha = 1.0f
                binding.cardMonthly.strokeWidth = strokeWidthPx
                binding.cardMonthly.strokeColor = getColor(R.color.beatwell_blue)
            }
            SUBSCRIPTION_YEARLY -> {
                binding.cardYearly.alpha = 1.0f
                binding.cardYearly.strokeWidth = strokeWidthPx
                binding.cardYearly.strokeColor = getColor(R.color.beatwell_blue)
            }
        }
        
        // Enable continue button
        binding.btnContinue.isEnabled = true
        binding.btnContinue.alpha = 1.0f
    }
    
    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            if (selectedSubscription != null) {
                // Save selected subscription
                prefs.edit().putString(KEY_SELECTED_SUBSCRIPTION, selectedSubscription).apply()
                
                // Navigate to login activity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}

