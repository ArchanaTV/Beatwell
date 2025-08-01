<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");

include "dbcon.php"; // Include your db connection

// Check if the request method is POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["success" => false, "message" => "Only POST method is allowed."]);
    exit;
}

// Query
$sql = "SELECT `s_no`, `surgery_type`, `date`, `breakfast`, `lunch`, `snacks`, `dinner`, `breakfast_description`, `lunch_description`, `snacks_description`, `dinner_description` FROM `meal_recomms` WHERE 1";

$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $meals = [];
    while ($row = $result->fetch_assoc()) {
        $meals[] = $row;
    }
    echo json_encode(["success" => true, "data" => $meals]);
} else {
    echo json_encode(["success" => false, "message" => "No records found."]);
}

$conn->close();
?>
