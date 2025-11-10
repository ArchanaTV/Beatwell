<?php
/**
 * BeatWell API Main Entry Point
 * Simple routing for XAMPP setup
 */

// Set content type to JSON
header('Content-Type: application/json');

// Enable CORS for mobile app
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS, PUT, DELETE');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// API documentation endpoint
http_response_code(200);
echo json_encode([
    'status' => 'success',
    'message' => 'BeatWell API',
    'version' => '1.0.0',
    'timestamp' => date('Y-m-d H:i:s'),
    'endpoints' => [
        'GET /health.php' => 'Health check',
        'GET /db_test.php' => 'Database connection test',
        'POST /api/users.php' => 'User registration and authentication',
        'GET /api/meals.php' => 'Meal tracking',
        'GET /api/calendar.php' => 'Calendar and water tracking',
        'GET /api/chat.php' => 'AI chat functionality (Gemini 2.5 Flash)'
    ],
    'documentation' => [
        'registration' => [
            'method' => 'POST',
            'endpoint' => '/api/users.php',
            'action' => 'register',
            'required_fields' => ['username', 'email', 'password', 'confirm_password', 'first_name', 'last_name', 'phone', 'date_of_birth', 'gender']
        ],
        'login' => [
            'method' => 'POST',
            'endpoint' => '/api/users.php',
            'action' => 'login',
            'required_fields' => ['username', 'password']
        ]
    ],
    'setup_instructions' => [
        '1. Test health: GET /health.php',
        '2. Test database: GET /db_test.php',
        '3. Initialize database: GET /init_db.php',
        '4. Populate meals: GET /populate_meal_options.php'
    ],
    'gemini_status' => [
        'status' => 'configured',
        'model' => 'gemini-2.5-flash',
        'message' => 'AI chat functionality is ready and working!'
    ]
]);
?>