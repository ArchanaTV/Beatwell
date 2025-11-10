package com.beatwell.app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.beatwell.app.models.User
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "BeatWellDB"
        private const val DATABASE_VERSION = 3

        // Users table
        private const val TABLE_USERS = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD_HASH = "password_hash"
        private const val COLUMN_FIRST_NAME = "first_name"
        private const val COLUMN_LAST_NAME = "last_name"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_DATE_OF_BIRTH = "date_of_birth"
        private const val COLUMN_GENDER = "gender"
        private const val COLUMN_ADDRESS = "address"
        private const val COLUMN_CITY = "city"
        private const val COLUMN_STATE = "state"
        private const val COLUMN_ZIP_CODE = "zip_code"
        private const val COLUMN_EMERGENCY_CONTACT_NAME = "emergency_contact_name"
        private const val COLUMN_EMERGENCY_CONTACT_PHONE = "emergency_contact_phone"
        private const val COLUMN_MEDICAL_CONDITIONS = "medical_conditions"
        private const val COLUMN_ALLERGIES = "allergies"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"

        // Sessions table
        private const val TABLE_SESSIONS = "sessions"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_SESSION_TOKEN = "session_token"
        private const val COLUMN_EXPIRES_AT = "expires_at"

        // Meal logs table
        private const val TABLE_MEAL_LOGS = "meal_logs"
        private const val COLUMN_MEAL_TYPE = "meal_type"
        private const val COLUMN_MEAL_OPTION_ID = "meal_option_id"
        private const val COLUMN_MEAL_OPTION_NAME = "meal_option_name"
        private const val COLUMN_MEAL_OPTION_DESCRIPTION = "meal_option_description"
        private const val COLUMN_PORTION_SIZE = "portion_size"
        private const val COLUMN_CALORIES = "calories"
        private const val COLUMN_IS_CUSTOM = "is_custom"
        private const val COLUMN_LOGGED_AT = "logged_at"

        // Water intake table
        private const val TABLE_WATER_INTAKE = "water_intake"
        private const val COLUMN_GLASSES = "glasses"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD_HASH TEXT NOT NULL,
                $COLUMN_FIRST_NAME TEXT NOT NULL,
                $COLUMN_LAST_NAME TEXT NOT NULL,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_DATE_OF_BIRTH TEXT NOT NULL,
                $COLUMN_GENDER TEXT NOT NULL,
                $COLUMN_ADDRESS TEXT,
                $COLUMN_CITY TEXT,
                $COLUMN_STATE TEXT,
                $COLUMN_ZIP_CODE TEXT,
                $COLUMN_EMERGENCY_CONTACT_NAME TEXT,
                $COLUMN_EMERGENCY_CONTACT_PHONE TEXT,
                $COLUMN_MEDICAL_CONDITIONS TEXT,
                $COLUMN_ALLERGIES TEXT,
                $COLUMN_CREATED_AT TEXT NOT NULL,
                $COLUMN_UPDATED_AT TEXT NOT NULL
            )
        """.trimIndent()

        // Create sessions table
        val createSessionsTable = """
            CREATE TABLE $TABLE_SESSIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_SESSION_TOKEN TEXT UNIQUE NOT NULL,
                $COLUMN_EXPIRES_AT TEXT NOT NULL,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS ($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        // Create meal logs table
        val createMealLogsTable = """
            CREATE TABLE $TABLE_MEAL_LOGS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_MEAL_TYPE TEXT NOT NULL,
                $COLUMN_MEAL_OPTION_ID INTEGER NOT NULL,
                $COLUMN_MEAL_OPTION_NAME TEXT NOT NULL,
                $COLUMN_MEAL_OPTION_DESCRIPTION TEXT,
                $COLUMN_PORTION_SIZE REAL NOT NULL,
                $COLUMN_CALORIES INTEGER NOT NULL,
                $COLUMN_IS_CUSTOM INTEGER NOT NULL DEFAULT 0,
                $COLUMN_LOGGED_AT TEXT NOT NULL,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS ($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        // Create water intake table
        val createWaterIntakeTable = """
            CREATE TABLE $TABLE_WATER_INTAKE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_GLASSES INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CREATED_AT TEXT NOT NULL,
                $COLUMN_UPDATED_AT TEXT NOT NULL,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS ($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createSessionsTable)
        db.execSQL(createMealLogsTable)
        db.execSQL(createWaterIntakeTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Create new tables for version 2
            val createMealLogsTable = """
                CREATE TABLE $TABLE_MEAL_LOGS (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_USER_ID INTEGER NOT NULL,
                    $COLUMN_MEAL_TYPE TEXT NOT NULL,
                    $COLUMN_MEAL_OPTION_ID INTEGER NOT NULL,
                    $COLUMN_MEAL_OPTION_NAME TEXT NOT NULL,
                    $COLUMN_MEAL_OPTION_DESCRIPTION TEXT,
                    $COLUMN_PORTION_SIZE REAL NOT NULL,
                    $COLUMN_CALORIES INTEGER NOT NULL,
                    $COLUMN_IS_CUSTOM INTEGER NOT NULL DEFAULT 0,
                    $COLUMN_LOGGED_AT TEXT NOT NULL,
                    FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS ($COLUMN_ID) ON DELETE CASCADE
                )
            """.trimIndent()

            val createWaterIntakeTable = """
                CREATE TABLE $TABLE_WATER_INTAKE (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_USER_ID INTEGER NOT NULL,
                    $COLUMN_GLASSES INTEGER NOT NULL DEFAULT 0,
                    $COLUMN_CREATED_AT TEXT NOT NULL,
                    $COLUMN_UPDATED_AT TEXT NOT NULL,
                    FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS ($COLUMN_ID) ON DELETE CASCADE
                )
            """.trimIndent()

            db.execSQL(createMealLogsTable)
            db.execSQL(createWaterIntakeTable)
        }
        
        if (oldVersion < 3) {
            // Drop and recreate water intake table to fix UNIQUE constraint issue
            db.execSQL("DROP TABLE IF EXISTS $TABLE_WATER_INTAKE")
            val createWaterIntakeTable = """
                CREATE TABLE $TABLE_WATER_INTAKE (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_USER_ID INTEGER NOT NULL,
                    $COLUMN_GLASSES INTEGER NOT NULL DEFAULT 0,
                    $COLUMN_CREATED_AT TEXT NOT NULL,
                    $COLUMN_UPDATED_AT TEXT NOT NULL,
                    FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS ($COLUMN_ID) ON DELETE CASCADE
                )
            """.trimIndent()
            db.execSQL(createWaterIntakeTable)
        }
    }

    // User operations
    fun insertUser(user: User): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, user.username)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PASSWORD_HASH, user.passwordHash)
            put(COLUMN_FIRST_NAME, user.firstName)
            put(COLUMN_LAST_NAME, user.lastName)
            put(COLUMN_PHONE, user.phone)
            put(COLUMN_DATE_OF_BIRTH, user.dateOfBirth)
            put(COLUMN_GENDER, user.gender)
            put(COLUMN_ADDRESS, user.address)
            put(COLUMN_CITY, user.city)
            put(COLUMN_STATE, user.state)
            put(COLUMN_ZIP_CODE, user.zipCode)
            put(COLUMN_EMERGENCY_CONTACT_NAME, user.emergencyContactName)
            put(COLUMN_EMERGENCY_CONTACT_PHONE, user.emergencyContactPhone)
            put(COLUMN_MEDICAL_CONDITIONS, user.medicalConditions)
            put(COLUMN_ALLERGIES, user.allergies)
            put(COLUMN_CREATED_AT, getCurrentDateTime())
            put(COLUMN_UPDATED_AT, getCurrentDateTime())
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun getUserByUsername(username: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USERNAME = ?",
            arrayOf(username),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH)),
                firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME)),
                lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                dateOfBirth = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_OF_BIRTH)),
                gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)),
                address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)),
                city = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY)),
                state = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATE)),
                zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE)),
                emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME)),
                emergencyContactPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_PHONE)),
                medicalConditions = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDICAL_CONDITIONS)),
                allergies = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ALLERGIES))
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH)),
                firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME)),
                lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                dateOfBirth = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_OF_BIRTH)),
                gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)),
                address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)),
                city = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY)),
                state = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATE)),
                zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE)),
                emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME)),
                emergencyContactPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_PHONE)),
                medicalConditions = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDICAL_CONDITIONS)),
                allergies = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ALLERGIES))
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun checkUserExists(username: String, email: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_USERNAME = ? OR $COLUMN_EMAIL = ?",
            arrayOf(username, email),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun updateUser(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FIRST_NAME, user.firstName)
            put(COLUMN_LAST_NAME, user.lastName)
            put(COLUMN_PHONE, user.phone)
            put(COLUMN_DATE_OF_BIRTH, user.dateOfBirth)
            put(COLUMN_GENDER, user.gender)
            put(COLUMN_ADDRESS, user.address)
            put(COLUMN_CITY, user.city)
            put(COLUMN_STATE, user.state)
            put(COLUMN_ZIP_CODE, user.zipCode)
            put(COLUMN_EMERGENCY_CONTACT_NAME, user.emergencyContactName)
            put(COLUMN_EMERGENCY_CONTACT_PHONE, user.emergencyContactPhone)
            put(COLUMN_MEDICAL_CONDITIONS, user.medicalConditions)
            put(COLUMN_ALLERGIES, user.allergies)
            put(COLUMN_UPDATED_AT, getCurrentDateTime())
        }
        val result = db.update(TABLE_USERS, values, "$COLUMN_ID = ?", arrayOf(user.id.toString()))
        return result > 0
    }

    // Session operations
    fun createSession(userId: Long, sessionToken: String, expiresAt: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_SESSION_TOKEN, sessionToken)
            put(COLUMN_EXPIRES_AT, expiresAt)
        }
        return db.insert(TABLE_SESSIONS, null, values)
    }

    fun getValidSession(sessionToken: String): Pair<User?, String?>? {
        val db = readableDatabase
        val query = """
            SELECT u.*, s.$COLUMN_EXPIRES_AT 
            FROM $TABLE_USERS u 
            JOIN $TABLE_SESSIONS s ON u.$COLUMN_ID = s.$COLUMN_USER_ID 
            WHERE s.$COLUMN_SESSION_TOKEN = ? AND s.$COLUMN_EXPIRES_AT > datetime('now')
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(sessionToken))

        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH)),
                firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME)),
                lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                dateOfBirth = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_OF_BIRTH)),
                gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)),
                address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)),
                city = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY)),
                state = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATE)),
                zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE)),
                emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME)),
                emergencyContactPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_PHONE)),
                medicalConditions = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDICAL_CONDITIONS)),
                allergies = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ALLERGIES))
            )
            val expiresAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPIRES_AT))
            cursor.close()
            Pair(user, expiresAt)
        } else {
            cursor.close()
            null
        }
    }

    fun deleteSession(sessionToken: String): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_SESSIONS, "$COLUMN_SESSION_TOKEN = ?", arrayOf(sessionToken))
        return result > 0
    }

    fun deleteAllSessionsForUser(userId: Long): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_SESSIONS, "$COLUMN_USER_ID = ?", arrayOf(userId.toString()))
        return result > 0
    }

    fun clearAllSessions(): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_SESSIONS, null, null)
        return result >= 0
    }

    // Meal operations
    fun insertMealLog(
        userId: Long,
        mealType: String,
        mealOptionId: Int,
        mealOptionName: String,
        mealOptionDescription: String,
        portionSize: Float,
        calories: Int,
        isCustom: Boolean
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_MEAL_TYPE, mealType)
            put(COLUMN_MEAL_OPTION_ID, mealOptionId)
            put(COLUMN_MEAL_OPTION_NAME, mealOptionName)
            put(COLUMN_MEAL_OPTION_DESCRIPTION, mealOptionDescription)
            put(COLUMN_PORTION_SIZE, portionSize)
            put(COLUMN_CALORIES, calories)
            put(COLUMN_IS_CUSTOM, if (isCustom) 1 else 0)
            put(COLUMN_LOGGED_AT, getCurrentDateTime())
        }
        return db.insert(TABLE_MEAL_LOGS, null, values)
    }

    fun getMealsForMonth(userId: Long, year: Int, month: Int): List<Map<String, Any>> {
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_MEAL_LOGS 
            WHERE $COLUMN_USER_ID = ? 
            AND strftime('%Y', $COLUMN_LOGGED_AT) = ? 
            AND strftime('%m', $COLUMN_LOGGED_AT) = ?
            ORDER BY $COLUMN_LOGGED_AT DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString(), year.toString(), String.format("%02d", month + 1)))
        val meals = mutableListOf<Map<String, Any>>()

        while (cursor.moveToNext()) {
            val meal = mapOf(
                "id" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                "user_id" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                "meal_type" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_TYPE)),
                "meal_option_id" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_ID)),
                "meal_option_name" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_NAME)),
                "meal_option_description" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_DESCRIPTION)),
                "portion_size" to cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_PORTION_SIZE)),
                "calories" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CALORIES)),
                "is_custom" to (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CUSTOM)) == 1),
                "logged_at" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGGED_AT))
            )
            meals.add(meal)
        }
        cursor.close()
        return meals
    }

    fun getTodayMeals(userId: Long): List<Map<String, Any>> {
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_MEAL_LOGS 
            WHERE $COLUMN_USER_ID = ? 
            AND DATE($COLUMN_LOGGED_AT) = DATE('now')
            ORDER BY $COLUMN_LOGGED_AT DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        val meals = mutableListOf<Map<String, Any>>()

        while (cursor.moveToNext()) {
            val meal = mapOf(
                "id" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                "user_id" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                "meal_type" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_TYPE)),
                "meal_option_id" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_ID)),
                "meal_option_name" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_NAME)),
                "meal_option_description" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_DESCRIPTION)),
                "portion_size" to cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_PORTION_SIZE)),
                "calories" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CALORIES)),
                "is_custom" to (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CUSTOM)) == 1),
                "logged_at" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGGED_AT))
            )
            meals.add(meal)
        }
        cursor.close()
        return meals
    }

    fun getMealsForDate(userId: Long, date: String): List<Map<String, Any>> {
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_MEAL_LOGS 
            WHERE $COLUMN_USER_ID = ? AND DATE($COLUMN_LOGGED_AT) = ?
            ORDER BY $COLUMN_LOGGED_AT ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString(), date))
        val meals = mutableListOf<Map<String, Any>>()
        
        while (cursor.moveToNext()) {
            val meal = mapOf(
                "id" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                "user_id" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                "meal_type" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_TYPE)),
                "meal_option_id" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_ID)),
                "meal_option_name" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_NAME)),
                "meal_option_description" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_DESCRIPTION)),
                "portion_size" to cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_PORTION_SIZE)),
                "calories" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CALORIES)),
                "is_custom" to (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CUSTOM)) == 1),
                "logged_at" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGGED_AT))
            )
            meals.add(meal)
        }
        cursor.close()
        return meals
    }

    fun getAllMealsHistory(userId: Long, limit: Int = 50, offset: Int = 0): List<Map<String, Any>> {
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_MEAL_LOGS 
            WHERE $COLUMN_USER_ID = ? 
            ORDER BY $COLUMN_LOGGED_AT DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString(), limit.toString(), offset.toString()))
        val meals = mutableListOf<Map<String, Any>>()
        
        while (cursor.moveToNext()) {
            val meal = mapOf(
                "id" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                "user_id" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                "meal_type" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_TYPE)),
                "meal_option_id" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_ID)),
                "meal_option_name" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_NAME)),
                "meal_option_description" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_OPTION_DESCRIPTION)),
                "portion_size" to cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_PORTION_SIZE)),
                "calories" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CALORIES)),
                "is_custom" to (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CUSTOM)) == 1),
                "logged_at" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGGED_AT))
            )
            meals.add(meal)
        }
        cursor.close()
        return meals
    }

    // Water intake operations
    fun saveWaterIntake(userId: Long, glasses: Int): Long {
        val db = writableDatabase
        
        // Check if water intake already exists for today
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val checkQuery = """
            SELECT $COLUMN_ID FROM $TABLE_WATER_INTAKE 
            WHERE $COLUMN_USER_ID = ? AND DATE($COLUMN_CREATED_AT) = ?
        """.trimIndent()
        
        val cursor = db.rawQuery(checkQuery, arrayOf(userId.toString(), today))
        val exists = cursor.moveToFirst()
        val existingId = if (exists) cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)) else -1
        cursor.close()
        
        return if (exists) {
            // Update existing record
            val values = ContentValues().apply {
                put(COLUMN_GLASSES, glasses)
                put(COLUMN_UPDATED_AT, getCurrentDateTime())
            }
            db.update(TABLE_WATER_INTAKE, values, "$COLUMN_ID = ?", arrayOf(existingId.toString()))
            existingId
        } else {
            // Insert new record
            val values = ContentValues().apply {
                put(COLUMN_USER_ID, userId)
                put(COLUMN_GLASSES, glasses)
                put(COLUMN_CREATED_AT, getCurrentDateTime())
                put(COLUMN_UPDATED_AT, getCurrentDateTime())
            }
            db.insert(TABLE_WATER_INTAKE, null, values)
        }
    }

    fun getTodayWaterIntake(userId: Long): Int {
        val db = readableDatabase
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val query = """
            SELECT $COLUMN_GLASSES FROM $TABLE_WATER_INTAKE 
            WHERE $COLUMN_USER_ID = ? AND DATE($COLUMN_CREATED_AT) = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString(), today))
        val glasses = if (cursor.moveToFirst()) {
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GLASSES))
        } else {
            0
        }
        cursor.close()
        return glasses
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
