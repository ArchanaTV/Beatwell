<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once __DIR__ . '/../includes/functions.php';

// Get request method and path
$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$pathParts = explode('/', trim($path, '/'));

// Route the request
switch ($method) {
    case 'POST':
        if (isset($_POST['action']) && $_POST['action'] === 'save_water') {
            saveWaterIntake();
        } else {
            $input = json_decode(file_get_contents('php://input'), true);
            if (isset($input['action']) && $input['action'] === 'save_water') {
                saveWaterIntake();
            } else {
                http_response_code(404);
                echo json_encode(['error' => 'Endpoint not found']);
            }
        }
        break;
    
    case 'GET':
        if (isset($_GET['action']) && $_GET['action'] === 'month_meals') {
            getMealsForMonth();
        } elseif (isset($_GET['action']) && $_GET['action'] === 'get_date_meals') {
            getMealsForDate();
        } elseif (isset($_GET['action']) && $_GET['action'] === 'water_intake') {
            getWaterIntake();
        } else {
            http_response_code(404);
            echo json_encode(['error' => 'Endpoint not found']);
        }
        break;
    
    default:
        http_response_code(405);
        echo json_encode(['error' => 'Method not allowed']);
        break;
}

function saveWaterIntake() {
    try {
        // Get JSON input
        $input = json_decode(file_get_contents('php://input'), true);
        
        if (!$input) {
            $input = $_POST;
        }
        
        if (!$input) {
            http_response_code(400);
            echo json_encode(['error' => 'Invalid input']);
            return;
        }
        
        // Validate required fields
        $requiredFields = ['user_id', 'glasses'];
        foreach ($requiredFields as $field) {
            if (!isset($input[$field])) {
                http_response_code(400);
                echo json_encode(['error' => "Missing required field: $field"]);
                return;
            }
        }
        
        // Get database connection
        $pdo = getDatabaseConnection();
        
        // Check if water intake already exists for today
        $checkSql = "SELECT id FROM water_intake 
                     WHERE user_id = ? AND DATE(created_at) = CURDATE()";
        $checkStmt = $pdo->prepare($checkSql);
        $checkStmt->execute([$input['user_id']]);
        $existingRecord = $checkStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($existingRecord) {
            // Update existing record
            $sql = "UPDATE water_intake 
                    SET glasses = ?, updated_at = NOW() 
                    WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $result = $stmt->execute([$input['glasses'], $existingRecord['id']]);
        } else {
            // Insert new record
            $sql = "INSERT INTO water_intake (user_id, glasses, created_at, updated_at) 
                    VALUES (?, ?, NOW(), NOW())";
            $stmt = $pdo->prepare($sql);
            $result = $stmt->execute([$input['user_id'], $input['glasses']]);
        }
        
        if ($result) {
            http_response_code(201);
            echo json_encode([
                'success' => true,
                'message' => 'Water intake saved successfully',
                'data' => [
                    'user_id' => $input['user_id'],
                    'glasses' => $input['glasses'],
                    'date' => date('Y-m-d')
                ]
            ]);
        } else {
            http_response_code(500);
            echo json_encode(['error' => 'Failed to save water intake']);
        }
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function getMealsForMonth() {
    try {
        $userId = $_GET['user_id'] ?? null;
        $year = $_GET['year'] ?? null;
        $month = $_GET['month'] ?? null;
        
        if (!$userId || !$year || !$month) {
            http_response_code(400);
            echo json_encode(['error' => 'User ID, year, and month are required']);
            return;
        }
        
        $pdo = getDatabaseConnection();
        
        // Get meals for the specified month
        $sql = "SELECT 
                    id,
                    meal_type,
                    meal_option_name,
                    meal_option_description,
                    portion_size,
                    calories,
                    is_custom,
                    DATE(logged_at) as date,
                    logged_at
                FROM meal_logs 
                WHERE user_id = ? 
                AND YEAR(logged_at) = ? 
                AND MONTH(logged_at) = ?
                ORDER BY logged_at DESC";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$userId, $year, $month]);
        $meals = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Format the data for the calendar
        $formattedMeals = array_map(function($meal) {
            return [
                'id' => (int)$meal['id'],
                'meal_type' => $meal['meal_type'],
                'meal_option_name' => $meal['meal_option_name'],
                'meal_option_description' => $meal['meal_option_description'],
                'portion_size' => (float)$meal['portion_size'],
                'calories' => (int)$meal['calories'],
                'is_custom' => (bool)$meal['is_custom'],
                'date' => $meal['date'],
                'logged_at' => strtotime($meal['logged_at']) * 1000 // Convert to milliseconds for JavaScript
            ];
        }, $meals);
        
        echo json_encode([
            'success' => true,
            'meals' => $formattedMeals,
            'count' => count($formattedMeals)
        ]);
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function getMealsForDate() {
    try {
        $userId = $_GET['user_id'] ?? null;
        $date = $_GET['date'] ?? null;
        
        if (!$userId || !$date) {
            http_response_code(400);
            echo json_encode(['error' => 'User ID and date are required']);
            return;
        }
        
        $pdo = getDatabaseConnection();
        
        // Get meals for the specified date
        $sql = "SELECT 
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
                AND DATE(logged_at) = ?
                ORDER BY logged_at ASC";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$userId, $date]);
        $meals = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Format the data
        $formattedMeals = array_map(function($meal) {
            return [
                'id' => (int)$meal['id'],
                'meal_type' => $meal['meal_type'],
                'meal_option_name' => $meal['meal_option_name'],
                'meal_option_description' => $meal['meal_option_description'],
                'portion_size' => (float)$meal['portion_size'],
                'calories' => (int)$meal['calories'],
                'is_custom' => (bool)$meal['is_custom'],
                'logged_at' => strtotime($meal['logged_at']) * 1000 // Convert to milliseconds for JavaScript
            ];
        }, $meals);
        
        echo json_encode([
            'success' => true,
            'meals' => $formattedMeals,
            'count' => count($formattedMeals)
        ]);
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function getWaterIntake() {
    try {
        $userId = $_GET['user_id'] ?? null;
        $date = $_GET['date'] ?? date('Y-m-d');
        
        if (!$userId) {
            http_response_code(400);
            echo json_encode(['error' => 'User ID is required']);
            return;
        }
        
        $pdo = getDatabaseConnection();
        
        $sql = "SELECT glasses, created_at, updated_at 
                FROM water_intake 
                WHERE user_id = ? AND DATE(created_at) = ?";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$userId, $date]);
        $waterIntake = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($waterIntake) {
            echo json_encode([
                'success' => true,
                'data' => [
                    'glasses' => (int)$waterIntake['glasses'],
                    'date' => $date,
                    'created_at' => $waterIntake['created_at'],
                    'updated_at' => $waterIntake['updated_at']
                ]
            ]);
        } else {
            echo json_encode([
                'success' => true,
                'data' => [
                    'glasses' => 0,
                    'date' => $date,
                    'created_at' => null,
                    'updated_at' => null
                ]
            ]);
        }
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}
?>
