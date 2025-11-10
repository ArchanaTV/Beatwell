package com.beatwell.app

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beatwell.app.auth.AuthManager
import com.beatwell.app.auth.AuthResult
import com.beatwell.app.databinding.ActivitySignupBinding
import kotlinx.coroutines.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class SignupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySignupBinding
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize auth manager
        authManager = AuthManager(this)
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Load logo from assets
        loadLogoFromAssets()
        
        // Set up click listeners
        setupClickListeners()
        
        // Set up gender spinner
        setupGenderSpinner()
        
        // Set up date picker
        setupDatePicker()
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
    
    private fun setupClickListeners() {
        // Signup button click listener
        binding.btnSignup.setOnClickListener {
            performSignup()
        }
        
        // Login text click listener
        binding.tvLogin.setOnClickListener {
            // Navigate to login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun performSignup() {
        // Get all form data
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
        val gender = binding.spinnerGender.selectedItem?.toString() ?: ""
        val address = binding.etAddress.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val state = binding.etState.text.toString().trim()
        val zipCode = binding.etZipCode.text.toString().trim()
        val emergencyContactName = binding.etEmergencyContactName.text.toString().trim()
        val emergencyContactPhone = binding.etEmergencyContactPhone.text.toString().trim()
        val medicalConditions = binding.etMedicalConditions.text.toString().trim()
        val allergies = binding.etAllergies.text.toString().trim()
        
        // Basic validation for required fields
        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.error = "Username is required"
            binding.etUsername.requestFocus()
            return
        }
        
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.error = "Email is required"
            binding.etEmail.requestFocus()
            return
        }
        
        if (!isValidEmail(email)) {
            binding.tilEmail.error = "Please enter a valid email ending with .com"
            binding.etEmail.requestFocus()
            return
        }
        
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.error = "Password is required"
            binding.etPassword.requestFocus()
            return
        }
        
        if (password.length < 8) {
            binding.tilPassword.error = "Password must be at least 8 characters"
            binding.etPassword.requestFocus()
            return
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            binding.etConfirmPassword.requestFocus()
            return
        }
        
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            return
        }
        
        if (TextUtils.isEmpty(firstName)) {
            binding.tilFirstName.error = "First name is required"
            binding.etFirstName.requestFocus()
            return
        }
        
        if (TextUtils.isEmpty(lastName)) {
            binding.tilLastName.error = "Last name is required"
            binding.etLastName.requestFocus()
            return
        }
        
        if (TextUtils.isEmpty(phone)) {
            binding.tilPhone.error = "Phone number is required"
            binding.etPhone.requestFocus()
            return
        }
        
        if (TextUtils.isEmpty(dateOfBirth)) {
            binding.tilDateOfBirth.error = "Date of birth is required"
            binding.etDateOfBirth.requestFocus()
            return
        }
        
        if (gender.isEmpty() || gender == "Select Gender") {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Clear any previous errors
        clearAllErrors()
        
        // Show loading state
        binding.btnSignup.isEnabled = false
        binding.btnSignup.text = "Creating Account..."
        
        // Removed connectivity status for unified approach
        
        // Perform registration using auth manager
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = authManager.registerUser(
                    username, email, password, confirmPassword,
                    firstName, lastName, phone, dateOfBirth, gender,
                    address, city, state, zipCode,
                    emergencyContactName, emergencyContactPhone,
                    medicalConditions, allergies
                )
                
                withContext(Dispatchers.Main) {
                    when (result) {
                        is AuthResult.Success -> {
                            Toast.makeText(this@SignupActivity, result.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                            finish()
                        }
                        is AuthResult.Error -> {
                            Toast.makeText(this@SignupActivity, result.message, Toast.LENGTH_LONG).show()
                            binding.btnSignup.isEnabled = true
                            binding.btnSignup.text = "Sign Up"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SignupActivity, "Registration error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnSignup.isEnabled = true
                    binding.btnSignup.text = "Sign Up"
                }
            }
        }
    }
    
    private fun clearAllErrors() {
        binding.tilUsername.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tilFirstName.error = null
        binding.tilLastName.error = null
        binding.tilPhone.error = null
        binding.tilDateOfBirth.error = null
        binding.tilAddress.error = null
        binding.tilCity.error = null
        binding.tilState.error = null
        binding.tilZipCode.error = null
        binding.tilEmergencyContactName.error = null
        binding.tilEmergencyContactPhone.error = null
        binding.tilMedicalConditions.error = null
        binding.tilAllergies.error = null
    }
    
    private fun setupGenderSpinner() {
        val genderOptions = arrayOf("Select Gender", "Male", "Female", "Other", "Prefer not to say")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = adapter
    }
    
    private fun setupDatePicker() {
        binding.etDateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(selectedYear, selectedMonth, selectedDay)
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    binding.etDateOfBirth.setText(dateFormat.format(selectedDate.time))
                },
                year, month, day
            )
            
            // Set maximum date to today (can't be born in the future)
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            
            // Set minimum date to 120 years ago
            val minDate = Calendar.getInstance()
            minDate.add(Calendar.YEAR, -120)
            datePickerDialog.datePicker.minDate = minDate.timeInMillis
            
            datePickerDialog.show()
        }
    }
    
    
    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.com$"
        val pattern = Pattern.compile(emailPattern)
        return pattern.matcher(email).matches()
    }
}
