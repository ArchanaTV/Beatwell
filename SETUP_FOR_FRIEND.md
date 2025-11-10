# 🚀 BeatWell Setup Guide for Your Friend

## 📋 **What Your Friend Needs**
1. **XAMPP** (Apache + MySQL + PHP) - https://www.apachefriends.org/
2. **Android Studio** - https://developer.android.com/studio
3. **The BeatWell project files** (copy the entire project folder)

## 🔧 **Simple Setup Steps**

### **Step 1: Install XAMPP**
1. Download and install XAMPP
2. Start **Apache** and **MySQL** services in XAMPP Control Panel
3. Test: Open http://localhost - should show XAMPP dashboard

### **Step 2: Setup Database**
1. Copy the `backend` folder to: `C:\xampp\htdocs\BeatWell\backend`
2. Open phpMyAdmin: http://localhost/phpmyadmin
3. Create database: `beatwell_db`
4. Import SQL files in order:
   - First: `backend/manual_setup.sql`
   - Then: `backend/add_missing_profile_fields.sql`
5. Test: http://localhost/BeatWell/backend/health.php should show "API is running"

### **Step 3: Configure Mobile App**
1. Find their computer's IP address:
   ```cmd
   ipconfig
   ```
   Look for "IPv4 Address" (e.g., `192.168.1.100`)

2. Edit `frontend/app/src/main/java/com/beatwell/app/network/ApiConfig.kt`:
   ```kotlin
   private const val DEVICE_BASE_URL = "http://[THEIR_IP]/BeatWell/backend/api"
   ```
   Replace `[THEIR_IP]` with their actual IP address

### **Step 4: Build and Test**
```bash
cd frontend
./gradlew assembleDebug
```

## 🎯 **That's It!**

### **Test Credentials:**
- Username: `Boss`
- Password: `password123`

### **Expected Results:**
- ✅ Profile shows: Name, Height (175.5 cm), Weight (70.2 kg), Blood Pressure (120/80)
- ✅ Meal History shows: 7+ meal records with dates and calories
- ✅ Edit Profile works and updates database
- ✅ All dynamic functionality working

## ⚠️ **If Something Goes Wrong**

### **Backend Issues:**
- Ensure XAMPP Apache and MySQL are running
- Check if database `beatwell_db` exists
- Verify backend URL works in browser

### **Mobile App Issues:**
- Both devices must be on same WiFi network
- IP address must be correct
- Try disabling Windows Firewall temporarily

### **Build Issues:**
- Ensure Android Studio is properly installed
- Try: `./gradlew clean assembleDebug`

## 🚀 **Success = Complete Working App**
Your friend will have the full BeatWell app running on their machine with all the dynamic features working perfectly!