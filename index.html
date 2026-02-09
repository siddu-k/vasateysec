<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>VasateySec - System Flowchart</title>
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            min-height: 100vh;
        }

        .container {
            max-width: 1600px;
            margin: 0 auto;
            background: white;
            border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            overflow: hidden;
        }

        header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }

        header h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
        }

        header p {
            font-size: 1.1em;
            opacity: 0.9;
        }

        .controls {
            background: #f8f9fa;
            padding: 20px;
            border-bottom: 3px solid #667eea;
            display: flex;
            justify-content: center;
            gap: 15px;
            flex-wrap: wrap;
        }

        .btn {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            padding: 12px 30px;
            border-radius: 25px;
            font-size: 1em;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.3);
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4);
        }

        .btn.active {
            background: #2d3748;
        }

        .content {
            padding: 40px;
            overflow-x: auto;
        }

        .diagram-container {
            display: none;
            animation: fadeIn 0.5s;
        }

        .diagram-container.active {
            display: block;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .mermaid {
            background: white;
            padding: 20px;
            border-radius: 15px;
            margin: 20px 0;
        }

        .legend {
            background: #f8f9fa;
            border-left: 5px solid #667eea;
            padding: 20px;
            margin: 20px 0;
            border-radius: 10px;
        }

        .legend h3 {
            color: #667eea;
            margin-bottom: 15px;
        }

        .legend-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-top: 15px;
        }

        .legend-item {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .legend-color {
            width: 30px;
            height: 30px;
            border-radius: 5px;
        }

        .info-box {
            background: #e7f3ff;
            border-left: 5px solid #2196f3;
            padding: 20px;
            margin: 20px 0;
            border-radius: 10px;
        }

        .warning-box {
            background: #fff3cd;
            border-left: 5px solid #ffc107;
            padding: 20px;
            margin: 20px 0;
            border-radius: 10px;
        }

        footer {
            background: #2d3748;
            color: white;
            padding: 20px;
            text-align: center;
        }

        @media (max-width: 768px) {
            header h1 {
                font-size: 1.8em;
            }
            
            .content {
                padding: 20px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>🚨 VASATEYSEC - System Flowchart</h1>
            <p>Complete Emergency Alert System Data Flow</p>
        </header>

        <div class="controls">
            <button class="btn active" onclick="showDiagram('complete')">🔄 Complete Flow</button>
            <button class="btn" onclick="showDiagram('alert')">🚨 Alert Trigger</button>
            <button class="btn" onclick="showDiagram('database')">🗄️ Database Flow</button>
            <button class="btn" onclick="showDiagram('notification')">🔔 Notification Flow</button>
            <button class="btn" onclick="showDiagram('architecture')">🏗️ System Architecture</button>
        </div>

        <div class="content">
            <!-- COMPLETE FLOW DIAGRAM -->
            <div id="complete" class="diagram-container active">
                <h2 style="color: #667eea; margin-bottom: 20px;">🔄 Complete Emergency Alert Flow</h2>
                
                <div class="info-box">
                    <strong>📖 How to Read This Diagram:</strong>
                    <p>Follow the arrows from top to bottom. Each box represents a step in the emergency alert process. Green boxes are successful outcomes, blue boxes are processes, and orange boxes are decisions.</p>
                </div>

                <div class="mermaid">
graph TB
    Start([👤 USER IN DANGER]) --> Trigger{Alert Trigger?}
    
    Trigger -->|🎤 Voice| Voice[VoskWakeWordService<br/>Detects 'help me']
    Trigger -->|👆 Manual| Manual[User Presses<br/>SOS Button]
    
    Voice --> Vibrate[📳 Immediate Feedback<br/>Vibration + Notification]
    Manual --> Vibrate
    
    Vibrate --> DataCollection[📊 DATA COLLECTION<br/>Parallel Execution]
    
    DataCollection --> Location[📍 LocationManager<br/>Get GPS Coordinates<br/>3 Retry Attempts<br/>15s Timeout]
    DataCollection --> Camera[📸 CameraManager<br/>Front Camera Photo<br/>2s Delay<br/>Back Camera Photo]
    DataCollection --> UserData[👤 Get User Profile<br/>Supabase Auth<br/>Fallback: SessionManager]
    
    Location --> LocCheck{Location<br/>Found?}
    LocCheck -->|✅ Yes| LocSuccess[Lat/Long + Accuracy]
    LocCheck -->|❌ No| LocFallback[Use Cached Location<br/>or Proceed Without]
    
    Camera --> CamCheck{Photos<br/>Captured?}
    CamCheck -->|✅ Yes| Photos[Front.jpg + Back.jpg]
    CamCheck -->|❌ No| NoPhotos[Proceed Without Photos]
    
    UserData --> UserCheck{User<br/>Authenticated?}
    UserCheck -->|✅ Yes| UserInfo[Name, Email, Phone]
    UserCheck -->|❌ No| Error[❌ ERROR<br/>User Not Logged In]
    
    LocSuccess --> Upload
    LocFallback --> Upload
    Photos --> Upload[☁️ UPLOAD PHOTOS<br/>Supabase Storage<br/>emergency-photos bucket<br/>Parallel Upload 15s timeout]
    NoPhotos --> CreateAlert
    UserInfo --> Upload
    
    Upload --> UploadCheck{Upload<br/>Success?}
    UploadCheck -->|✅ Yes| PhotoURLs[🔗 Get Public URLs<br/>frontPhotoUrl<br/>backPhotoUrl]
    UploadCheck -->|❌ Timeout| CreateAlert
    
    PhotoURLs --> CreateAlert[💾 CREATE ALERT RECORD<br/>Insert into alert_history<br/>Returns: alertId UUID]
    
    CreateAlert --> QueryGuardians[👨‍👩‍👧‍👦 QUERY GUARDIANS<br/>SELECT * FROM guardians<br/>WHERE user_id = current_user<br/>AND status = 'active']
    
    QueryGuardians --> GuardCheck{Guardians<br/>Found?}
    GuardCheck -->|✅ Yes| GetTokens[🔑 GET FCM TOKENS<br/>For Each Guardian:<br/>Query fcm_tokens table<br/>Filter is_active = true]
    GuardCheck -->|❌ No| NoGuardians[⚠️ WARNING<br/>No Guardians Found<br/>Alert Saved but Not Sent]
    
    GetTokens --> TokenCheck{Tokens<br/>Found?}
    TokenCheck -->|✅ Yes| SendLoop[🔁 FOR EACH GUARDIAN]
    TokenCheck -->|❌ No| NoTokens[⚠️ WARNING<br/>No Active Tokens<br/>Guardians Not Using App]
    
    SendLoop --> Vercel[📤 HTTP POST to Vercel<br/>https://vasatey-notify-msg<br/>.vercel.app/api/sendNotification<br/><br/>Payload:<br/>- FCM Token<br/>- User Details<br/>- Location<br/>- Photo URLs<br/>- Alert ID]
    
    Vercel --> VercelProcess[⚙️ Vercel Function<br/>Validates Input<br/>Uses Firebase Admin SDK]
    
    VercelProcess --> FCM[🔥 FIREBASE CLOUD MESSAGING<br/>Send Data-Only Message<br/>Priority: HIGH<br/>Platform: Android]
    
    FCM --> TrackDB[💾 TRACK IN DATABASE<br/>Insert into alert_recipients<br/>notification_sent = true]
    
    TrackDB --> Guardian[📱 GUARDIAN'S DEVICE<br/>VasateyFCMService<br/>Receives FCM Message]
    
    Guardian --> CreateNotif[🔔 CREATE NOTIFICATION<br/>Title: Emergency Alert<br/>Body: User needs help!<br/>Sound + Vibration]
    
    CreateNotif --> GuardianTap{Guardian<br/>Taps?}
    GuardianTap -->|✅ Yes| OpenAlert[📱 EmergencyAlertViewerActivity<br/><br/>DISPLAYS:<br/>👤 User Name, Email, Phone<br/>📍 Location on Google Maps<br/>📸 Front + Back Photos<br/>📞 Call Button<br/>🗺️ Navigate Button]
    GuardianTap -->|❌ No| NotifStays[Notification Stays<br/>in Status Bar]
    
    OpenAlert --> UpdateViewed[💾 UPDATE DATABASE<br/>alert_recipients<br/>viewed_at = NOW<br/>notification_delivered = true]
    
    UpdateViewed --> Complete[✅ ALERT COMPLETE<br/>Guardian Can Help User]
    
    NoGuardians --> End
    NoTokens --> End
    Error --> End
    NotifStays --> End
    Complete --> End([🎯 END])
    
    style Start fill:#e74c3c,stroke:#c0392b,stroke-width:3px,color:#fff
    style Vibrate fill:#3498db,stroke:#2980b9,stroke-width:2px,color:#fff
    style DataCollection fill:#9b59b6,stroke:#8e44ad,stroke-width:3px,color:#fff
    style Upload fill:#f39c12,stroke:#e67e22,stroke-width:2px,color:#fff
    style CreateAlert fill:#27ae60,stroke:#229954,stroke-width:2px,color:#fff
    style Vercel fill:#e67e22,stroke:#d35400,stroke-width:3px,color:#fff
    style FCM fill:#e74c3c,stroke:#c0392b,stroke-width:2px,color:#fff
    style Guardian fill:#3498db,stroke:#2980b9,stroke-width:2px,color:#fff
    style Complete fill:#27ae60,stroke:#229954,stroke-width:3px,color:#fff
    style Error fill:#e74c3c,stroke:#c0392b,stroke-width:2px,color:#fff
    style End fill:#95a5a6,stroke:#7f8c8d,stroke-width:2px,color:#fff
                </div>

                <div class="legend">
                    <h3>📊 Flow Legend</h3>
                    <div class="legend-grid">
                        <div class="legend-item">
                            <div class="legend-color" style="background: #e74c3c;"></div>
                            <span><strong>Start/Critical Points</strong></span>
                        </div>
                        <div class="legend-item">
                            <div class="legend-color" style="background: #3498db;"></div>
                            <span><strong>Processing Steps</strong></span>
                        </div>
                        <div class="legend-item">
                            <div class="legend-color" style="background: #27ae60;"></div>
                            <span><strong>Success/Database Operations</strong></span>
                        </div>
                        <div class="legend-item">
                            <div class="legend-color" style="background: #f39c12;"></div>
                            <span><strong>External API Calls</strong></span>
                        </div>
                        <div class="legend-item">
                            <div class="legend-color" style="background: #9b59b6;"></div>
                            <span><strong>Parallel Operations</strong></span>
                        </div>
                        <div class="legend-item">
                            <div class="legend-color" style="background: #95a5a6;"></div>
                            <span><strong>End States</strong></span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ALERT TRIGGER FLOW -->
            <div id="alert" class="diagram-container">
                <h2 style="color: #667eea; margin-bottom: 20px;">🚨 Alert Trigger & Data Collection Flow</h2>

                <div class="mermaid">
graph TB
    User([👤 USER IN DANGER])
    
    User --> TriggerType{How to Trigger Alert?}
    
    TriggerType -->|🎤 VOICE| VoiceFlow[Voice Wake Word Detection]
    TriggerType -->|👆 MANUAL| ManualFlow[Manual SOS Button]
    
    VoiceFlow --> VoskService[🎙️ VoskWakeWordService<br/>Foreground Service<br/>Continuously Listening]
    
    VoskService --> VoskModel[📦 Vosk Speech Model<br/>Offline Recognition<br/>No Internet Required]
    
    VoskModel --> WakeWord{Detects<br/>'help me'?}
    
    WakeWord -->|✅ Yes| VoiceDetected[✅ WAKE WORD DETECTED]
    WakeWord -->|❌ No| VoskModel
    
    ManualFlow --> HomeActivity[🏠 HomeActivity<br/>SOS Button Visible]
    
    HomeActivity --> ButtonPress[User Presses<br/>Emergency Button]
    
    ButtonPress --> Confirmation{Show Confirmation<br/>Dialog?}
    
    Confirmation -->|✅ Confirm| ManualConfirm[✅ USER CONFIRMED]
    Confirmation -->|❌ Cancel| Cancelled[❌ Cancelled]
    
    VoiceDetected --> Feedback
    ManualConfirm --> Feedback
    
    Feedback[📳 IMMEDIATE FEEDBACK<br/>Vibration Pattern: 200ms × 3<br/>Notification: Alert Triggered<br/>Toast Message]
    
    Feedback --> StartCollection[🚀 START DATA COLLECTION<br/>3 Parallel Operations]
    
    StartCollection --> Loc[��� LOCATION]
    StartCollection --> Cam[📸 CAMERA]
    StartCollection --> Prof[👤 PROFILE]
    
    Loc --> LocMgr[LocationManager.getCurrentLocation]
    
    LocMgr --> Strategy1[Strategy 1: Last Known Location<br/>Fast - Uses Cached<br/>If < 5min old & < 200m accuracy]
    
    Strategy1 --> S1Check{Recent &<br/>Accurate?}
    S1Check -->|✅ Yes| LocFound[✅ Location Found]
    S1Check -->|❌ No| Strategy2
    
    Strategy2[Strategy 2: Fresh Location<br/>FusedLocationProvider<br/>15s Timeout]
    
    Strategy2 --> S2Check{Location<br/>Obtained?}
    S2Check -->|✅ Yes| LocFound
    S2Check -->|❌ No| Strategy3
    
    Strategy3[Strategy 3: System LocationManager<br/>GPS + Network Providers]
    
    Strategy3 --> S3Check{Location<br/>Found?}
    S3Check -->|✅ Yes| LocFound
    S3Check -->|❌ No| Strategy4
    
    Strategy4[Strategy 4: Passive Provider<br/>Last Location from Any App]
    
    Strategy4 --> S4Check{Location<br/>Available?}
    S4Check -->|✅ Yes| LocFound
    S4Check -->|❌ No| NoLoc[⚠️ No Location<br/>Proceed Without]
    
    Cam --> CamMgr[CameraManager.captureEmergencyPhotos]
    
    CamMgr --> FrontCam[📸 FRONT CAMERA<br/>Camera2 API<br/>Max Retries: 3<br/>Timeout: 15s]
    
    FrontCam --> FrontCheck{Capture<br/>Success?}
    FrontCheck -->|✅ Yes| FrontFile[front.jpg Saved]
    FrontCheck -->|❌ Failed| FrontRetry{Retries<br/>Left?}
    FrontRetry -->|Yes| FrontCam
    FrontRetry -->|No| NoFront[⚠️ No Front Photo]
    
    FrontFile --> Delay[⏱️ 2 Second Delay<br/>Camera Release Time]
    NoFront --> Delay
    
    Delay --> BackCam[📸 BACK CAMERA<br/>Camera2 API<br/>Max Retries: 3<br/>Timeout: 15s]
    
    BackCam --> BackCheck{Capture<br/>Success?}
    BackCheck -->|✅ Yes| BackFile[back.jpg Saved]
    BackCheck -->|❌ Failed| BackRetry{Retries<br/>Left?}
    BackRetry -->|Yes| BackCam
    BackRetry -->|No| NoBack[⚠️ No Back Photo]
    
    Prof --> AuthCheck[Check Authentication]
    
    AuthCheck --> Supabase{Supabase<br/>Session?}
    
    Supabase -->|✅ Active| SupabaseAuth[Use Supabase Auth<br/>currentUserOrNull]
    Supabase -->|❌ Null| SessionMgr
    
    SupabaseAuth --> GetProfile[Query users table<br/>Get name, email, phone]
    
    SessionMgr[Fallback to SessionManager<br/>Local Encrypted Storage]
    
    SessionMgr --> SessionCheck{Session<br/>Valid?}
    SessionCheck -->|✅ Yes| SessionData[Get Cached User Data]
    SessionCheck -->|❌ No| AuthError[❌ ERROR: Not Logged In]
    
    GetProfile --> ProfileData[✅ User Profile Retrieved]
    SessionData --> ProfileData
    
    LocFound --> DataReady
    NoLoc --> DataReady
    FrontFile --> DataReady
    BackFile --> DataReady
    NoFront --> DataReady
    NoBack --> DataReady
    ProfileData --> DataReady
    
    DataReady[✅ DATA COLLECTION COMPLETE<br/>Ready for Upload]
    
    DataReady --> NextStep([➡️ Proceed to Upload & Alert Creation])
    
    Cancelled --> EndFlow
    AuthError --> EndFlow([❌ END - Error])
    
    style User fill:#e74c3c,stroke:#c0392b,stroke-width:3px,color:#fff
    style Feedback fill:#3498db,stroke:#2980b9,stroke-width:2px,color:#fff
    style StartCollection fill:#9b59b6,stroke:#8e44ad,stroke-width:3px,color:#fff
    style DataReady fill:#27ae60,stroke:#229954,stroke-width:3px,color:#fff
    style AuthError fill:#e74c3c,stroke:#c0392b,stroke-width:2px,color:#fff
                </div>
            </div>

            <!-- DATABASE FLOW -->
            <div id="database" class="diagram-container">
                <h2 style="color: #667eea; margin-bottom: 20px;">🗄️ Database Operations Flow</h2>

                <div class="mermaid">
graph TB
    Start([Data Collected<br/>Photos Uploaded])
    
    Start --> CreateAlert[💾 CREATE ALERT RECORD<br/>Insert into alert_history]
    
    CreateAlert --> AlertData["{<br/>user_id: UUID<br/>user_name: 'John Doe'<br/>user_email: 'john@example.com'<br/>user_phone: '+1234567890'<br/>latitude: 37.7749<br/>longitude: -122.4194<br/>location_accuracy: 12.5<br/>front_photo_url: 'https://...'<br/>back_photo_url: 'https://...'<br/>alert_type: 'voice_help'<br/>status: 'sent'<br/>}"]
    
    AlertData --> InsertAlert[INSERT INTO alert_history<br/>RETURNING id]
    
    InsertAlert --> AlertID[✅ Alert Created<br/>alertId: UUID Generated]
    
    AlertID --> QueryGuardians[👨‍👩‍👧‍👦 QUERY GUARDIANS<br/>Get Guardian List]
    
    QueryGuardians --> GuardianQuery["SELECT * FROM guardians<br/>WHERE user_id = '$userId'<br/>AND status = 'active'"]
    
    GuardianQuery --> GuardianResults{Guardians<br/>Found?}
    
    GuardianResults -->|✅ Yes| GuardianList[📋 List of Guardians<br/>guardian_email<br/>guardian_user_id nullable]
    GuardianResults -->|❌ No| NoGuardians[⚠️ No Active Guardians<br/>Alert Saved<br/>No Notifications Sent]
    
    GuardianList --> LoopStart[🔁 FOR EACH GUARDIAN]
    
    LoopStart --> GetTokens[🔑 GET FCM TOKENS<br/>Query fcm_tokens Table]
    
    GetTokens --> TokenQuery["SELECT token FROM fcm_tokens<br/>WHERE user_id = guardian_user_id<br/>AND is_active = true<br/>LIMIT 1"]
    
    TokenQuery --> TokenCheck{Token<br/>Found?}
    
    TokenCheck -->|✅ Yes| TokenFound[✅ FCM Token Retrieved]
    TokenCheck -->|❌ No| NoToken[⚠️ Guardian Has No App<br/>Skip This Guardian]
    
    TokenFound --> CreateRecipient[💾 CREATE RECIPIENT RECORD<br/>Track Notification]
    
    CreateRecipient --> RecipientData["{<br/>alert_id: alertId<br/>guardian_email: 'guardian@...'<br/>guardian_user_id: UUID nullable<br/>fcm_token: 'token_string'<br/>notification_sent: false<br/>notification_delivered: false<br/>viewed_at: null<br/>}"]
    
    RecipientData --> InsertRecipient[INSERT INTO alert_recipients]
    
    InsertRecipient --> RecipientCreated[✅ Recipient Record Created]
    
    RecipientCreated --> SendNotif[📤 SEND NOTIFICATION<br/>Call Vercel API]
    
    SendNotif --> NotifSent{Notification<br/>Sent?}
    
    NotifSent -->|✅ Success| UpdateSent[UPDATE alert_recipients<br/>SET notification_sent = true<br/>WHERE id = recipient_id]
    NotifSent -->|❌ Failed| UpdateFailed[UPDATE alert_recipients<br/>SET notification_sent = false<br/>Log Error]
    
    UpdateSent --> NextGuardian{More<br/>Guardians?}
    UpdateFailed --> NextGuardian
    NoToken --> NextGuardian
    
    NextGuardian -->|Yes| LoopStart
    NextGuardian -->|No| AllSent[✅ ALL GUARDIANS PROCESSED]
    
    AllSent --> Cleanup[🧹 CLEANUP OLD ALERTS<br/>Keep Last 10 Per User]
    
    Cleanup --> DeleteOld["DELETE FROM alert_history<br/>WHERE user_id = '$userId'<br/>
