package com.beatwell.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beatwell.app.auth.AuthManager
import com.beatwell.app.databinding.ActivityEditProfileBinding
import com.beatwell.app.models.User
import com.beatwell.app.network.NetworkManager
import com.beatwell.app.utils.BottomNavigationHelper
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var authManager: AuthManager
    private lateinit var networkManager: NetworkManager
    private lateinit var bottomNavigationHelper: BottomNavigationHelper
    private var currentUser: User? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize managers
        authManager = AuthManager(this)
        networkManager = NetworkManager(this)
        bottomNavigationHelper = BottomNavigationHelper(this)
        
        // Setup bottom navigation
        bottomNavigationHelper.setupBottomNavigation()
        bottomNavigationHelper.setCurrentTab(BottomNavigationHelper.Tab.PROFILE)
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Setup click listeners
        setupClickListeners()
        
        // Load user profile data
        loadUserProfile()
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.ivBackButton.setOnClickListener {
            finish()
        }
        
        // Save button
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
        
        // Date of birth picker
        binding.etDateOfBirth.setOnClickListener {
            showDatePicker()
        }
        
        // Gender spinner setup
        setupGenderSpinner()
    }
    
    private fun setupGenderSpinner() {
        val genderOptions = arrayOf("Male", "Female", "Other", "Prefer not to say")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = adapter
    }
    
    private fun loadUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionToken = authManager.getCurrentSessionToken()
                android.util.Log.d("EditProfileActivity", "Loading profile with session token: ${sessionToken?.take(10)}...")
                
                if (sessionToken != null) {
                    val result = networkManager.getUserProfile(sessionToken)
                    
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            try {
                                val responseData = result.getOrThrow()
                                android.util.Log.d("EditProfileActivity", "Profile API response: $responseData")
                                
                                val data = responseData["data"] as? Map<String, Any>
                                if (data != null) {
                                    val user = User.fromMap(data)
                                    currentUser = user
                                    populateForm(user)
                                    android.util.Log.d("EditProfileActivity", "Profile loaded successfully from API")
                                    Toast.makeText(this@EditProfileActivity, "Profile loaded from server", Toast.LENGTH_SHORT).show()
                                } else {
                                    android.util.Log.e("EditProfileActivity", "Profile API returned null data")
                                    // Try fallback to local data instead of finishing
                                    loadFallbackProfile()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("EditProfileActivity", "Error parsing profile data: ${e.message}", e)
                                loadFallbackProfile()
                            }
                        } else {
                            android.util.Log.e("EditProfileActivity", "Profile API failed: ${result.exceptionOrNull()?.message}")
                            // Try fallback to local data instead of finishing
                            loadFallbackProfile()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("EditProfileActivity", "No session token available")
                        loadFallbackProfile()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("EditProfileActivity", "Exception loading profile: ${e.message}", e)
                    loadFallbackProfile()
                }
            }
        }
    }
    
    private fun loadFallbackProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = authManager.getCurrentUser()
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        currentUser = user
                        populateForm(user)
                        android.util.Log.d("EditProfileActivity", "Using fallback profile data")
                        Toast.makeText(this@EditProfileActivity, "Using cached profile data", Toast.LENGTH_SHORT).show()
                    } else {
                        // Create minimal user from stored session data
                        val userId = authManager.getCurrentUserId()
                        val username = authManager.getCurrentUsername()
                        val email = authManager.getCurrentEmail()
                        val firstName = authManager.getCurrentFirstName()
                        
                        if (userId > 0 && !username.isNullOrEmpty()) {
                            val fallbackUser = User(
                                id = userId,
                                username = username,
                                email = email ?: "",
                                passwordHash = "",
                                firstName = firstName ?: username,
                                lastName = "",
                                phone = "",
                                dateOfBirth = "",
                                gender = ""
                            )
                            currentUser = fallbackUser
                            populateForm(fallbackUser)
                            android.util.Log.d("EditProfileActivity", "Using minimal fallback profile")
                            Toast.makeText(this@EditProfileActivity, "Limited profile data available", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@EditProfileActivity, "No profile data available", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("EditProfileActivity", "Error in fallback profile: ${e.message}", e)
                    Toast.makeText(this@EditProfileActivity, "Error loading profile data", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    private fun populateForm(user: User) {
        // Basic information
        binding.etFirstName.setText(user.firstName)
        binding.etLastName.setText(user.lastName)
        binding.etEmail.setText(user.email)
        binding.etPhone.setText(user.phone)
        
        // Date of birth
        binding.etDateOfBirth.setText(user.formattedDateOfBirth)
        
        // Gender
        val genderOptions = arrayOf("Male", "Female", "Other", "Prefer not to say")
        val genderIndex = genderOptions.indexOf(user.gender)
        if (genderIndex >= 0) {
            binding.spinnerGender.setSelection(genderIndex)
        }
        
        // Address information
        binding.etAddress.setText(user.address)
        binding.etCity.setText(user.city)
        binding.etState.setText(user.state)
        binding.etZipCode.setText(user.zipCode)
        
        // Emergency contact
        binding.etEmergencyContactName.setText(user.emergencyContactName)
        binding.etEmergencyContactPhone.setText(user.emergencyContactPhone)
        
        // Medical information
        binding.etMedicalConditions.setText(user.medicalConditions)
        binding.etAllergies.setText(user.allergies)
    }
    
    private fun showDatePicker() {
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
        
        // Set max date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        
        // Set min date to 100 years ago
        val minDate = Calendar.getInstance()
        minDate.set(year - 100, month, day)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        
        datePickerDialog.show()
    }
    
    private fun saveProfile() {
        if (!validateForm()) {
            return
        }
        
        val profileData = mapOf(
            "first_name" to binding.etFirstName.text.toString().trim(),
            "last_name" to binding.etLastName.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),
            "date_of_birth" to binding.etDateOfBirth.text.toString().trim(),
            "gender" to binding.spinnerGender.selectedItem.toString(),
            "address" to binding.etAddress.text.toString().trim(),
            "city" to binding.etCity.text.toString().trim(),
            "state" to binding.etState.text.toString().trim(),
            "zip_code" to binding.etZipCode.text.toString().trim(),
            "emergency_contact_name" to binding.etEmergencyContactName.text.toString().trim(),
            "emergency_contact_phone" to binding.etEmergencyContactPhone.text.toString().trim(),
            "medical_conditions" to binding.etMedicalConditions.text.toString().trim(),
            "allergies" to binding.etAllergies.text.toString().trim()
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionToken = authManager.getCurrentSessionToken()
                if (sessionToken != null) {
                    val result = networkManager.updateUserProfile(sessionToken, profileData)
                    
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@EditProfileActivity, "Failed to update profile: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "No active session", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun validateForm(): Boolean {
        var isValid = true
        
        // Clear previous errors
        binding.etFirstName.error = null
        binding.etLastName.error = null
        binding.etPhone.error = null
        binding.etDateOfBirth.error = null
        
        // Validate first name
        if (binding.etFirstName.text.toString().trim().isEmpty()) {
            binding.etFirstName.error = "First name is required"
            isValid = false
        }
        
        // Validate last name
        if (binding.etLastName.text.toString().trim().isEmpty()) {
            binding.etLastName.error = "Last name is required"
            isValid = false
        }
        
        // Validate phone
        val phone = binding.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone number is required"
            isValid = false
        } else if (!phone.matches(Regex("^[+]?[1-9][\\d]{0,15}$"))) {
            binding.etPhone.error = "Invalid phone number format"
            isValid = false
        }
        
        // Validate date of birth
        if (binding.etDateOfBirth.text.toString().trim().isEmpty()) {
            binding.etDateOfBirth.error = "Date of birth is required"
            isValid = false
        }
        
        return isValid
    }
}
