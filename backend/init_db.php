<?php
/**
 * BeatWell Database Initialization Script
 * Run this script to set up the database tables
 */

require_once 'config/database.php';

echo "BeatWell Database Initialization\n";
echo "================================\n\n";

// Test database connection
echo "Testing database connection...\n";
$pdo = getDBConnection();

if (!$pdo) {
    echo "❌ Database connection failed!\n";
    echo "Please check your database configuration in config/database.php\n";
    exit(1);
}

echo "✅ Database connection successful!\n\n";

// Initialize database tables
echo "Initializing database tables...\n";
if (initializeDatabase()) {
    echo "✅ Database tables created successfully!\n\n";
    
    // Display table information
    echo "Created tables:\n";
    echo "- users: Stores user account information\n";
    echo "- sessions: Manages user sessions\n";
    echo "- meal_options: Predefined meal options for tracking\n";
    echo "- meal_logs: User meal tracking logs\n\n";
    
    // Test the tables
    echo "Testing table creation...\n";
    try {
        $stmt = $pdo->query("SHOW TABLES");
        $tables = $stmt->fetchAll(PDO::FETCH_COLUMN);
        
        if (in_array('users', $tables) && in_array('sessions', $tables) && 
            in_array('meal_options', $tables) && in_array('meal_logs', $tables)) {
            echo "✅ All tables verified!\n\n";
            
            // Show table structure
            echo "Users table structure:\n";
            $stmt = $pdo->query("DESCRIBE users");
            $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($columns as $column) {
                echo "  - {$column['Field']}: {$column['Type']}\n";
            }
            
            echo "\nSessions table structure:\n";
            $stmt = $pdo->query("DESCRIBE sessions");
            $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($columns as $column) {
                echo "  - {$column['Field']}: {$column['Type']}\n";
            }
            
        } else {
            echo "❌ Some tables are missing!\n";
        }
        
    } catch (PDOException $e) {
        echo "❌ Error testing tables: " . $e->getMessage() . "\n";
    }
    
} else {
    echo "❌ Database initialization failed!\n";
    echo "Check the error logs for more details.\n";
    exit(1);
}

echo "\n🎉 Database initialization completed successfully!\n";
echo "Your BeatWell backend is ready to use.\n";
echo "\nAPI Endpoints:\n";
echo "- POST /api/users (register, login, logout)\n";
echo "- GET /api/users (verify session)\n";
echo "- POST /api/meals/save (save meal data)\n";
echo "- GET /api/meals/history (get meal history)\n";
echo "- GET /api/meals/today (get today's meals)\n";
echo "- GET /api/meals (get meal options)\n";
echo "- GET /api/health (health check)\n";
echo "\nFor detailed API documentation, visit: http://your-domain/backend/\n";
?>
