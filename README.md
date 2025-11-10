# 🍎 BeatWell - Meal Planner App for Heart Patients

A complete Android application for tracking meals, water intake, and health metrics with a PHP/MySQL backend.

## 📱 Features

- **User Authentication** - Secure login and registration
- **Profile Management** - Complete user profiles with health metrics
- **Meal Tracking** - Log meals with calories and nutritional info
- **Water Intake** - Track daily water consumption
- **Calendar View** - View meal history by date
- **Dynamic Dashboard** - Real-time progress tracking

## 🏗️ Architecture

- **Frontend**: Android (Kotlin)
- **Backend**: PHP REST API
- **Database**: MySQL
- **Server**: XAMPP (Apache + MySQL)

## 🚀 Quick Setup

### For Development:
1. Install XAMPP
2. Copy `backend` folder to `C:\xampp\htdocs\BeatWell\backend`
3. Create database `beatwell_db` in phpMyAdmin
4. Run SQL scripts: `manual_setup.sql` then `add_missing_profile_fields.sql`
5. Update IP address in `frontend/app/src/main/java/com/beatwell/app/network/ApiConfig.kt`
6. Build app: `cd frontend && ./gradlew assembleDebug`

### For New Developers:
See `SETUP_FOR_FRIEND.md` for detailed setup instructions.

## 🧪 Test Credentials

- **Username**: `Boss`
- **Password**: `password123`

## 📁 Project Structure

```
BeatWell/
├── backend/                 # PHP REST API
│   ├── api/                # API endpoints
│   ├── config/             # Database configuration
│   └── includes/           # Utility functions
├── frontend/               # Android app
│   └── app/src/main/java/  # Kotlin source code
└── *.sql                   # Database setup scripts
```

## ✅ Working Features

- ✅ User registration and login
- ✅ Complete profile management (height, weight, blood pressure, etc.)
- ✅ Meal logging with calorie tracking
- ✅ Water intake tracking
- ✅ Meal history with dates and times
- ✅ Dynamic dashboard with real progress data
- ✅ Profile editing and updates

## 🛠️ Built With

- **Android Studio** - Mobile app development
- **Kotlin** - Android app language
- **PHP** - Backend API
- **MySQL** - Database
- **XAMPP** - Local development server

---

**BeatWell** - Your personal health and nutrition companion! 🌟
