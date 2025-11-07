@echo off
echo Installing Debug APK to connected device...
echo.

REM Try common ADB locations
set ADB_PATH=""

if exist "C:\Users\srida\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set ADB_PATH="C:\Users\srida\AppData\Local\Android\Sdk\platform-tools\adb.exe"
) else if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set ADB_PATH="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
) else if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set ADB_PATH="%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
) else (
    echo ERROR: ADB not found!
    echo Please install Android SDK or use Method 1 (manual transfer)
    pause
    exit /b 1
)

echo Found ADB at: %ADB_PATH%
echo.

echo Checking for connected devices...
%ADB_PATH% devices
echo.

echo Installing app-debug.apk...
%ADB_PATH% install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ SUCCESS! Debug APK installed successfully!
) else (
    echo.
    echo ❌ FAILED! Installation failed.
    echo Make sure:
    echo 1. Phone is connected via USB
    echo 2. USB debugging is enabled
    echo 3. You authorized the PC on your phone
)

echo.
pause
