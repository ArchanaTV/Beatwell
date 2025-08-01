<?php
$host = "localhost";
$user = "root";
$password = "";  // Update if your MySQL has a password
$database = "new_beatwell";

// Create connection
$conn = new mysqli($host, $user, $password, $database);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
?>
