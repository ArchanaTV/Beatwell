package com.beatwell.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.beatwell.app.adapters.MealOptionAdapter
import com.beatwell.app.databinding.ActivityMealBinding
import com.beatwell.app.models.MealOption
import com.beatwell.app.network.NetworkManager
import kotlinx.coroutines.*

class MealActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMealBinding
    private lateinit var networkManager: NetworkManager
    private lateinit var mealType: String
    private var selectedMealOption: MealOption? = null
    private var portionSize: Float = 0.5f // Default portion size (50%)
    
    companion object {
        const val EXTRA_MEAL_TYPE = "meal_type"
        const val MEAL_BREAKFAST = "breakfast"
        const val MEAL_LUNCH = "lunch"
        const val MEAL_DINNER = "dinner"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealBinding.inflate(layoutInflater)
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
        
        // Load meal options
        loadMealOptions()
    }
    
    private fun initializeUI() {
        // Set meal type title
        binding.tvMealTitle.text = mealType.uppercase()
        
        // Set inspirational quote
        binding.tvInspirationalQuote.text = "\"Your heart beats for you—fuel it with care and consistency.\""
        
        // Initialize portion size slider
        binding.portionSlider.value = portionSize
        binding.portionSlider.addOnChangeListener { _, value, _ ->
            portionSize = value
            updatePortionSizeText()
        }
        
        updatePortionSizeText()
        
        // Set up meal options recycler view
        binding.recyclerViewMealOptions.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.ivBackButton.setOnClickListener {
            finish()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            saveMealSelection()
        }
        
        // Custom meal button
        binding.btnCustomMeal.setOnClickListener {
            showCustomMealDialog()
        }
    }
    
    private fun loadMealOptions() {
        val mealOptions = getMealOptionsForType(mealType)
        
        val adapter = MealOptionAdapter(mealOptions) { selectedOption ->
            selectedMealOption = selectedOption
            updateSaveButtonState()
        }
        
        binding.recyclerViewMealOptions.adapter = adapter
    }
    
    private fun getMealOptionsForType(mealType: String): List<MealOption> {
        return when (mealType) {
            MEAL_BREAKFAST -> listOf(
                MealOption(
                    id = 1,
                    name = "Oats with fruits and nuts",
                    description = "Healthy oats with fresh fruits and mixed nuts",
                    calories = 350,
                    imageUrl = "https://example.com/oats.jpg"
                ),
                MealOption(
                    id = 2,
                    name = "Scrambled eggs with toast",
                    description = "Protein-rich scrambled eggs with whole wheat toast",
                    calories = 280,
                    imageUrl = "https://example.com/eggs.jpg"
                ),
                MealOption(
                    id = 3,
                    name = "Smoothie bowl",
                    description = "Nutritious smoothie bowl with berries and granola",
                    calories = 320,
                    imageUrl = "https://example.com/smoothie.jpg"
                ),
                MealOption(
                    id = 4,
                    name = "Greek yogurt parfait",
                    description = "Greek yogurt with honey, granola, and fresh berries",
                    calories = 250,
                    imageUrl = "https://example.com/yogurt.jpg"
                )
            )
            MEAL_LUNCH -> listOf(
                MealOption(
                    id = 5,
                    name = "Grilled chicken salad",
                    description = "Fresh mixed greens with grilled chicken breast",
                    calories = 420,
                    imageUrl = "https://example.com/salad.jpg"
                ),
                MealOption(
                    id = 6,
                    name = "Quinoa bowl",
                    description = "Quinoa with roasted vegetables and tahini dressing",
                    calories = 380,
                    imageUrl = "https://example.com/quinoa.jpg"
                ),
                MealOption(
                    id = 7,
                    name = "Lentil curry with rice",
                    description = "Spiced lentil curry served with brown rice",
                    calories = 450,
                    imageUrl = "https://example.com/lentil.jpg"
                ),
                MealOption(
                    id = 8,
                    name = "Vegetable stir-fry",
                    description = "Mixed vegetables stir-fried with tofu and brown rice",
                    calories = 320,
                    imageUrl = "https://example.com/stirfry.jpg"
                )
            )
            MEAL_DINNER -> listOf(
                MealOption(
                    id = 9,
                    name = "Millet upma + light coconut chutney",
                    description = "Healthy millet upma with coconut chutney",
                    calories = 320,
                    imageUrl = "https://example.com/upma.jpg"
                ),
                MealOption(
                    id = 10,
                    name = "Ragi idiyappam + vegetable curry",
                    description = "Traditional ragi idiyappam with mixed vegetable curry",
                    calories = 380,
                    imageUrl = "https://example.com/idiyappam.jpg"
                ),
                MealOption(
                    id = 11,
                    name = "Ragi dosa with chutney",
                    description = "Nutritious ragi dosa served with coconut chutney",
                    calories = 280,
                    imageUrl = "https://example.com/dosa.jpg"
                ),
                MealOption(
                    id = 12,
                    name = "Idiyappam, light sambar",
                    description = "Soft idiyappam with light sambar",
                    calories = 300,
                    imageUrl = "https://example.com/idiyappam2.jpg"
                ),
                MealOption(
                    id = 13,
                    name = "Wheat khichdi",
                    description = "Comforting wheat khichdi with vegetables",
                    calories = 350,
                    imageUrl = "https://example.com/khichdi.jpg"
                ),
                MealOption(
                    id = 14,
                    name = "Idli (3 pcs) + sambar",
                    description = "Traditional idli served with sambar",
                    calories = 250,
                    imageUrl = "https://example.com/idli.jpg"
                )
            )
            else -> emptyList()
        }
    }
    
    private fun updatePortionSizeText() {
        val percentage = (portionSize * 100).toInt()
        binding.tvPortionSize.text = "Portion size: $percentage%"
    }
    
    private fun updateSaveButtonState() {
        binding.btnSave.isEnabled = selectedMealOption != null
        binding.btnSave.alpha = if (selectedMealOption != null) 1.0f else 0.5f
    }
    
    private fun saveMealSelection() {
        if (selectedMealOption == null) {
            Toast.makeText(this, "Please select a meal option", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Calculate actual calories based on portion size
                val actualCalories = (selectedMealOption!!.calories * portionSize).toInt()
                
                // Save meal data to backend
                val result = networkManager.saveMealData(
                    mealType = mealType,
                    mealOption = selectedMealOption!!,
                    portionSize = portionSize,
                    calories = actualCalories
                )
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(this@MealActivity, "Meal saved successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@MealActivity, "Failed to save meal. Please try again.", Toast.LENGTH_SHORT).show()
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MealActivity, "Error saving meal: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                }
            }
        }
    }
    
    private fun showCustomMealDialog() {
        val intent = Intent(this, CustomFoodActivity::class.java)
        intent.putExtra(CustomFoodActivity.EXTRA_MEAL_TYPE, mealType)
        startActivity(intent)
    }
}
