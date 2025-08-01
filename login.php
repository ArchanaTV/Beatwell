<?php
// Include the DB connection file
include "dbcon.php";

// Set response type to JSON
header('Content-Type: application/json');

// Check if required fields are sent via POST
if (!isset($_POST['user_name']) || !isset($_POST['password'])) {
    echo json_encode([
        'success' => false,
        'message' => 'Username and password are required.'
    ]);
    exit();
}

// Get and sanitize inputs
$user_name = trim($_POST['user_name']);
$password = $_POST['password']; // raw password for bcrypt check

// Prepare and execute the SQL query securely using prepared statements
$sql = "SELECT user_name, password FROM auth WHERE user_name = ?";
$stmt = $conn->prepare($sql);

if (!$stmt) {
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $conn->error
    ]);
    exit();
}

$stmt->bind_param("s", $user_name);
$stmt->execute();
$result = $stmt->get_result();

// Check if user exists
if ($result->num_rows > 0) {
    $user = $result->fetch_assoc();

    // Verify password using bcrypt
    if (password_verify($password, $user['password'])) {
        echo json_encode([
            'success' => true,
            'message' => 'Login successful.',
            'data' => [
                'user_name' => $user['user_name']
            ]
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Invalid username or password.'
        ]);
    }
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid username or password.'
    ]);
}

// Clean up
$stmt->close();
$conn->close();
?>
