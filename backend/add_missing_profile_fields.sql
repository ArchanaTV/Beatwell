-- =====================================================
-- BeatWell Database Enhancement Script
-- Adds missing profile fields to users table
-- =====================================================

-- Add missing profile fields to users table
ALTER TABLE users ADD COLUMN height DECIMAL(5,2) COMMENT "Height in cm";
ALTER TABLE users ADD COLUMN weight DECIMAL(5,2) COMMENT "Weight in kg";
ALTER TABLE users ADD COLUMN blood_pressure_systolic INT COMMENT "Systolic BP";
ALTER TABLE users ADD COLUMN blood_pressure_diastolic INT COMMENT "Diastolic BP";
ALTER TABLE users ADD COLUMN diabetes_type ENUM("none", "type1", "type2", "gestational") DEFAULT "none";
ALTER TABLE users ADD COLUMN treatment_type VARCHAR(255) COMMENT "Current treatment/medication";

-- Add indexes for better performance
CREATE INDEX idx_meal_logs_user_date ON meal_logs(user_id, logged_at);
CREATE INDEX idx_meal_logs_user_type ON meal_logs(user_id, meal_type);

-- Update Boss user with sample complete profile data
UPDATE users SET 
    height = 175.5,
    weight = 70.2,
    blood_pressure_systolic = 120,
    blood_pressure_diastolic = 80,
    diabetes_type = 'none',
    treatment_type = 'Regular exercise and balanced diet'
WHERE username = 'Boss';

-- Update testuser with sample data too
UPDATE users SET 
    height = 180.0,
    weight = 75.0,
    blood_pressure_systolic = 118,
    blood_pressure_diastolic = 78,
    diabetes_type = 'none',
    treatment_type = 'Preventive care'
WHERE username = 'testuser';

-- Add some more meal logs for Boss user for testing meal history
INSERT INTO meal_logs (user_id, meal_type, meal_option_id, meal_option_name, meal_option_description, portion_size, calories, is_custom, logged_at) VALUES
(14, 'breakfast', -1, 'Oatmeal with Fruits', 'Healthy breakfast with oats and mixed fruits', 1.0, 320, 1, '2025-10-26 08:30:00'),
(14, 'lunch', -1, 'Grilled Chicken Salad', 'Fresh salad with grilled chicken breast', 1.0, 450, 1, '2025-10-26 13:15:00'),
(14, 'dinner', -1, 'Fish Curry with Rice', 'Traditional fish curry with steamed rice', 1.0, 520, 1, '2025-10-26 19:45:00'),
(14, 'breakfast', -1, 'Scrambled Eggs', 'Protein-rich scrambled eggs with toast', 1.0, 280, 1, '2025-10-25 08:00:00'),
(14, 'lunch', -1, 'Vegetable Biryani', 'Aromatic vegetable biryani', 1.0, 480, 1, '2025-10-25 12:30:00');

-- Verify the changes
SELECT 'Users table structure after enhancement:' as info;
DESCRIBE users;

SELECT 'Boss user profile data:' as info;
SELECT id, username, first_name, height, weight, blood_pressure_systolic, blood_pressure_diastolic, diabetes_type, treatment_type FROM users WHERE username = 'Boss';

SELECT 'Boss user meal history:' as info;
SELECT meal_type, calories, logged_at FROM meal_logs WHERE user_id = 14 ORDER BY logged_at DESC;

SELECT 'Enhancement complete!' as status;