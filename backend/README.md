# BeatWell Backend API

A robust PHP backend API for the BeatWell Android application, providing user authentication and session management.

## Features

- **User Registration**: Secure account creation with comprehensive validation
- **User Login**: Authentication with session management
- **Password Security**: Strong password requirements and secure hashing
- **Email Validation**: Strict email format validation (must end with .com)
- **Session Management**: Secure token-based sessions
- **Database Security**: Prepared statements and input sanitization
- **CORS Support**: Configured for mobile app integration
- **Error Logging**: Comprehensive error tracking and logging

## Requirements

- PHP 7.4 or higher
- MySQL 5.7 or higher
- PDO MySQL extension
- Web server (Apache/Nginx)

## Installation

### 1. Database Setup

1. Create a MySQL database named `beatwell_db`
2. Update database credentials in `config/database.php`:

```php
define('DB_HOST', 'localhost');
define('DB_NAME', 'beatwell_db');
define('DB_USER', 'your_username');
define('DB_PASS', 'your_password');
```

### 2. Initialize Database

Run the database initialization script:

```bash
php init_db.php
```

This will create the necessary tables:
- `users`: Stores user account information
- `sessions`: Manages user authentication sessions

### 3. Web Server Configuration

#### Apache
Ensure mod_rewrite is enabled and create a `.htaccess` file:

```apache
RewriteEngine On
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule ^(.*)$ index.php [QSA,L]
```

#### Nginx
Add the following location block:

```nginx
location / {
    try_files $uri $uri/ /index.php?$query_string;
}
```

## API Endpoints

### Base URL
```
http://your-domain/backend/
```

### User Registration
**POST** `/api/users`

```json
{
    "action": "register",
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "confirm_password": "SecurePass123!"
}
```

**Validation Rules:**
- Username: 3-50 characters, alphanumeric and underscores only
- Email: Valid format ending with `.com`
- Password: Minimum 8 characters with uppercase, lowercase, number, and special character
- Confirm Password: Must match password

**Response:**
```json
{
    "status": "success",
    "message": "User registered successfully",
    "data": {
        "user_id": 1,
        "username": "johndoe",
        "email": "john@example.com",
        "session_token": "abc123...",
        "expires_at": "2024-02-15 10:30:00"
    }
}
```

### User Login
**POST** `/api/users`

```json
{
    "action": "login",
    "username": "johndoe",
    "password": "SecurePass123!"
}
```

**Response:**
```json
{
    "status": "success",
    "message": "Login successful",
    "data": {
        "user_id": 1,
        "username": "johndoe",
        "email": "john@example.com",
        "session_token": "def456...",
        "expires_at": "2024-02-15 10:30:00"
    }
}
```

### User Logout
**POST** `/api/users`

```json
{
    "action": "logout",
    "session_token": "def456..."
}
```

### Session Verification
**GET** `/api/users?action=verify&session_token=def456...`

**Response:**
```json
{
    "status": "success",
    "message": "Session valid",
    "data": {
        "user_id": 1,
        "username": "johndoe",
        "email": "john@example.com",
        "expires_at": "2024-02-15 10:30:00"
    }
}
```

### Health Check
**GET** `/api/health`

**Response:**
```json
{
    "status": "success",
    "message": "BeatWell API is running",
    "timestamp": "2024-01-15 10:30:00",
    "version": "1.0.0"
}
```

## Error Handling

All API responses follow a consistent format:

**Success Response:**
```json
{
    "status": "success",
    "message": "Operation successful",
    "data": { ... }
}
```

**Error Response:**
```json
{
    "status": "error",
    "message": "Error description"
}
```

**Common HTTP Status Codes:**
- `200`: Success
- `400`: Bad Request (validation errors)
- `401`: Unauthorized (invalid credentials)
- `404`: Not Found
- `409`: Conflict (user already exists)
- `500`: Internal Server Error

## Security Features

### Password Security
- Minimum 8 characters
- Must contain uppercase, lowercase, number, and special character
- Secure hashing using PHP's `password_hash()` function

### Email Validation
- Strict email format validation
- Must end with `.com` domain
- Duplicate email prevention

### Session Security
- Random token generation
- 30-day session expiration
- Automatic cleanup of expired sessions

### Input Sanitization
- All inputs are sanitized and validated
- SQL injection prevention with prepared statements
- XSS protection with input escaping

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Sessions Table
```sql
CREATE TABLE sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## Deployment

### Production Considerations

1. **Database Security:**
   - Use strong database credentials
   - Enable SSL connections
   - Regular backups

2. **Server Security:**
   - Use HTTPS for all API calls
   - Implement rate limiting
   - Regular security updates

3. **Environment Configuration:**
   - Set appropriate PHP error reporting levels
   - Configure proper logging
   - Use environment variables for sensitive data

### College Server Deployment

For deployment on your college server:

1. Upload all backend files to your web directory
2. Create the MySQL database
3. Update database credentials in `config/database.php`
4. Run `php init_db.php` to initialize tables
5. Configure web server (Apache/Nginx)
6. Test API endpoints
7. Update Android app with server URL

## Testing

Test the API using tools like Postman or curl:

```bash
# Test registration
curl -X POST http://your-domain/backend/api/users \
  -H "Content-Type: application/json" \
  -d '{"action":"register","username":"testuser","email":"test@example.com","password":"TestPass123!","confirm_password":"TestPass123!"}'

# Test login
curl -X POST http://your-domain/backend/api/users \
  -H "Content-Type: application/json" \
  -d '{"action":"login","username":"testuser","password":"TestPass123!"}'
```

## Support

For issues or questions:
1. Check error logs in your web server error log
2. Verify database connection and credentials
3. Ensure all required PHP extensions are installed
4. Test API endpoints individually

## License

This project is part of the BeatWell Android application.
