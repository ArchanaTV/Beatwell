<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");

include "dbcon.php"; // Include DB connection

// Ensure it's a POST request
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed."
    ]);
    exit;
}

// Your SQL query
$sql = "SELECT s_no, date, meal_type, meal_status FROM user_meal WHERE 1";

$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $meals = [];
    while ($row = $result->fetch_assoc()) {
        $meals[] = $row;
    }
    echo json_encode([
        "success" => true,
        "data" => $meals
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "No records found."
    ]);
}

$conn->close();
?>
