<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");

include "dbcon.php"; // Include DB connection

// Allow only POST method
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed."
    ]);
    exit;
}

// SQL query
$sql = "SELECT s_no, surgery_type, date, breakfast, lunch, snacks, dinner, 
               breakfast_description, lunch_description, snacks_description, dinner_description 
        FROM meal_recomms WHERE 1";

$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $data = [];
    while ($row = $result->fetch_assoc()) {
        $data[] = $row;
    }
    echo json_encode([
        "success" => true,
        "data" => $data
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "No records found."
    ]);
}

$conn->close();
?>
