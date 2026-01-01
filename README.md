# VasateySec - Personal Safety & Emergency Alert System

A comprehensive Android safety application that provides voice-activated emergency alerts with real-time location tracking and guardian notification system.

---

## 📱 Project Overview

**VasateySec** is an Android-based personal safety application designed to help users send emergency alerts to designated guardians through voice activation. The app features real-time location tracking, push notifications, and a robust backend infrastructure using Supabase and Firebase Cloud Messaging.

---

## 🎯 Resume Bullet Points

### For Software Engineer / Mobile Developer Role:

- **Developed a full-stack Android safety application** using Kotlin, integrating voice recognition, real-time location tracking, and push notifications, resulting in a comprehensive emergency alert system

- **Architected and implemented a cloud-based backend infrastructure** leveraging Supabase (PostgreSQL) for data management and Firebase Cloud Messaging (FCM) for real-time notifications, supporting multi-device synchronization

- **Built voice-activated emergency alert system** using Vosk speech recognition library with custom wake word detection ("help me"), enabling hands-free emergency response capabilities

- **Designed and implemented Row Level Security (RLS) policies** in PostgreSQL/Supabase to ensure secure data access and privacy protection across user and guardian relationships

- **Integrated Google Maps SDK** for real-time location visualization, providing guardians with live location tracking and navigation capabilities during emergencies

- **Developed RESTful API integration** with custom Vercel serverless functions for sending emergency notifications with user location data and contact information

- **Implemented secure authentication system** using Supabase Auth with email/password authentication, including auto-login, session management, and secure logout with token cleanup

- **Created comprehensive guardian management system** allowing users to add/remove guardians via email with automatic user linking through database triggers

- **Built real-time location tracking service** using Google Play Services Location API with background location updates and live location sharing capabilities

- **Designed Material Design 3 UI/UX** with navigation drawer, bottom navigation, and responsive layouts following Android design guidelines

- **Implemented camera integration** for capturing emergency photos (front/back camera) with Supabase Storage integration for secure photo storage

- **Created alert history and tracking system** allowing users to view sent/received alerts with detailed information including timestamps, location data, and recipient status

- **Developed FCM token management system** with automatic token refresh, multi-device support, and token deactivation on device changes

- **Built database schema with 5 normalized tables** (users, fcm_tokens, guardians, alert_history, alert_recipients) with proper foreign key relationships and indexes

- **Implemented Edge Functions in Supabase** for server-side logic including live location requests using FCM V1 API with JWT authentication

- **Wrote ~8,000 lines of Kotlin code** including activities, services, utilities, and data models following Android best practices and MVVM architecture patterns

- **Configured Gradle build system** with multi-module support, ProGuard rules, and release signing for production deployment

- **Implemented secure data storage** using Android Security Crypto library for encrypted SharedPreferences storing sensitive user data

- **Created comprehensive permission handling system** for runtime permissions including location (foreground/background), camera, microphone, and notifications

- **Developed notification handling service** with custom notification channels, data-only messages, and deep linking to specific app screens

---

## 🛠️ Technical Stack

### **Frontend (Android)**
- **Language:** Kotlin
- **UI Framework:** Material Design 3, Android Jetpack
- **Architecture:** MVVM pattern
- **View Binding:** Android View Binding
- **Navigation:** Navigation Component, DrawerLayout

### **Backend & Cloud Services**
- **Database:** Supabase (PostgreSQL) with Row Level Security (RLS)
- **Authentication:** Supabase Auth (JWT-based)
- **Storage:** Supabase Storage (S3-compatible)
- **Push Notifications:** Firebase Cloud Messaging (FCM) V1 API
- **Serverless Functions:** Vercel Edge Functions, Supabase Edge Functions (Deno)

### **APIs & SDKs**
- **Google Play Services:** Location API, Maps SDK
- **Firebase:** Cloud Messaging, Analytics
- **Vosk:** Offline speech recognition for wake word detection
- **Camera2 API:** Camera functionality
- **OkHttp:** HTTP client for API calls
- **Ktor:** Kotlin HTTP client for Supabase integration

### **Security**
- **Authentication:** JWT tokens with Supabase Auth
- **Data Encryption:** Android Security Crypto
- **Database Security:** PostgreSQL RLS policies
- **API Security:** Service role keys, environment variables

### **Build Tools**
- **Build System:** Gradle (Kotlin DSL)
- **Version Control:** Git
- **CI/CD Ready:** Gradle build scripts for release builds
- **Code Quality:** ProGuard configuration, Lint checks

---

## ✨ Key Features

### 1. **Voice-Activated Emergency Alerts**
   - Wake word detection using Vosk ("help me")
   - Runs as foreground service for always-on protection
   - Automatic location capture on alert trigger
   - Emergency photo capture (front and back camera)

### 2. **Real-Time Location Tracking**
   - Live location updates using GPS and network providers
   - Background location tracking with proper permissions
   - Location sharing with guardians during emergencies
   - Google Maps integration for visualization

### 3. **Guardian Management System**
   - Add/remove guardians by email
   - Automatic user linking when guardians sign up
   - Support for multiple guardians per user
   - Guardian relationship tracking in database

### 4. **Push Notification System**
   - Data-only FCM notifications for privacy
   - Custom notification channels for Android O+
   - Deep linking to alert details from notifications
   - Multi-device token management

### 5. **Alert History & Tracking**
   - View all sent and received alerts
   - Detailed alert information (time, location, status)
   - Track guardian viewing status
   - Alert confirmation system

### 6. **Secure Authentication**
   - Email/password authentication via Supabase
   - Auto-login with session persistence
   - Secure logout with FCM token cleanup
   - Password validation and error handling

### 7. **User Profile Management**
   - Edit name and phone number
   - Profile data synchronized with Supabase
   - Secure data storage using encrypted preferences

### 8. **Maps & Navigation**
   - Interactive Google Maps for alert locations
   - Live vs. old location differentiation
   - Direct navigation to emergency location
   - Custom markers for different alert types

---

## 📊 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Android App (Kotlin)                     │
├─────────────────────────────────────────────────────────────┤
│  Activities: Login, Home, AddGuardian, AlertHistory, etc.   │
│  Services: VoskWakeWordService, VasateyFCMService           │
│  Utils: AlertManager, LocationManager, FCMTokenManager       │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────┐
│                   Backend Services                           │
├─────────────────────────────────────────────────────────────┤
│  Supabase:                                                   │
│    - PostgreSQL Database (5 tables with RLS)                │
│    - Authentication (JWT)                                    │
│    - Storage (Emergency photos)                              │
│    - Edge Functions (Live location requests)                │
│                                                               │
│  Firebase:                                                   │
│    - Cloud Messaging (FCM V1 API)                           │
│    - Analytics                                               │
│                                                               │
│  Vercel:                                                     │
│    - Serverless Functions (Send notifications)              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Schema

### Tables:
1. **users** - User profiles (name, email, phone)
2. **fcm_tokens** - FCM tokens for push notifications
3. **guardians** - Guardian relationships
4. **alert_history** - Emergency alert records
5. **alert_recipients** - Alert delivery tracking

### Security:
- Row Level Security (RLS) policies on all tables
- Users can only access their own data
- Guardians can only view alerts sent to them
- Service role for admin operations

---

## 🚀 Performance & Scalability

- **Efficient location updates:** Batched updates to reduce battery drain
- **Database indexing:** Optimized queries with proper indexes
- **Token management:** Automatic deactivation of old tokens
- **Multi-device support:** One user can have multiple active devices
- **Edge functions:** Serverless architecture for scalability
- **Offline support:** Local caching with sync on reconnection

---

## 📱 Supported Platforms

- **Android:** API Level 26+ (Android 8.0 Oreo and above)
- **Target SDK:** Android 14 (API Level 34)
- **Architecture:** ARM, ARM64, x86, x86_64

---

## 🔒 Security Features

- End-to-end encrypted communication
- Secure token storage using Android Keystore
- Row Level Security (RLS) in database
- Encrypted SharedPreferences for local data
- Permission-based access control
- Secure API endpoints with authentication

---

## 📈 Project Statistics

- **Lines of Code:** ~8,000+ lines of Kotlin
- **Activities:** 15+ Android activities
- **Services:** 2 background services
- **Database Tables:** 5 tables with relationships
- **API Integrations:** 3 (Supabase, Firebase, Vercel)
- **Dependencies:** 30+ libraries and SDKs

---

## 🎓 Learning Outcomes & Skills Demonstrated

### Technical Skills:
- ✅ Android development with Kotlin
- ✅ RESTful API integration
- ✅ Real-time data synchronization
- ✅ Database design and optimization
- ✅ Cloud services integration (Firebase, Supabase)
- ✅ Location-based services
- ✅ Push notification implementation
- ✅ Voice recognition and speech processing
- ✅ Security best practices
- ✅ Material Design implementation

### Software Engineering Practices:
- ✅ Version control with Git
- ✅ Code organization and architecture
- ✅ Error handling and logging
- ✅ Permission management
- ✅ Asynchronous programming (Coroutines)
- ✅ Dependency injection patterns
- ✅ Build automation with Gradle

---

## 📝 Use Cases

1. **Emergency Situations:** User in danger says "help me" → Alerts sent to guardians with location
2. **Safety Check:** Guardians can track user's live location during emergencies
3. **Alert History:** Review past emergencies and response times
4. **Guardian Network:** Build a network of trusted contacts for safety

---

## 🌟 Unique Selling Points

- **Voice-activated alerts:** Hands-free operation in emergencies
- **Offline wake word detection:** Works without internet
- **Real-time location sharing:** Live updates to guardians
- **Multi-platform notifications:** FCM ensures reliable delivery
- **Privacy-focused:** Data-only notifications, RLS policies
- **Scalable architecture:** Cloud-based backend supports growth

---

## 📞 Contact & Support

For questions or contributions, please contact the development team.

---

## 📄 License

This project is part of a portfolio demonstrating mobile development capabilities.

---

## 🔗 Related Documentation

- [Setup Guide](SETUP_GUIDE.md) - Step-by-step instructions for configuring and running the app
- [Implementation Details](README_IMPLEMENTATION.md) - Detailed implementation guide and current status
- [Alert System Documentation](ALERT_SYSTEM_IMPLEMENTATION.md) - Complete alert system architecture and flow
- [Live Location Guide](COMPLETE_LIVE_LOCATION_GUIDE.md) - Real-time location tracking implementation guide

---

**Built with ❤️ using Kotlin & Modern Android Development**
