<?php
/**
 * BeatWell Users API
 * Handles user registration, login, and authentication
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/functions.php';

// Set content type to JSON
header('Content-Type: application/json');

// Enable CORS for mobile app
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// Initialize database
if (!initializeDatabase()) {
    sendError('Database initialization failed', 500);
}

// Get request method and data
$method = $_SERVER['REQUEST_METHOD'];
$data = getRequestData();

// Route requests
switch ($method) {
    case 'POST':
        $action = $data['action'] ?? '';
        switch ($action) {
            case 'register':
                handleRegister($data);
                break;
            case 'login':
                handleLogin($data);
                break;
            case 'logout':
                handleLogout($data);
                break;
            case 'update_profile':
                handleUpdateProfile($data);
                break;
            default:
                sendError('Invalid action', 400);
        }
        break;
    case 'GET':
        $action = $_GET['action'] ?? '';
        switch ($action) {
            case 'verify':
                handleVerify($_GET);
                break;
            case 'profile':
                handleGetProfile($_GET);
                break;
            case '':
                // Default GET response - API documentation
                sendSuccess([
                    'api' => 'BeatWell Users API',
                    'version' => '1.0.0',
                    'endpoints' => [
                        'POST with action=register' => 'User registration',
                        'POST with action=login' => 'User login',
                        'POST with action=logout' => 'User logout',
                        'GET with action=verify&session_token=X' => 'Verify session',
                        'GET with action=profile&session_token=X' => 'Get user profile'
                    ],
                    'timestamp' => date('Y-m-d H:i:s')
                ], 'Users API is working');
                break;
            default:
                sendError('Invalid action', 400);
        }
        break;
    default:
        sendError('Method not allowed', 405);
}

/**
 * Handle user registration
 * @param array $data
 */
function handleRegister($data) {
    // Validate required fields
    $requiredFields = ['username', 'email', 'password', 'confirm_password', 'first_name', 'last_name', 'phone', 'date_of_birth', 'gender'];
    foreach ($requiredFields as $field) {
        if (empty($data[$field])) {
            sendError("Field '$field' is required", 400);
        }
    }
    
    // Sanitize inputs
    $username = sanitizeInput($data['username']);
    $email = sanitizeInput($data['email']);
    $password = $data['password'];
    $confirmPassword = $data['confirm_password'];
    $firstName = sanitizeInput($data['first_name']);
    $lastName = sanitizeInput($data['last_name']);
    $phone = sanitizeInput($data['phone']);
    $dateOfBirth = sanitizeInput($data['date_of_birth']);
    $gender = sanitizeInput($data['gender']);
    $address = sanitizeInput($data['address'] ?? '');
    $city = sanitizeInput($data['city'] ?? '');
    $state = sanitizeInput($data['state'] ?? '');
    $zipCode = sanitizeInput($data['zip_code'] ?? '');
    $emergencyContactName = sanitizeInput($data['emergency_contact_name'] ?? '');
    $emergencyContactPhone = sanitizeInput($data['emergency_contact_phone'] ?? '');
    $medicalConditions = sanitizeInput($data['medical_conditions'] ?? '');
    $allergies = sanitizeInput($data['allergies'] ?? '');
    
    // Validate username
    if (strlen($username) < 3 || strlen($username) > 50) {
        sendError('Username must be between 3 and 50 characters', 400);
    }
    
    if (!preg_match('/^[a-zA-Z0-9_]+$/', $username)) {
        sendError('Username can only contain letters, numbers, and underscores', 400);
    }
    
    // Validate email
    if (!isValidEmail($email)) {
        sendError('Invalid email format', 400);
    }
    
    // Check if email ends with .com
    if (!preg_match('/\.com$/', $email)) {
        sendError('Email must end with .com', 400);
    }
    
    // Validate password
    if (strlen($password) < 8) {
        sendError('Password must be at least 8 characters long', 400);
    }
    
    if (!preg_match('/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/', $password)) {
        sendError('Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character', 400);
    }
    
    // Check password confirmation
    if ($password !== $confirmPassword) {
        sendError('Passwords do not match', 400);
    }
    
    // Validate first name
    if (strlen($firstName) < 2 || strlen($firstName) > 50) {
        sendError('First name must be between 2 and 50 characters', 400);
    }
    
    // Validate last name
    if (strlen($lastName) < 2 || strlen($lastName) > 50) {
        sendError('Last name must be between 2 and 50 characters', 400);
    }
    
    // Validate phone number
    if (!preg_match('/^[\+]?[1-9][\d]{0,15}$/', $phone)) {
        sendError('Invalid phone number format', 400);
    }
    
    // Validate date of birth
    $dateOfBirthObj = DateTime::createFromFormat('m/d/Y', $dateOfBirth);
    if (!$dateOfBirthObj || $dateOfBirthObj->format('m/d/Y') !== $dateOfBirth) {
        sendError('Invalid date of birth format. Use MM/DD/YYYY', 400);
    }
    
    // Check if date of birth is not in the future
    if ($dateOfBirthObj > new DateTime()) {
        sendError('Date of birth cannot be in the future', 400);
    }
    
    // Check if user is at least 13 years old
    $age = $dateOfBirthObj->diff(new DateTime())->y;
    if ($age < 13) {
        sendError('You must be at least 13 years old to register', 400);
    }
    
    // Validate gender
    $validGenders = ['Male', 'Female', 'Other', 'Prefer not to say'];
    if (!in_array($gender, $validGenders)) {
        sendError('Invalid gender selection', 400);
    }
    
    // Check if user already exists
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        // Check username exists
        $stmt = $pdo->prepare("SELECT id FROM users WHERE username = ?");
        $stmt->execute([$username]);
        if ($stmt->fetch()) {
            sendError('Username already exists', 409);
        }
        
        // Check email exists
        $stmt = $pdo->prepare("SELECT id FROM users WHERE email = ?");
        $stmt->execute([$email]);
        if ($stmt->fetch()) {
            sendError('Email already exists', 409);
        }
        
        // Hash password
        $passwordHash = hashPassword($password);
        
        // Insert new user
        $stmt = $pdo->prepare("INSERT INTO users (username, email, password_hash, first_name, last_name, phone, date_of_birth, gender, address, city, state, zip_code, emergency_contact_name, emergency_contact_phone, medical_conditions, allergies) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([
            $username, 
            $email, 
            $passwordHash, 
            $firstName, 
            $lastName, 
            $phone, 
            $dateOfBirthObj->format('Y-m-d'), 
            $gender, 
            $address, 
            $city, 
            $state, 
            $zipCode, 
            $emergencyContactName, 
            $emergencyContactPhone, 
            $medicalConditions, 
            $allergies
        ]);
        
        $userId = $pdo->lastInsertId();
        
        // Generate session token
        $sessionToken = generateToken();
        $expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));
        
        $stmt = $pdo->prepare("INSERT INTO sessions (user_id, session_token, expires_at) VALUES (?, ?, ?)");
        $stmt->execute([$userId, $sessionToken, $expiresAt]);
        
        // Log successful registration
        logError("User registered successfully", ['user_id' => $userId, 'username' => $username, 'email' => $email]);
        
        sendSuccess([
            'user_id' => $userId,
            'username' => $username,
            'email' => $email,
            'first_name' => $firstName,
            'last_name' => $lastName,
            'phone' => $phone,
            'date_of_birth' => $dateOfBirthObj->format('Y-m-d'),
            'gender' => $gender,
            'address' => $address,
            'city' => $city,
            'state' => $state,
            'zip_code' => $zipCode,
            'emergency_contact_name' => $emergencyContactName,
            'emergency_contact_phone' => $emergencyContactPhone,
            'medical_conditions' => $medicalConditions,
            'allergies' => $allergies,
            'session_token' => $sessionToken,
            'expires_at' => $expiresAt
        ], 'User registered successfully');
        
    } catch (PDOException $e) {
        logError("Registration failed", ['error' => $e->getMessage(), 'username' => $username, 'email' => $email]);
        sendError('Registration failed', 500);
    }
}

/**
 * Handle user login
 * @param array $data
 */
function handleLogin($data) {
    // Validate required fields
    if (empty($data['username']) || empty($data['password'])) {
        sendError('Username and password are required', 400);
    }
    
    // Sanitize inputs
    $username = sanitizeInput($data['username']);
    $password = $data['password'];
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        // Find user by username or email
        $stmt = $pdo->prepare("SELECT id, username, email, password_hash FROM users WHERE username = ? OR email = ?");
        $stmt->execute([$username, $username]);
        $user = $stmt->fetch();
        
        if (!$user) {
            sendError('Invalid username or password', 401);
        }
        
        // Verify password
        if (!verifyPassword($password, $user['password_hash'])) {
            sendError('Invalid username or password', 401);
        }
        
        // Generate new session token
        $sessionToken = generateToken();
        $expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));
        
        // Delete old sessions for this user
        $stmt = $pdo->prepare("DELETE FROM sessions WHERE user_id = ?");
        $stmt->execute([$user['id']]);
        
        // Create new session
        $stmt = $pdo->prepare("INSERT INTO sessions (user_id, session_token, expires_at) VALUES (?, ?, ?)");
        $stmt->execute([$user['id'], $sessionToken, $expiresAt]);
        
        // Log successful login
        logError("User logged in successfully", ['user_id' => $user['id'], 'username' => $user['username']]);
        
        // Get full user profile data
        $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
        $stmt->execute([$user['id']]);
        $fullUser = $stmt->fetch();
        
        sendSuccess([
            'user_id' => $user['id'],
            'username' => $user['username'],
            'email' => $user['email'],
            'first_name' => $fullUser['first_name'],
            'last_name' => $fullUser['last_name'],
            'phone' => $fullUser['phone'],
            'date_of_birth' => $fullUser['date_of_birth'],
            'gender' => $fullUser['gender'],
            'session_token' => $sessionToken,
            'expires_at' => $expiresAt
        ], 'Login successful');
        
    } catch (PDOException $e) {
        logError("Login failed", ['error' => $e->getMessage(), 'username' => $username]);
        sendError('Login failed', 500);
    }
}

/**
 * Handle user logout
 * @param array $data
 */
function handleLogout($data) {
    if (empty($data['session_token'])) {
        sendError('Session token is required', 400);
    }
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        $stmt = $pdo->prepare("DELETE FROM sessions WHERE session_token = ?");
        $stmt->execute([$data['session_token']]);
        
        sendSuccess([], 'Logout successful');
        
    } catch (PDOException $e) {
        logError("Logout failed", ['error' => $e->getMessage(), 'session_token' => $data['session_token']]);
        sendError('Logout failed', 500);
    }
}

/**
 * Handle session verification
 * @param array $data
 */
function handleVerify($data) {
    if (empty($data['session_token'])) {
        sendError('Session token is required', 400);
    }
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        $stmt = $pdo->prepare("
            SELECT u.id, u.username, u.email, s.expires_at 
            FROM users u 
            JOIN sessions s ON u.id = s.user_id 
            WHERE s.session_token = ? AND s.expires_at > NOW()
        ");
        $stmt->execute([$data['session_token']]);
        $session = $stmt->fetch();
        
        if (!$session) {
            sendError('Invalid or expired session', 401);
        }
        
        sendSuccess([
            'user_id' => $session['id'],
            'username' => $session['username'],
            'email' => $session['email'],
            'expires_at' => $session['expires_at']
        ], 'Session valid');
        
    } catch (PDOException $e) {
        logError("Session verification failed", ['error' => $e->getMessage(), 'session_token' => $data['session_token']]);
        sendError('Session verification failed', 500);
    }
}

/**
 * Handle get user profile
 * @param array $data
 */
function handleGetProfile($data) {
    if (empty($data['session_token'])) {
        sendError('Session token is required', 400);
    }
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        // Verify session and get user data
        $stmt = $pdo->prepare("
            SELECT u.*, s.expires_at 
            FROM users u 
            JOIN sessions s ON u.id = s.user_id 
            WHERE s.session_token = ? AND s.expires_at > NOW()
        ");
        $stmt->execute([$data['session_token']]);
        $user = $stmt->fetch();
        
        if (!$user) {
            sendError('Invalid or expired session', 401);
        }
        
        // Calculate age from date of birth
        $age = 0;
        if ($user['date_of_birth']) {
            $birthDate = new DateTime($user['date_of_birth']);
            $today = new DateTime();
            $age = $today->diff($birthDate)->y;
        }
        
        // Format user data for response (including new profile fields)
        $profileData = [
            'id' => $user['id'],
            'username' => $user['username'],
            'email' => $user['email'],
            'first_name' => $user['first_name'],
            'last_name' => $user['last_name'],
            'phone' => $user['phone'],
            'date_of_birth' => $user['date_of_birth'],
            'age' => $age,
            'gender' => $user['gender'],
            'address' => $user['address'],
            'city' => $user['city'],
            'state' => $user['state'],
            'zip_code' => $user['zip_code'],
            'emergency_contact_name' => $user['emergency_contact_name'],
            'emergency_contact_phone' => $user['emergency_contact_phone'],
            'medical_conditions' => $user['medical_conditions'],
            'allergies' => $user['allergies'],
            // New profile fields
            'height' => $user['height'] ? (float)$user['height'] : null,
            'weight' => $user['weight'] ? (float)$user['weight'] : null,
            'blood_pressure_systolic' => $user['blood_pressure_systolic'] ? (int)$user['blood_pressure_systolic'] : null,
            'blood_pressure_diastolic' => $user['blood_pressure_diastolic'] ? (int)$user['blood_pressure_diastolic'] : null,
            'diabetes_type' => $user['diabetes_type'] ?: 'none',
            'treatment_type' => $user['treatment_type'] ?: '',
            'created_at' => $user['created_at'],
            'updated_at' => $user['updated_at']
        ];
        
        sendSuccess($profileData, 'Profile retrieved successfully');
        
    } catch (PDOException $e) {
        logError("Profile retrieval failed", ['error' => $e->getMessage(), 'session_token' => $data['session_token']]);
        sendError('Profile retrieval failed', 500);
    }
}

/**
 * Handle update user profile
 * @param array $data
 */
function handleUpdateProfile($data) {
    if (empty($data['session_token'])) {
        sendError('Session token is required', 400);
    }
    
    $pdo = getDBConnection();
    if (!$pdo) {
        sendError('Database connection failed', 500);
    }
    
    try {
        // Verify session and get user ID
        $stmt = $pdo->prepare("
            SELECT u.id 
            FROM users u 
            JOIN sessions s ON u.id = s.user_id 
            WHERE s.session_token = ? AND s.expires_at > NOW()
        ");
        $stmt->execute([$data['session_token']]);
        $session = $stmt->fetch();
        
        if (!$session) {
            sendError('Invalid or expired session', 401);
        }
        
        $userId = $session['id'];
        
        // Prepare update fields
        $updateFields = [];
        $updateValues = [];
        
        // Check which fields to update (including new profile fields)
        $updatableFields = [
            'first_name', 'last_name', 'phone', 'date_of_birth', 'gender',
            'address', 'city', 'state', 'zip_code', 'emergency_contact_name',
            'emergency_contact_phone', 'medical_conditions', 'allergies',
            'height', 'weight', 'blood_pressure_systolic', 'blood_pressure_diastolic',
            'diabetes_type', 'treatment_type'
        ];
        
        foreach ($updatableFields as $field) {
            if (isset($data[$field])) {
                $updateFields[] = "$field = ?";
                $updateValues[] = sanitizeInput($data[$field]);
            }
        }
        
        if (empty($updateFields)) {
            sendError('No fields to update', 400);
        }
        
        // Add updated_at timestamp
        $updateFields[] = "updated_at = NOW()";
        
        // Execute update
        $sql = "UPDATE users SET " . implode(', ', $updateFields) . " WHERE id = ?";
        $updateValues[] = $userId;
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute($updateValues);
        
        // Get updated user data
        $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
        
        // Calculate age
        $age = 0;
        if ($user['date_of_birth']) {
            $birthDate = new DateTime($user['date_of_birth']);
            $today = new DateTime();
            $age = $today->diff($birthDate)->y;
        }
        
        $profileData = [
            'id' => $user['id'],
            'username' => $user['username'],
            'email' => $user['email'],
            'first_name' => $user['first_name'],
            'last_name' => $user['last_name'],
            'phone' => $user['phone'],
            'date_of_birth' => $user['date_of_birth'],
            'age' => $age,
            'gender' => $user['gender'],
            'address' => $user['address'],
            'city' => $user['city'],
            'state' => $user['state'],
            'zip_code' => $user['zip_code'],
            'emergency_contact_name' => $user['emergency_contact_name'],
            'emergency_contact_phone' => $user['emergency_contact_phone'],
            'medical_conditions' => $user['medical_conditions'],
            'allergies' => $user['allergies'],
            'updated_at' => $user['updated_at']
        ];
        
        logError("Profile updated successfully", ['user_id' => $userId]);
        sendSuccess($profileData, 'Profile updated successfully');
        
    } catch (PDOException $e) {
        logError("Profile update failed", ['error' => $e->getMessage(), 'session_token' => $data['session_token']]);
        sendError('Profile update failed', 500);
    }
}
?>