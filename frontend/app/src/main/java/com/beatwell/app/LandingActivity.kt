package com.beatwell.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.beatwell.app.databinding.ActivityLandingBinding
import java.io.IOException

class LandingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLandingBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide action bar for full screen experience
        supportActionBar?.hide()
        
        // Load logo from assets
        loadLogoFromAssets()
        
        // Set up click listener for Get Started button
        binding.btnGetStarted.setOnClickListener {
            // Navigate to login activity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
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
}

