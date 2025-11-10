package com.beatwell.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beatwell.app.auth.AuthManager
import com.beatwell.app.auth.AuthResult
import com.beatwell.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.*
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize auth manager
        authManager = AuthManager(this)
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Load logo from assets
        loadLogoFromAssets()
        
        // Set up click listeners
        setupClickListeners()
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
        // Login button click listener
        binding.btnLogin.setOnClickListener {
            performLogin()
        }
        
        // Sign up text click listener
        binding.tvSignUp.setOnClickListener {
            // Navigate to sign up screen
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
        
        // Forgot password click listener
        binding.tvForgotPassword.setOnClickListener {
            // TODO: Implement forgot password functionality
            Toast.makeText(this, "Forgot password feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        // Basic validation
        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.error = "Username is required"
            binding.etUsername.requestFocus()
            return
        }
        
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.error = "Password is required"
            binding.etPassword.requestFocus()
            return
        }
        
        // Clear any previous errors
        binding.tilUsername.error = null
        binding.tilPassword.error = null
        
        // Show loading state
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Signing In..."
        
        // Removed connectivity status for unified approach
        
        // Perform login using auth manager
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = authManager.loginUser(username, password)
                
                withContext(Dispatchers.Main) {
                    when (result) {
                        is AuthResult.Success -> {
                            Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        is AuthResult.Error -> {
                            Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.text = "Sign In"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Login error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Sign In"
                }
            }
        }
    }
    
}
