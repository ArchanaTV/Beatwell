<?php
/**
 * Common functions for BeatWell API
 */

/**
 * Send JSON response
 * @param array $data
 * @param int $statusCode
 */
function sendResponse($data, $statusCode = 200) {
    http_response_code($statusCode);
    echo json_encode($data);
    exit();
}

/**
 * Send error response
 * @param string $message
 * @param int $statusCode
 */
function sendError($message, $statusCode = 400) {
    sendResponse([
        'status' => 'error',
        'message' => $message
    ], $statusCode);
}

/**
 * Send success response
 * @param array $data
 * @param string $message
 */
function sendSuccess($data = [], $message = 'Success') {
    sendResponse([
        'status' => 'success',
        'message' => $message,
        'data' => $data
    ]);
}

/**
 * Validate email format
 * @param string $email
 * @return bool
 */
function isValidEmail($email) {
    return filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
}

/**
 * Hash password
 * @param string $password
 * @return string
 */
function hashPassword($password) {
    return password_hash($password, PASSWORD_DEFAULT);
}

/**
 * Verify password
 * @param string $password
 * @param string $hash
 * @return bool
 */
function verifyPassword($password, $hash) {
    return password_verify($password, $hash);
}

/**
 * Generate random token
 * @param int $length
 * @return string
 */
function generateToken($length = 32) {
    return bin2hex(random_bytes($length));
}

/**
 * Get request data
 * @return array
 */
function getRequestData() {
    $input = file_get_contents('php://input');
    return json_decode($input, true) ?? [];
}

/**
 * Sanitize input
 * @param string $input
 * @return string
 */
function sanitizeInput($input) {
    return htmlspecialchars(strip_tags(trim($input)));
}

/**
 * Log error
 * @param string $message
 * @param array $context
 */
function logError($message, $context = []) {
    $logMessage = date('Y-m-d H:i:s') . ' - ' . $message;
    if (!empty($context)) {
        $logMessage .= ' - Context: ' . json_encode($context);
    }
    error_log($logMessage);
}

/**
 * Get database connection
 * @return PDO
 */
function getDatabaseConnection() {
    require_once __DIR__ . '/../config/database.php';
    return getDBConnection();
}
?>

