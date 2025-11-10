<?php
/**
 * Chat API endpoints for BeatWell
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/functions.php';
require_once __DIR__ . '/../includes/gemini_service.php';

// Set content type to JSON
header('Content-Type: application/json');

// Handle CORS
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Get request method and data
$method = $_SERVER['REQUEST_METHOD'];
$data = getRequestData();

// Route the request
try {
    switch ($method) {
        case 'GET':
            $action = $_GET['action'] ?? '';
            if ($action === 'messages') {
                getChatMessages();
            } else {
                // Default GET response - API documentation
                sendSuccess([
                    'api' => 'BeatWell Chat API',
                    'version' => '1.0.0',
                    'endpoints' => [
                        'POST' => 'Send chat message',
                        'GET with action=messages' => 'Get chat history'
                    ],
                    'timestamp' => date('Y-m-d H:i:s')
                ], 'Chat API is working');
            }
            break;
            
        case 'POST':
            // Default POST action is to send chat message
            sendChatMessage();
            break;
            
        default:
            sendError('Method not allowed', 405);
            break;
    }
} catch (Exception $e) {
    logError('Chat API error: ' . $e->getMessage());
    sendError('Internal server error', 500);
}

/**
 * Get chat messages for a user
 */
function getChatMessages() {
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    // Get user ID from session or token
    $userId = getCurrentUserId();
    if (!$userId) {
        sendError('Authentication required', 401);
    }
    
    try {
        $stmt = $pdo->prepare("
            SELECT id, message, sender_type, created_at 
            FROM chat_messages 
            WHERE user_id = ? 
            ORDER BY created_at ASC
        ");
        $stmt->execute([$userId]);
        $messages = $stmt->fetchAll();
        
        sendSuccess($messages, 'Messages retrieved successfully');
    } catch (PDOException $e) {
        logError('Failed to get chat messages: ' . $e->getMessage());
        sendError('Failed to retrieve messages', 500);
    }
}

/**
 * Send a chat message
 */
function sendChatMessage() {
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    // Get user ID from session or token
    $userId = getCurrentUserId();
    if (!$userId) {
        sendError('Authentication required', 401);
    }
    
    $data = getRequestData();
    $message = sanitizeInput($data['message'] ?? '');
    $senderType = sanitizeInput($data['sender_type'] ?? 'user');
    
    if (empty($message)) {
        sendError('Message is required', 400);
    }
    
    if (!in_array($senderType, ['user', 'ai'])) {
        sendError('Invalid sender type', 400);
    }
    
    try {
        // Insert user message
        $stmt = $pdo->prepare("
            INSERT INTO chat_messages (user_id, message, sender_type, created_at) 
            VALUES (?, ?, ?, NOW())
        ");
        $stmt->execute([$userId, $message, $senderType]);
        $messageId = $pdo->lastInsertId();
        
        // If it's a user message, generate AI response using Gemini
        if ($senderType === 'user') {
            try {
                // Get user context for personalized responses
                $userContext = getUserContext($userId, $pdo);
                
                // Initialize Gemini service
                $geminiService = new GeminiService();
                
                // Generate AI response
                $aiResponse = $geminiService->generateNutritionResponse($message, $userContext);
                
                // Insert AI response
                $stmt = $pdo->prepare("
                    INSERT INTO chat_messages (user_id, message, sender_type, created_at) 
                    VALUES (?, ?, 'ai', NOW())
                ");
                $stmt->execute([$userId, $aiResponse]);
                
            } catch (Exception $e) {
                logError('Gemini API failed, using fallback: ' . $e->getMessage());
                
                // Fallback to basic response if Gemini fails
                $aiResponse = generateAIResponse($message);
                
                $stmt = $pdo->prepare("
                    INSERT INTO chat_messages (user_id, message, sender_type, created_at) 
                    VALUES (?, ?, 'ai', NOW())
                ");
                $stmt->execute([$userId, $aiResponse]);
            }
        }
        
        sendSuccess(['message_id' => $messageId], 'Message sent successfully');
    } catch (PDOException $e) {
        logError('Failed to send chat message: ' . $e->getMessage());
        sendError('Failed to send message', 500);
    }
}

/**
 * Generate AI response based on user message
 */
function generateAIResponse($userMessage) {
    $lowerMessage = strtolower($userMessage);
    
    if (strpos($lowerMessage, 'pizza') !== false || strpos($lowerMessage, 'cauliflower') !== false) {
        return "Great choice! Cauliflower-crust pizza is a healthier alternative. Here's the nutrition breakdown:\n\n• Nutrition: 280 kcal, 8g fat (2g saturated), 6g fiber\n• Benefits: Lower carbs, more fiber, gluten-free option";
    }
    
    if (strpos($lowerMessage, 'calories') !== false || strpos($lowerMessage, 'calorie') !== false) {
        return "I can help you track calories! A balanced approach is key:\n\n• Focus on nutrient-dense foods\n• Include lean proteins, whole grains, and vegetables\n• Stay hydrated and listen to your body's hunger cues";
    }
    
    if (strpos($lowerMessage, 'healthy') !== false || strpos($lowerMessage, 'diet') !== false) {
        return "A healthy diet includes variety and balance:\n\n• Fill half your plate with vegetables\n• Choose whole grains over refined\n• Include lean proteins and healthy fats\n• Stay hydrated throughout the day";
    }
    
    if (strpos($lowerMessage, 'weight') !== false || strpos($lowerMessage, 'lose') !== false) {
        return "Sustainable weight management focuses on:\n\n• Creating a moderate calorie deficit\n• Building healthy eating habits\n• Regular physical activity\n• Getting adequate sleep and managing stress";
    }
    
    return "I'm here to help with your nutrition questions! Feel free to ask about:\n\n• Meal planning and recipes\n• Nutritional information\n• Healthy eating tips\n• Dietary concerns or goals";
}

/**
 * Get current user ID from session or token
 */
function getCurrentUserId() {
    // Check for session token in request
    $sessionToken = $_GET['session_token'] ?? $_POST['session_token'] ?? null;
    
    if ($sessionToken) {
        $pdo = getDBConnection();
        if ($pdo) {
            try {
                $stmt = $pdo->prepare("
                    SELECT u.id 
                    FROM users u 
                    JOIN sessions s ON u.id = s.user_id 
                    WHERE s.session_token = ? AND s.expires_at > NOW()
                ");
                $stmt->execute([$sessionToken]);
                $user = $stmt->fetch();
                
                if ($user) {
                    return $user['id'];
                }
            } catch (PDOException $e) {
                logError('Session verification failed: ' . $e->getMessage());
            }
        }
    }
    
    // Fallback for development - return user ID 1
    return 1;
}

/**
 * Get user context for personalized AI responses
 * @param int $userId
 * @param PDO $pdo
 * @return array
 */
function getUserContext($userId, $pdo) {
    try {
        $stmt = $pdo->prepare("
            SELECT 
                first_name, last_name, date_of_birth, gender,
                height, weight, blood_pressure_systolic, blood_pressure_diastolic,
                diabetes_type, treatment_type, medical_conditions, allergies
            FROM users 
            WHERE id = ?
        ");
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
        
        if (!$user) {
            return [];
        }
        
        // Calculate age
        $age = 0;
        if ($user['date_of_birth']) {
            $birthDate = new DateTime($user['date_of_birth']);
            $today = new DateTime();
            $age = $today->diff($birthDate)->y;
        }
        
        return [
            'age' => $age,
            'gender' => $user['gender'],
            'height' => $user['height'] ? (float)$user['height'] : null,
            'weight' => $user['weight'] ? (float)$user['weight'] : null,
            'blood_pressure_systolic' => $user['blood_pressure_systolic'] ? (int)$user['blood_pressure_systolic'] : null,
            'blood_pressure_diastolic' => $user['blood_pressure_diastolic'] ? (int)$user['blood_pressure_diastolic'] : null,
            'diabetes_type' => $user['diabetes_type'] ?: 'none',
            'treatment_type' => $user['treatment_type'] ?: '',
            'medical_conditions' => $user['medical_conditions'] ?: '',
            'allergies' => $user['allergies'] ?: ''
        ];
        
    } catch (PDOException $e) {
        logError('Failed to get user context: ' . $e->getMessage());
        return [];
    }
}
?>
