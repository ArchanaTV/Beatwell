package com.beatwell.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beatwell.app.auth.AuthManager
import com.beatwell.app.databinding.ActivityDebugBinding
import com.beatwell.app.network.NetworkManager
import kotlinx.coroutines.*

class DebugActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDebugBinding
    private lateinit var networkManager: NetworkManager
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        networkManager = NetworkManager(this)
        authManager = AuthManager(this)
        
        setupClickListeners()
        updateStatus()
    }
    
    private fun setupClickListeners() {
        binding.btnTestConnectivity.setOnClickListener {
            testConnectivity()
        }
        
        binding.btnTestApiLogin.setOnClickListener {
            testApiLogin()
        }
        
        binding.btnTestRegistration.setOnClickListener {
            testRegistration()
        }
        
        binding.btnClearLocalData.setOnClickListener {
            clearLocalData()
        }
    }
    
    private fun updateStatus() {
        binding.tvNetworkStatus.text = "Network: ${if (networkManager.isNetworkAvailable()) "Available" else "Not Available"}"
        binding.tvApiUrl.text = "API URL: ${com.beatwell.app.network.ApiConfig.BASE_URL}"
        binding.tvLoggedIn.text = "Logged In: ${authManager.isLoggedInSync()}"
        binding.tvCurrentUser.text = "User: ${authManager.getCurrentUsername() ?: "None"}"
    }
    
    private fun testConnectivity() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.tvResults.text = "Testing connectivity..."
            
            try {
                val result = networkManager.testApiConnectivity()
                if (result.isSuccess) {
                    binding.tvResults.text = "✅ ${result.getOrNull()}"
                } else {
                    binding.tvResults.text = "❌ ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                binding.tvResults.text = "❌ Error: ${e.message}"
            }
        }
    }
    
    private fun testApiLogin() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.tvResults.text = "Testing API login..."
            
            try {
                // Test with a user we know exists and the correct password
                val result = authManager.loginUser("testauth_1761572177", "TestAuth123!")
                when (result) {
                    is com.beatwell.app.auth.AuthResult.Success -> {
                        binding.tvResults.text = "✅ Login: ${result.message}"
                        updateStatus()
                    }
                    is com.beatwell.app.auth.AuthResult.Error -> {
                        binding.tvResults.text = "❌ Login: ${result.message}\n\nTrying with BaraniS..."
                        
                        // Try with different passwords for BaraniS
                        val passwords = listOf("Test123!", "TestPass123!", "Password123!", "Barani123!")
                        var loginSuccess = false
                        
                        for (password in passwords) {
                            val testResult = authManager.loginUser("BaraniS", password)
                            if (testResult is com.beatwell.app.auth.AuthResult.Success) {
                                binding.tvResults.text = "✅ Login successful with BaraniS and password: $password"
                                loginSuccess = true
                                updateStatus()
                                break
                            }
                        }
                        
                        if (!loginSuccess) {
                            binding.tvResults.text = "❌ Login failed for BaraniS with all test passwords"
                        }
                    }
                }
            } catch (e: Exception) {
                binding.tvResults.text = "❌ Login Error: ${e.message}"
            }
        }
    }
    
    private fun testRegistration() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.tvResults.text = "Testing registration..."
            
            try {
                val result = authManager.registerUser(
                    username = "debuguser",
                    email = "debug@example.com",
                    password = "DebugTest123!",
                    confirmPassword = "DebugTest123!",
                    firstName = "Debug",
                    lastName = "User",
                    phone = "1234567890",
                    dateOfBirth = "01/15/1990",
                    gender = "Male"
                )
                when (result) {
                    is com.beatwell.app.auth.AuthResult.Success -> {
                        binding.tvResults.text = "✅ Registration: ${result.message}"
                        updateStatus()
                    }
                    is com.beatwell.app.auth.AuthResult.Error -> {
                        binding.tvResults.text = "❌ Registration: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                binding.tvResults.text = "❌ Registration Error: ${e.message}"
            }
        }
    }
    
    private fun clearLocalData() {
        authManager.logout()
        updateStatus()
        binding.tvResults.text = "Local data cleared"
        Toast.makeText(this, "Local data cleared", Toast.LENGTH_SHORT).show()
    }
}