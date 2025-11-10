package com.beatwell.app.utils

import java.security.MessageDigest
import java.util.regex.Pattern

object PasswordUtils {
    
    /**
     * Hash a password using SHA-256
     */
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verify a password against its hash
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
    
    /**
     * Validate password strength
     */
    fun validatePassword(password: String): PasswordValidationResult {
        if (password.length < 8) {
            return PasswordValidationResult(false, "Password must be at least 8 characters long")
        }
        
        if (!password.any { it.isLowerCase() }) {
            return PasswordValidationResult(false, "Password must contain at least one lowercase letter")
        }
        
        if (!password.any { it.isUpperCase() }) {
            return PasswordValidationResult(false, "Password must contain at least one uppercase letter")
        }
        
        if (!password.any { it.isDigit() }) {
            return PasswordValidationResult(false, "Password must contain at least one number")
        }
        
        val specialCharPattern = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]")
        if (!specialCharPattern.matcher(password).find()) {
            return PasswordValidationResult(false, "Password must contain at least one special character")
        }
        
        return PasswordValidationResult(true, "Password is valid")
    }
    
    /**
     * Generate a secure session token
     */
    fun generateSessionToken(): String {
        return java.util.UUID.randomUUID().toString().replace("-", "")
    }
}

data class PasswordValidationResult(
    val isValid: Boolean,
    val message: String
)
