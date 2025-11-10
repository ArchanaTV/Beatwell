<?php
/**
 * Simple health check endpoint
 */

// Set content type to JSON
header('Content-Type: application/json');

// Enable CORS for mobile app
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Health check response
http_response_code(200);
echo json_encode([
    'status' => 'success',
    'message' => 'BeatWell API is running',
    'timestamp' => date('Y-m-d H:i:s'),
    'version' => '1.0.0',
    'php_version' => phpversion(),
    'server' => $_SERVER['SERVER_SOFTWARE'] ?? 'Unknown'
]);
?>