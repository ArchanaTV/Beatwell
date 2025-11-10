package com.beatwell.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beatwell.app.databinding.ActivityCustomFoodBinding
import com.beatwell.app.network.NetworkManager
import kotlinx.coroutines.*

class CustomFoodActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCustomFoodBinding
    private lateinit var networkManager: NetworkManager
    private lateinit var mealType: String
    private var portionSize: Float = 0.5f // Default portion size (50%)
    
    companion object {
        const val EXTRA_MEAL_TYPE = "meal_type"
        const val MEAL_BREAKFAST = "breakfast"
        const val MEAL_LUNCH = "lunch"
        const val MEAL_DINNER = "dinner"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomFoodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize network manager
        networkManager = NetworkManager(this)
        
        // Get meal type from intent
        mealType = intent.getStringExtra(EXTRA_MEAL_TYPE) ?: MEAL_BREAKFAST
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Initialize UI
        initializeUI()
        
        // Set up click listeners
        setupClickListeners()
    }
    
    private fun initializeUI() {
        // Set meal type title
        binding.tvMealTitle.text = "CUSTOM ${mealType.uppercase()}"
        
        // Initialize portion size slider
        binding.portionSlider.value = portionSize
        binding.portionSlider.addOnChangeListener { _, value, _ ->
            portionSize = value
            updatePortionSizeText()
        }
        
        updatePortionSizeText()
        
        // Set up input fields
        binding.etFoodName.hint = "Enter food name"
        binding.etNotes.hint = "Add any notes (optional)"
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.ivBackButton.setOnClickListener {
            finish()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            saveCustomFood()
        }
    }
    
    private fun updatePortionSizeText() {
        val percentage = (portionSize * 100).toInt()
        binding.tvPortionSize.text = "Portion size: $percentage%"
    }
    
    private fun saveCustomFood() {
        val foodName = binding.etFoodName.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()
        
        // Validate input
        if (foodName.isEmpty()) {
            binding.etFoodName.error = "Please enter a food name"
            binding.etFoodName.requestFocus()
            return
        }
        
        // Show loading state
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Calculate calories (you might want to implement a calorie estimation API)
                val estimatedCalories = estimateCalories(foodName, portionSize)
                
                // Save custom food data to backend
                val result = networkManager.saveCustomFood(
                    mealType = mealType,
                    foodName = foodName,
                    notes = notes,
                    portionSize = portionSize,
                    calories = estimatedCalories
                )
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(this@CustomFoodActivity, "Custom food saved successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CustomFoodActivity, "Failed to save custom food. Please try again.", Toast.LENGTH_SHORT).show()
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CustomFoodActivity, "Error saving custom food: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                }
            }
        }
    }
    
    private fun estimateCalories(foodName: String, portionSize: Float): Int {
        // Simple calorie estimation based on common foods
        // In a real app, you might want to use a food database API
        val baseCalories = when (foodName.lowercase()) {
            "rice" -> 130
            "bread" -> 80
            "chicken" -> 165
            "fish" -> 150
            "vegetables" -> 25
            "fruits" -> 60
            "milk" -> 42
            "eggs" -> 70
            "pasta" -> 131
            "potato" -> 77
            else -> 100 // Default estimation
        }
        
        return (baseCalories * portionSize).toInt()
    }
}

