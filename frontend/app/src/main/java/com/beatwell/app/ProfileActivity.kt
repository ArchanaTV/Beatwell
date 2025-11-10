package com.beatwell.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.beatwell.app.auth.AuthManager
import com.beatwell.app.databinding.ActivityProfileBinding
import com.beatwell.app.models.User
import com.beatwell.app.network.NetworkManager
import com.beatwell.app.utils.BottomNavigationHelper
import kotlinx.coroutines.*

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfileBinding
    private lateinit var authManager: AuthManager
    private lateinit var networkManager: NetworkManager
    private lateinit var bottomNavigationHelper: BottomNavigationHelper
    private var currentUser: User? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityProfileBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Initialize managers with error handling
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
            
            // Show loading state initially
            showLoadingState()
            
            // Load user profile data
            loadUserProfile()
            
            android.util.Log.d("ProfileActivity", "ProfileActivity created successfully")
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error in onCreate: ${e.message}", e)
            // Try to show a basic error state instead of crashing
            try {
                Toast.makeText(this, "Error initializing profile screen", Toast.LENGTH_LONG).show()
                displayFallbackProfile()
            } catch (fallbackError: Exception) {
                android.util.Log.e("ProfileActivity", "Fallback also failed: ${fallbackError.message}", fallbackError)
                finish()
            }
        }
    }
    
    private fun setupClickListeners() {
        // Edit Profile button
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
        
        // View Meal Log History button
        binding.btnViewMealHistory.setOnClickListener {
            val intent = Intent(this, MealLogHistoryActivity::class.java)
            startActivity(intent)
        }
        
        // Logout button
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
        
        // Back button
        binding.ivBackButton.setOnClickListener {
            finish()
        }
    }
    
    private fun loadUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionToken = authManager.getCurrentSessionToken()
                if (sessionToken != null) {
                    android.util.Log.d("ProfileActivity", "Loading profile with session token: ${sessionToken.take(10)}...")
                    val result = networkManager.getUserProfile(sessionToken)
                    
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            try {
                                val responseData = result.getOrThrow()
                                android.util.Log.d("ProfileActivity", "Profile API response: $responseData")
                                
                                val data = responseData["data"] as? Map<String, Any>
                                if (data != null) {
                                    android.util.Log.d("ProfileActivity", "Profile data keys: ${data.keys}")
                                    android.util.Log.d("ProfileActivity", "Profile data values: $data")
                                    val user = User.fromMap(data)
                                    currentUser = user
                                    displayUserProfile(user)
                                    android.util.Log.d("ProfileActivity", "Profile loaded successfully from API")
                                    Toast.makeText(this@ProfileActivity, "Profile loaded from server", Toast.LENGTH_SHORT).show()
                                } else {
                                    android.util.Log.e("ProfileActivity", "Profile API returned null data")
                                    android.util.Log.e("ProfileActivity", "Full response: $responseData")
                                    // Show error but don't finish - display fallback profile
                                    displayFallbackProfile()
                                    Toast.makeText(this@ProfileActivity, "Profile data unavailable", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileActivity", "Error parsing profile data: ${e.message}", e)
                                displayFallbackProfile()
                                Toast.makeText(this@ProfileActivity, "Error parsing profile data", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.util.Log.e("ProfileActivity", "Profile API failed: ${result.exceptionOrNull()?.message}")
                            android.util.Log.e("ProfileActivity", "Session token used: ${sessionToken.take(10)}...")
                            // Show error but don't finish - display fallback profile
                            displayFallbackProfile()
                            Toast.makeText(this@ProfileActivity, "Failed to load profile from server", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("ProfileActivity", "No session token available")
                        // Show error but don't finish - display fallback profile
                        displayFallbackProfile()
                        Toast.makeText(this@ProfileActivity, "Please log in again", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("ProfileActivity", "Exception loading profile: ${e.message}", e)
                    // Show error but don't finish - display fallback profile
                    displayFallbackProfile()
                    Toast.makeText(this@ProfileActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun displayUserProfile(user: User) {
        try {
            // Set user name with safe fallback
            val displayName = when {
                !user.firstName.isNullOrEmpty() -> user.firstName
                !user.username.isNullOrEmpty() -> user.username
                else -> "User"
            }
            binding.tvUserName.text = displayName
            
            // Set profile information with null checks and safe fallbacks
            binding.tvEmailValue.text = if (user.email.isNullOrEmpty()) "Not set" else user.email
            binding.tvGenderValue.text = if (user.gender.isNullOrEmpty()) "Not set" else user.gender
            
            // Safe age calculation with multiple fallbacks
            binding.tvAgeValue.text = try {
                when {
                    user.age > 0 -> user.age.toString()
                    !user.dateOfBirth.isNullOrEmpty() -> {
                        // Try to calculate age from date of birth
                        val age = calculateAgeFromDateString(user.dateOfBirth)
                        if (age > 0) age.toString() else "Not set"
                    }
                    else -> "Not set"
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileActivity", "Error calculating age: ${e.message}")
                "Not set"
            }
            
            // Set medical and physical information from user data
            binding.tvHeightValue.text = user.height?.let { "${it} cm" } ?: "Not set"
            binding.tvWeightValue.text = user.weight?.let { "${it} kg" } ?: "Not set"
            
            val bloodPressure = if (user.bloodPressureSystolic != null && user.bloodPressureDiastolic != null) {
                "${user.bloodPressureSystolic}/${user.bloodPressureDiastolic} mmHg"
            } else {
                "Not set"
            }
            binding.tvBloodPressureValue.text = bloodPressure
            
            // Diabetes information
            val diabetesStatus = when (user.diabetesType) {
                "type1" -> "Type 1"
                "type2" -> "Type 2"
                "gestational" -> "Gestational"
                "none" -> "No"
                else -> "No"
            }
            binding.tvDiabetesValue.text = diabetesStatus
            
            // Treatment information
            binding.tvTreatmentTypeValue.text = user.treatmentType.ifEmpty { "None" }
            
            // Load profile image if available
            loadProfileImage()
            
            android.util.Log.d("ProfileActivity", "Profile displayed successfully for user: ${user.username}")
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error displaying profile: ${e.message}", e)
            // Don't crash - show fallback instead
            displayFallbackProfile()
            Toast.makeText(this, "Using fallback profile display", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayFallbackProfile() {
        try {
            // Try to get basic user info from AuthManager using synchronous methods
            val userId = authManager.getCurrentUserId()
            val username = authManager.getCurrentUsername()
            val email = authManager.getCurrentEmail()
            val firstName = authManager.getCurrentFirstName()
            val lastName = authManager.getCurrentLastName()
            
            if (userId > 0 && !username.isNullOrEmpty()) {
                binding.tvUserName.text = firstName ?: username
                binding.tvEmailValue.text = email ?: "Not available"
                binding.tvGenderValue.text = "Not set"
                binding.tvAgeValue.text = "Not set"
            } else {
                // Complete fallback when no user data is available
                binding.tvUserName.text = "User"
                binding.tvEmailValue.text = "Not available"
                binding.tvGenderValue.text = "Not set"
                binding.tvAgeValue.text = "Not set"
            }
            
            // Set default values for medical information
            binding.tvHeightValue.text = "Not set"
            binding.tvWeightValue.text = "Not set"
            binding.tvBloodPressureValue.text = "Not set"
            binding.tvDiabetesValue.text = "No"
            binding.tvTreatmentTypeValue.text = "None"
            
            // Load default profile image
            loadProfileImage()
            
            android.util.Log.d("ProfileActivity", "Fallback profile displayed")
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error in fallback profile: ${e.message}", e)
            // Last resort - set minimal safe values
            binding.tvUserName.text = "User"
            binding.tvEmailValue.text = "Not available"
            binding.tvGenderValue.text = "Not set"
            binding.tvAgeValue.text = "Not set"
            binding.tvHeightValue.text = "Not set"
            binding.tvWeightValue.text = "Not set"
            binding.tvBloodPressureValue.text = "Not set"
            binding.tvDiabetesValue.text = "No"
            binding.tvTreatmentTypeValue.text = "None"
        }
    }
    
    private fun calculateAgeFromDateString(dateOfBirth: String): Int {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val birthDate = sdf.parse(dateOfBirth)
            val today = java.util.Date()
            val diffInMilliseconds = today.time - birthDate.time
            val diffInYears = diffInMilliseconds / (365.25 * 24 * 60 * 60 * 1000)
            diffInYears.toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    private fun showLoadingState() {
        try {
            binding.tvUserName.text = "Loading..."
            binding.tvEmailValue.text = "Loading..."
            binding.tvGenderValue.text = "Loading..."
            binding.tvAgeValue.text = "Loading..."
            binding.tvHeightValue.text = "Loading..."
            binding.tvWeightValue.text = "Loading..."
            binding.tvBloodPressureValue.text = "Loading..."
            binding.tvDiabetesValue.text = "Loading..."
            binding.tvTreatmentTypeValue.text = "Loading..."
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error showing loading state: ${e.message}")
        }
    }
    
    private fun loadProfileImage() {
        try {
            // Try to load a default profile image from assets
            val inputStream = assets.open("images/profile_placeholder.png")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.ivProfilePicture.setImageBitmap(bitmap)
            inputStream.close()
        } catch (e: Exception) {
            // Use default drawable if no custom image is available
            binding.ivProfilePicture.setImageResource(R.drawable.ic_person)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning from other activities
        loadUserProfile()
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionToken = authManager.getCurrentSessionToken()
                if (sessionToken != null) {
                    // Call logout API (optional - local logout is primary)
                    try {
                        networkManager.logoutUser(sessionToken)
                    } catch (e: Exception) {
                        // API logout failed, but continue with local logout
                        android.util.Log.w("ProfileActivity", "API logout failed: ${e.message}")
                    }
                }
                
                // Clear local session (primary logout action)
                authManager.logout()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to landing screen and clear activity stack
                    val intent = Intent(this@ProfileActivity, LandingActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Even if logout fails, clear local session
                    authManager.logout()
                    Toast.makeText(this@ProfileActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(this@ProfileActivity, LandingActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
