-- BeatWell Database Manual Setup
-- Run this in phpMyAdmin if web setup fails

USE beatwell_db;

-- Create users table
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
);

-- Create sessions table
CREATE TABLE IF NOT EXISTS sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create meal_options table
CREATE TABLE IF NOT EXISTS meal_options (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    meal_type ENUM('breakfast', 'lunch', 'dinner') NOT NULL,
    calories INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_meal_type (meal_type)
);

-- Create meal_logs table
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
);

-- Create user_goals table
CREATE TABLE IF NOT EXISTS user_goals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    daily_calorie_goal INT NOT NULL DEFAULT 2000,
    daily_water_goal INT NOT NULL DEFAULT 8,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create chat_messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    message TEXT NOT NULL,
    sender_type ENUM('user', 'ai') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_sender_type (sender_type)
);

-- Create water_intake table
CREATE TABLE IF NOT EXISTS water_intake (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    glasses INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, created_at)
);

-- Insert sample meal options
INSERT INTO meal_options (name, description, meal_type, calories, image_url) VALUES
-- Breakfast options
('Oats with fruits and nuts', 'Healthy oats with fresh fruits and mixed nuts', 'breakfast', 350, 'https://example.com/oats.jpg'),
('Scrambled eggs with toast', 'Protein-rich scrambled eggs with whole wheat toast', 'breakfast', 280, 'https://example.com/eggs.jpg'),
('Smoothie bowl', 'Nutritious smoothie bowl with berries and granola', 'breakfast', 320, 'https://example.com/smoothie.jpg'),
('Greek yogurt parfait', 'Greek yogurt with honey, granola, and fresh berries', 'breakfast', 250, 'https://example.com/yogurt.jpg'),

-- Lunch options
('Grilled chicken salad', 'Fresh mixed greens with grilled chicken breast', 'lunch', 420, 'https://example.com/salad.jpg'),
('Quinoa bowl', 'Quinoa with roasted vegetables and tahini dressing', 'lunch', 380, 'https://example.com/quinoa.jpg'),
('Lentil curry with rice', 'Spiced lentil curry served with brown rice', 'lunch', 450, 'https://example.com/lentil.jpg'),
('Vegetable stir-fry', 'Mixed vegetables stir-fried with tofu and brown rice', 'lunch', 320, 'https://example.com/stirfry.jpg'),

-- Dinner options
('Millet upma + light coconut chutney', 'Healthy millet upma with coconut chutney', 'dinner', 320, 'https://example.com/upma.jpg'),
('Ragi idiyappam + vegetable curry', 'Traditional ragi idiyappam with mixed vegetable curry', 'dinner', 380, 'https://example.com/idiyappam.jpg'),
('Ragi dosa with chutney', 'Nutritious ragi dosa served with coconut chutney', 'dinner', 280, 'https://example.com/dosa.jpg'),
('Idiyappam, light sambar', 'Soft idiyappam with light sambar', 'dinner', 300, 'https://example.com/idiyappam2.jpg'),
('Wheat khichdi', 'Comforting wheat khichdi with vegetables', 'dinner', 350, 'https://example.com/khichdi.jpg'),
('Idli (3 pcs) + sambar', 'Traditional idli served with sambar', 'dinner', 250, 'https://example.com/idli.jpg');

-- Verify setup
SELECT 'Database setup completed successfully!' as status;
SELECT COUNT(*) as meal_options_count FROM meal_options;
SELECT COUNT(*) as tables_created FROM information_schema.tables WHERE table_schema = 'beatwell_db';