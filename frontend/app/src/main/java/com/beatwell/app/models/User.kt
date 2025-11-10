package com.beatwell.app.models

import java.text.SimpleDateFormat
import java.util.*

data class User(
    val id: Long = 0,
    val username: String,
    val email: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val dateOfBirth: String,
    val gender: String,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val medicalConditions: String = "",
    val allergies: String = "",
    // New profile fields
    val height: Float? = null,
    val weight: Float? = null,
    val bloodPressureSystolic: Int? = null,
    val bloodPressureDiastolic: Int? = null,
    val diabetesType: String = "none",
    val treatmentType: String = ""
) {
    val fullName: String
        get() = "$firstName $lastName"

    val age: Int
        get() {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val birthDate = sdf.parse(dateOfBirth)
                val today = Date()
                val diffInMilliseconds = today.time - birthDate.time
                val diffInYears = diffInMilliseconds / (365.25 * 24 * 60 * 60 * 1000)
                diffInYears.toInt()
            } catch (e: Exception) {
                0
            }
        }

    val formattedDateOfBirth: String
        get() {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateOfBirth)
                outputFormat.format(date)
            } catch (e: Exception) {
                dateOfBirth
            }
        }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "username" to username,
            "email" to email,
            "first_name" to firstName,
            "last_name" to lastName,
            "phone" to phone,
            "date_of_birth" to dateOfBirth,
            "gender" to gender,
            "address" to address,
            "city" to city,
            "state" to state,
            "zip_code" to zipCode,
            "emergency_contact_name" to emergencyContactName,
            "emergency_contact_phone" to emergencyContactPhone,
            "medical_conditions" to medicalConditions,
            "allergies" to allergies
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): User {
            return User(
                id = (map["id"] as? Number)?.toLong() ?: 0,
                username = map["username"] as? String ?: "",
                email = map["email"] as? String ?: "",
                passwordHash = map["password_hash"] as? String ?: "",
                firstName = map["first_name"] as? String ?: "",
                lastName = map["last_name"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                dateOfBirth = map["date_of_birth"] as? String ?: "",
                gender = map["gender"] as? String ?: "",
                address = map["address"] as? String ?: "",
                city = map["city"] as? String ?: "",
                state = map["state"] as? String ?: "",
                zipCode = map["zip_code"] as? String ?: "",
                emergencyContactName = map["emergency_contact_name"] as? String ?: "",
                emergencyContactPhone = map["emergency_contact_phone"] as? String ?: "",
                medicalConditions = map["medical_conditions"] as? String ?: "",
                allergies = map["allergies"] as? String ?: "",
                // New profile fields
                height = (map["height"] as? Number)?.toFloat(),
                weight = (map["weight"] as? Number)?.toFloat(),
                bloodPressureSystolic = (map["blood_pressure_systolic"] as? Number)?.toInt(),
                bloodPressureDiastolic = (map["blood_pressure_diastolic"] as? Number)?.toInt(),
                diabetesType = map["diabetes_type"] as? String ?: "none",
                treatmentType = map["treatment_type"] as? String ?: ""
            )
        }
    }
}
