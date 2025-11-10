<?php
/**
 * Database Configuration for BeatWell
 */

// Database configuration for XAMPP
define('DB_HOST', 'localhost');
define('DB_NAME', 'beatwell_db');
define('DB_USER', 'root');
define('DB_PASS', ''); // XAMPP default has no password
define('DB_CHARSET', 'utf8mb4');

/**
 * Get database connection
 * @return PDO|null
 */
function getDBConnection() {
    try {
        $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
        $pdo = new PDO($dsn, DB_USER, DB_PASS, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);
        return $pdo;
    } catch (PDOException $e) {
        error_log("Database connection failed: " . $e->getMessage());
        return null;
    }
}

/**
 * Initialize database tables
 */
function initializeDatabase() {
    $pdo = getDBConnection();
    if (!$pdo) {
        return false;
    }
    
    // Create users table
    $usersTable = "
        CREATE TABLE IF NOT EXISTS users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(50) UNIQUE NOT NULL,
            email VARCHAR(100) UNIQUE NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            first_name VARCHAR(50),
            last_name VARCHAR(50),
            phone VARCHAR(20),
            date_of_birth DATE,
            gender VARCHAR(20),
            address TEXT,
            city VARCHAR(50),
            state VARCHAR(50),
            zip_code VARCHAR(10),
            emergency_contact_name VARCHAR(100),
            emergency_contact_phone VARCHAR(20),
            medical_conditions TEXT,
            allergies TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        )
    ";
    
    // Create sessions table
    $sessionsTable = "
        CREATE TABLE IF NOT EXISTS sessions (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            session_token VARCHAR(255) UNIQUE NOT NULL,
            expires_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
    ";
    
    // Create meal_options table
    $mealOptionsTable = "
        CREATE TABLE IF NOT EXISTS meal_options (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            description TEXT,
            meal_type ENUM('breakfast', 'lunch', 'dinner') NOT NULL,
            calories INT NOT NULL DEFAULT 0,
            image_url VARCHAR(500),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_meal_type (meal_type)
        )
    ";
    
    // Create meal_logs table
    $mealLogsTable = "
        CREATE TABLE IF NOT EXISTS meal_logs (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            meal_type ENUM('breakfast', 'lunch', 'dinner') NOT NULL,
            meal_option_id INT,
            meal_option_name VARCHAR(255),
            meal_option_description TEXT,
            portion_size DECIMAL(3,2) NOT NULL DEFAULT 1.00,
            calories INT NOT NULL DEFAULT 0,
            is_custom BOOLEAN NOT NULL DEFAULT FALSE,
            logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            INDEX idx_user_date (user_id, logged_at),
            INDEX idx_meal_type (meal_type),
            INDEX idx_is_custom (is_custom)
        )
    ";
    
    // Create user_goals table
    $userGoalsTable = "
        CREATE TABLE IF NOT EXISTS user_goals (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL UNIQUE,
            daily_calorie_goal INT NOT NULL DEFAULT 2000,
            daily_water_goal INT NOT NULL DEFAULT 8,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
    ";
    
    // Create chat_messages table
    $chatMessagesTable = "
        CREATE TABLE IF NOT EXISTS chat_messages (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            message TEXT NOT NULL,
            sender_type ENUM('user', 'ai') NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            INDEX idx_user_created (user_id, created_at),
            INDEX idx_sender_type (sender_type)
        )
    ";
    
    // Create water_intake table
    $waterIntakeTable = "
        CREATE TABLE IF NOT EXISTS water_intake (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            glasses INT NOT NULL DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            INDEX idx_user_date (user_id, created_at)
        )
    ";
    
    try {
        $pdo->exec($usersTable);
        $pdo->exec($sessionsTable);
        $pdo->exec($mealOptionsTable);
        $pdo->exec($mealLogsTable);
        $pdo->exec($userGoalsTable);
        $pdo->exec($chatMessagesTable);
        $pdo->exec($waterIntakeTable);
        return true;
    } catch (PDOException $e) {
        error_log("Database initialization failed: " . $e->getMessage());
        return false;
    }
}
?>

