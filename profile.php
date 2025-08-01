<?php
// Include DB connection
include "dbcon.php";

// Set content type to JSON
header('Content-Type: application/json');

// Execute query
$sql = "SELECT `s_no`, `user_name`, `phone_no`, `dob` FROM `profile` WHERE 1";
$result = $conn->query($sql);


// Check if any rows were returned
if ($result && $result->num_rows > 0) {
    $profiles = [];

    while ($row = $result->fetch_assoc()) {
        $profiles[] = $row;
    }

    echo json_encode([
        'success' => true,
        'message' => 'Profile data fetched successfully.',
        'data' => $profiles
    ]);
} else {
    echo json_encode([
        'success' => false,
        'message' => 'No profile data found.'
    ]);
}

// Close DB connection
$conn->close();
?>
