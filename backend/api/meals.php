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
        // Check if URL contains 'save' or if action parameter is 'save'
        if ((isset($pathParts[3]) && $pathParts[3] === 'save') || 
            (strpos($path, '/save') !== false) ||
            (isset($_POST['action']) && $_POST['action'] === 'save')) {
            saveMealData();
        } else {
            // Default POST action is to save meal data
            saveMealData();
        }
        break;
    
    case 'GET':
        if ((isset($pathParts[3]) && $pathParts[3] === 'history') || 
            (strpos($path, '/history') !== false)) {
            getMealHistory();
        } elseif ((isset($pathParts[3]) && $pathParts[3] === 'today') || 
                  (strpos($path, '/today') !== false)) {
            getTodayMeals();
        } else {
            getMealOptions();
        }
        break;
    
    default:
        http_response_code(405);
        echo json_encode(['error' => 'Method not allowed']);
        break;
}

function saveMealData() {
    try {
        // Get JSON input
        $input = json_decode(file_get_contents('php://input'), true);
        
        if (!$input) {
            http_response_code(400);
            echo json_encode(['error' => 'Invalid JSON input']);
            return;
        }
        
        // Validate required fields
        $requiredFields = ['user_id', 'meal_type', 'portion_size', 'calories'];
        foreach ($requiredFields as $field) {
            if (!isset($input[$field])) {
                http_response_code(400);
                echo json_encode(['error' => "Missing required field: $field"]);
                return;
            }
        }
        
        // Check if it's a custom food
        $isCustom = $input['is_custom'] ?? false;
        $mealOption = $input['meal_option'] ?? null;
        
        if (!$isCustom && !$mealOption) {
            http_response_code(400);
            echo json_encode(['error' => "Missing meal_option for predefined food"]);
            return;
        }
        
        // Get database connection
        $pdo = getDatabaseConnection();
        
        // Prepare SQL statement
        $sql = "INSERT INTO meal_logs (user_id, meal_type, meal_option_id, meal_option_name, 
                meal_option_description, portion_size, calories, is_custom, logged_at) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        
        $stmt = $pdo->prepare($sql);
        
        // Execute the statement
        if ($isCustom) {
            $result = $stmt->execute([
                $input['user_id'],
                $input['meal_type'],
                -1, // Custom food ID
                $mealOption['name'],
                $mealOption['description'],
                $input['portion_size'],
                $input['calories'],
                1 // is_custom = true
            ]);
        } else {
            $result = $stmt->execute([
                $input['user_id'],
                $input['meal_type'],
                $mealOption['id'],
                $mealOption['name'],
                $mealOption['description'],
                $input['portion_size'],
                $input['calories'],
                0 // is_custom = false
            ]);
        }
        
        if ($result) {
            // Get the inserted record
            $mealLogId = $pdo->lastInsertId();
            
            // Fetch the complete record
            $selectSql = "SELECT * FROM meal_logs WHERE id = ?";
            $selectStmt = $pdo->prepare($selectSql);
            $selectStmt->execute([$mealLogId]);
            $mealLog = $selectStmt->fetch(PDO::FETCH_ASSOC);
            
            http_response_code(201);
            echo json_encode([
                'success' => true,
                'message' => 'Meal logged successfully',
                'data' => $mealLog
            ]);
        } else {
            http_response_code(500);
            echo json_encode(['error' => 'Failed to save meal data']);
        }
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function getMealHistory() {
    try {
        $userId = $_GET['user_id'] ?? null;
        $limit = $_GET['limit'] ?? 50;
        $offset = $_GET['offset'] ?? 0;
        
        if (!$userId) {
            http_response_code(400);
            echo json_encode(['error' => 'User ID is required']);
            return;
        }
        
        $pdo = getDatabaseConnection();
        
        $sql = "SELECT * FROM meal_logs 
                WHERE user_id = ? 
                ORDER BY logged_at DESC 
                LIMIT ? OFFSET ?";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$userId, $limit, $offset]);
        $meals = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        echo json_encode([
            'success' => true,
            'data' => $meals,
            'count' => count($meals)
        ]);
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function getTodayMeals() {
    try {
        $userId = $_GET['user_id'] ?? null;
        
        if (!$userId) {
            http_response_code(400);
            echo json_encode(['error' => 'User ID is required']);
            return;
        }
        
        $pdo = getDatabaseConnection();
        
        $sql = "SELECT * FROM meal_logs 
                WHERE user_id = ? 
                AND DATE(logged_at) = CURDATE()
                ORDER BY logged_at DESC";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$userId]);
        $meals = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Calculate daily totals
        $totalCalories = array_sum(array_column($meals, 'calories'));
        $mealsByType = [];
        foreach ($meals as $meal) {
            $mealsByType[$meal['meal_type']][] = $meal;
        }
        
        echo json_encode([
            'success' => true,
            'data' => [
                'meals' => $meals,
                'summary' => [
                    'total_calories' => $totalCalories,
                    'meals_count' => count($meals),
                    'breakfast' => $mealsByType['breakfast'] ?? [],
                    'lunch' => $mealsByType['lunch'] ?? [],
                    'dinner' => $mealsByType['dinner'] ?? []
                ]
            ]
        ]);
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function getMealOptions() {
    try {
        $mealType = $_GET['type'] ?? null;
        
        $pdo = getDatabaseConnection();
        
        $sql = "SELECT * FROM meal_options";
        $params = [];
        
        if ($mealType) {
            $sql .= " WHERE meal_type = ?";
            $params[] = $mealType;
        }
        
        $sql .= " ORDER BY meal_type, name";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $options = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        echo json_encode([
            'success' => true,
            'data' => $options
        ]);
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}
?>
