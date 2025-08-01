<?php
header('Content-Type: application/json');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");

include "dbcon.php"; // Include DB connection

// Only allow POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed."
    ]);
    exit;
}

// SQL query
$sql = "SELECT `s_no`, `name`, `dob`, `phone_no` FROM `profile` WHERE 1";

$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $profiles = [];

    while ($row = $result->fetch_assoc()) {
        $profiles[] = $row;
    }

    echo json_encode([
        "success" => true,
        "message" => "Profile data fetched successfully.",
        "data" => $profiles
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "No profile data found."
    ]);
}

$conn->close();
?>
