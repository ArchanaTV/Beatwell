<?php
/**
 * BeatWell Home API
 * Handles home screen data, meal tracking, and progress statistics
 */

require_once '../config/database.php';
require_once '../includes/functions.php';

// Set content type to JSON
header('Content-Type: application/json');

// Enable CORS for mobile app
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// Initialize database
if (!initializeDatabase()) {
    sendError('Database initialization failed', 500);
}

// Get request method and data
$method = $_SERVER['REQUEST_METHOD'];
$data = getRequestData();

// Route requests
switch ($method) {
    case 'POST':
        $action = $data['action'] ?? '';
        switch ($action) {
            case 'track_meal':
                handleTrackMeal($data);
                break;
            case 'update_progress':
                handleUpdateProgress($data);
                break;
            default:
                sendError('Invalid action', 400);
        }
        break;
    case 'GET':
        $action = $_GET['action'] ?? '';
        switch ($action) {
            case 'dashboard_data':
                handleGetDashboardData($_GET);
                break;
            case 'progress_stats':
                handleGetProgressStats($_GET);
                break;
            case 'meal_history':
                handleGetMealHistory($_GET);
                break;
            default:
                sendError('Invalid action', 400);
        }
        break;
    default:
        sendError('Method not allowed', 405);
}

/**
 * Handle getting dashboard data
 * @param array $data
 */
function handleGetDashboardData($data) {
    if (empty($data['session_token'])) {
        sendError('Session token is required', 400);
    }
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        // Verify session
        $stmt = $pdo->prepare("
            SELECT u.id, u.username, u.first_name, u.last_name, s.expires_at 
            FROM users u 
            JOIN sessions s ON u.id = s.user_id 
            WHERE s.session_token = ? AND s.expires_at > NOW()
        ");
        $stmt->execute([$data['session_token']]);
        $session = $stmt->fetch();
        
        if (!$session) {
            sendError('Invalid or expired session', 401);
        }
        
        $userId = $session['id'];
        
        // Get today's meal data
        $today = date('Y-m-d');
        
        // Get meal completion status
        $stmt = $pdo->prepare("
            SELECT meal_type, COUNT(*) as count 
            FROM meal_logs 
            WHERE user_id = ? AND DATE(logged_at) = ? 
            GROUP BY meal_type
        ");
        $stmt->execute([$userId, $today]);
        $mealData = $stmt->fetchAll();
        
        $mealStatus = [
            'breakfast' => false,
            'lunch' => false,
            'dinner' => false
        ];
        
        foreach ($mealData as $meal) {
            $mealStatus[$meal['meal_type']] = $meal['count'] > 0;
        }
        
        // Get today's progress stats
        $stmt = $pdo->prepare("
            SELECT 
                COALESCE(SUM(calories), 0) as total_calories,
                COUNT(DISTINCT meal_type) as meals_completed
            FROM meal_logs 
            WHERE user_id = ? AND DATE(logged_at) = ?
        ");
        $stmt->execute([$userId, $today]);
        $progressData = $stmt->fetch();
        
        // Get today's water intake
        $stmt = $pdo->prepare("
            SELECT COALESCE(glasses, 0) as total_water
            FROM water_intake 
            WHERE user_id = ? AND DATE(created_at) = ?
        ");
        $stmt->execute([$userId, $today]);
        $waterData = $stmt->fetch();
        $totalWater = $waterData ? $waterData['total_water'] : 0;
        
        // Get user's daily goals
        $stmt = $pdo->prepare("
            SELECT daily_calorie_goal, daily_water_goal 
            FROM user_goals 
            WHERE user_id = ?
        ");
        $stmt->execute([$userId]);
        $goals = $stmt->fetch();
        
        $dailyCalorieGoal = $goals['daily_calorie_goal'] ?? 2000;
        $dailyWaterGoal = $goals['daily_water_goal'] ?? 8;
        
        sendSuccess([
            'user' => [
                'id' => $session['id'],
                'username' => $session['username'],
                'first_name' => $session['first_name'],
                'last_name' => $session['last_name']
            ],
            'meal_status' => $mealStatus,
            'progress' => [
                'calories_consumed' => (int)$progressData['total_calories'],
                'calories_goal' => $dailyCalorieGoal,
                'water_intake' => (int)$totalWater,
                'water_goal' => $dailyWaterGoal,
                'meals_completed' => (int)$progressData['meals_completed'],
                'meals_total' => 3
            ],
            'date' => $today
        ], 'Dashboard data retrieved successfully');
        
    } catch (PDOException $e) {
        logError("Failed to get dashboard data", ['error' => $e->getMessage(), 'user_id' => $userId ?? null]);
        sendError('Failed to retrieve dashboard data', 500);
    }
}

/**
 * Handle getting progress statistics
 * @param array $data
 */
function handleGetProgressStats($data) {
    if (empty($data['session_token'])) {
        sendError('Session token is required', 400);
    }
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        // Verify session
        $stmt = $pdo->prepare("
            SELECT u.id FROM users u 
            JOIN sessions s ON u.id = s.user_id 
            WHERE s.session_token = ? AND s.expires_at > NOW()
        ");
        $stmt->execute([$data['session_token']]);
        $session = $stmt->fetch();
        
        if (!$session) {
            sendError('Invalid or expired session', 401);
        }
        
        $userId = $session['id'];
        $days = min(30, max(1, (int)($data['days'] ?? 7))); // Default 7 days, max 30
        
        // Get progress data for the last N days
        $stmt = $pdo->prepare("
            SELECT 
                DATE(logged_at) as date,
                SUM(calories) as daily_calories,
                0 as daily_water,
                COUNT(DISTINCT meal_type) as meals_completed
            FROM meal_logs 
            WHERE user_id = ? AND logged_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
            GROUP BY DATE(logged_at)
            ORDER BY date DESC
        ");
        $stmt->execute([$userId, $days]);
        $progressData = $stmt->fetchAll();
        
        // Calculate averages
        $totalDays = count($progressData);
        $avgCalories = $totalDays > 0 ? array_sum(array_column($progressData, 'daily_calories')) / $totalDays : 0;
        $avgWater = $totalDays > 0 ? array_sum(array_column($progressData, 'daily_water')) / $totalDays : 0;
        $avgMeals = $totalDays > 0 ? array_sum(array_column($progressData, 'meals_completed')) / $totalDays : 0;
        
        sendSuccess([
            'period_days' => $days,
            'total_days_tracked' => $totalDays,
            'averages' => [
                'calories' => round($avgCalories),
                'water' => round($avgWater, 1),
                'meals' => round($avgMeals, 1)
            ],
            'daily_data' => $progressData
        ], 'Progress statistics retrieved successfully');
        
    } catch (PDOException $e) {
        logError("Failed to get progress stats", ['error' => $e->getMessage(), 'user_id' => $userId ?? null]);
        sendError('Failed to retrieve progress statistics', 500);
    }
}

/**
 * Handle meal tracking
 * @param array $data
 */
function handleTrackMeal($data) {
    // Validate required fields
    $requiredFields = ['session_token', 'meal_type', 'calories'];
    foreach ($requiredFields as $field) {
        if (empty($data[$field])) {
            sendError("Field '$field' is required", 400);
        }
    }
    
    $sessionToken = sanitizeInput($data['session_token']);
    $mealType = sanitizeInput($data['meal_type']);
    $calories = (int)$data['calories'];
    $waterIntake = (int)($data['water_intake'] ?? 0);
    $notes = sanitizeInput($data['notes'] ?? '');
    
    // Validate meal type
    $validMealTypes = ['breakfast', 'lunch', 'dinner'];
    if (!in_array($mealType, $validMealTypes)) {
        sendError('Invalid meal type. Must be breakfast, lunch, or dinner', 400);
    }
    
    // Validate calories
    if ($calories < 0 || $calories > 5000) {
        sendError('Calories must be between 0 and 5000', 400);
    }
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        // Verify session
        $stmt = $pdo->prepare("
            SELECT u.id FROM users u 
            JOIN sessions s ON u.id = s.user_id 
            WHERE s.session_token = ? AND s.expires_at > NOW()
        ");
        $stmt->execute([$sessionToken]);
        $session = $stmt->fetch();
        
        if (!$session) {
            sendError('Invalid or expired session', 401);
        }
        
        $userId = $session['id'];
        
        // Insert meal tracking record
        $stmt = $pdo->prepare("
            INSERT INTO meal_logs (user_id, meal_type, meal_option_id, meal_option_name, meal_option_description, portion_size, calories, is_custom, logged_at) 
            VALUES (?, ?, -1, 'Custom Entry', ?, 1.0, ?, 1, NOW())
        ");
        $stmt->execute([$userId, $mealType, $notes, $calories]);
        
        $trackingId = $pdo->lastInsertId();
        
        // Log successful meal tracking
        logError("Meal tracked successfully", [
            'tracking_id' => $trackingId,
            'user_id' => $userId,
            'meal_type' => $mealType,
            'calories' => $calories
        ]);
        
        sendSuccess([
            'tracking_id' => $trackingId,
            'meal_type' => $mealType,
            'calories' => $calories,
            'water_intake' => $waterIntake,
            'timestamp' => date('Y-m-d H:i:s')
        ], 'Meal tracked successfully');
        
    } catch (PDOException $e) {
        logError("Meal tracking failed", ['error' => $e->getMessage(), 'user_id' => $userId ?? null]);
        sendError('Failed to track meal', 500);
    }
}

/**
 * Handle getting meal history
 * @param array $data
 */
function handleGetMealHistory($data) {
    if (empty($data['session_token'])) {
        sendError('Session token is required', 400);
    }
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        // Verify session
        $stmt = $pdo->prepare("
            SELECT u.id FROM users u 
            JOIN sessions s ON u.id = s.user_id 
            WHERE s.session_token = ? AND s.expires_at > NOW()
        ");
        $stmt->execute([$data['session_token']]);
        $session = $stmt->fetch();
        
        if (!$session) {
            sendError('Invalid or expired session', 401);
        }
        
        $userId = $session['id'];
        $limit = min(50, max(1, (int)($data['limit'] ?? 20))); // Default 20, max 50
        $offset = max(0, (int)($data['offset'] ?? 0));
        
        // Get meal history with enhanced data
        $stmt = $pdo->prepare("
            SELECT 
                id,
                meal_type,
                meal_option_name,
                meal_option_description,
                portion_size,
                calories,
                is_custom,
                logged_at
            FROM meal_logs 
            WHERE user_id = ?
            ORDER BY logged_at DESC
            LIMIT ? OFFSET ?
        ");
        $stmt->execute([$userId, $limit, $offset]);
        $mealHistory = $stmt->fetchAll();
        
        // Format meal history for mobile app
        $formattedMeals = array_map(function($meal) {
            return [
                'id' => (int)$meal['id'],
                'meal_type' => $meal['meal_type'],
                'meal_name' => $meal['meal_option_name'] ?: 'Custom Meal',
                'description' => $meal['meal_option_description'] ?: '',
                'portion_size' => (float)$meal['portion_size'],
                'calories' => (int)$meal['calories'],
                'is_custom' => (bool)$meal['is_custom'],
                'logged_at' => $meal['logged_at'],
                'date' => date('Y-m-d', strtotime($meal['logged_at'])),
                'time' => date('H:i', strtotime($meal['logged_at']))
            ];
        }, $mealHistory);
        
        // Get total count for pagination
        $stmt = $pdo->prepare("SELECT COUNT(*) as total FROM meal_logs WHERE user_id = ?");
        $stmt->execute([$userId]);
        $totalCount = $stmt->fetch()['total'];
        
        sendSuccess([
            'meals' => $formattedMeals,
            'pagination' => [
                'total' => (int)$totalCount,
                'limit' => $limit,
                'offset' => $offset,
                'has_more' => ($offset + $limit) < $totalCount
            ]
        ], 'Meal history retrieved successfully');
        
    } catch (PDOException $e) {
        logError("Failed to get meal history", ['error' => $e->getMessage(), 'user_id' => $userId ?? null]);
        sendError('Failed to retrieve meal history', 500);
    }
}

/**
 * Handle updating progress goals
 * @param array $data
 */
function handleUpdateProgress($data) {
    if (empty($data['session_token'])) {
        sendError('Session token is required', 400);
    }
    
    $sessionToken = sanitizeInput($data['session_token']);
    $dailyCalorieGoal = (int)($data['daily_calorie_goal'] ?? 0);
    $dailyWaterGoal = (int)($data['daily_water_goal'] ?? 0);
    
    // Validate goals
    if ($dailyCalorieGoal < 500 || $dailyCalorieGoal > 5000) {
        sendError('Daily calorie goal must be between 500 and 5000', 400);
    }
    
    if ($dailyWaterGoal < 1 || $dailyWaterGoal > 20) {
        sendError('Daily water goal must be between 1 and 20 glasses', 400);
    }
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        // Verify session
        $stmt = $pdo->prepare("
            SELECT u.id FROM users u 
            JOIN sessions s ON u.id = s.user_id 
            WHERE s.session_token = ? AND s.expires_at > NOW()
        ");
        $stmt->execute([$sessionToken]);
        $session = $stmt->fetch();
        
        if (!$session) {
            sendError('Invalid or expired session', 401);
        }
        
        $userId = $session['id'];
        
        // Update or insert user goals
        $stmt = $pdo->prepare("
            INSERT INTO user_goals (user_id, daily_calorie_goal, daily_water_goal, updated_at) 
            VALUES (?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE 
            daily_calorie_goal = VALUES(daily_calorie_goal),
            daily_water_goal = VALUES(daily_water_goal),
            updated_at = NOW()
        ");
        $stmt->execute([$userId, $dailyCalorieGoal, $dailyWaterGoal]);
        
        sendSuccess([
            'daily_calorie_goal' => $dailyCalorieGoal,
            'daily_water_goal' => $dailyWaterGoal,
            'updated_at' => date('Y-m-d H:i:s')
        ], 'Progress goals updated successfully');
        
    } catch (PDOException $e) {
        logError("Failed to update progress goals", ['error' => $e->getMessage(), 'user_id' => $userId ?? null]);
        sendError('Failed to update progress goals', 500);
    }
}
?>
