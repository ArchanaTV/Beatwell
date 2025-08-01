<?php
header("Content-Type: application/json");

// Include database connection
require 'dbcon.php';

// Get raw POST data and decode JSON
$data = json_decode(file_get_contents("php://input"), true);

// Validate required fields
if (
    isset($data['username'], $data['password'], $data['email'], $data['gender']) &&
    !empty($data['username']) && !empty($data['password']) && !empty($data['email'])
) {
    // Escape and assign inputs
    $username       = $conn->real_escape_string($data['username']);
    $password       = password_hash($data['password'], PASSWORD_DEFAULT); // hash password
    $email          = $conn->real_escape_string($data['email']);
    $gender         = $conn->real_escape_string($data['gender']);
    $height_cm      = isset($data['height']) ? floatval($data['height']) : null;
    $weight_kg      = isset($data['weight']) ? floatval($data['weight']) : null;
    $age            = isset($data['age']) ? intval($data['age']) : null;
    $blood_pressure = $conn->real_escape_string($data['blood_pressure'] ?? '');
    $diabetes       = $conn->real_escape_string($data['diabetes'] ?? '');
    $treatment_type = $conn->real_escape_string($data['treatment_type'] ?? '');

    // SQL Insert query
    $sql = "INSERT INTO users (username, password, email, gender, height_cm, weight_kg, age, blood_pressure, diabetes, treatment_type)
            VALUES ('$username', '$password', '$email', '$gender', '$height_cm', '$weight_kg', '$age', '$blood_pressure', '$diabetes', '$treatment_type')";

    if ($conn->query($sql) === TRUE) {
        echo json_encode(["status" => "success", "message" => "Signup successful."]);
    } else {
        echo json_encode(["status" => "error", "message" => "Database error: " . $conn->error]);
    }

} else {
    echo json_encode(["status" => "error", "message" => "Required fields are missing."]);
}

$conn->close();
?>
