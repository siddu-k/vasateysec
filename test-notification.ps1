# Test FCM Notification Script
# This sends a test notification to a specific FCM token

$fcmToken = "exwyMoJgRP-yYKtqWEDVMn:APA91bFuJsYrLLPLB3MaQPA6OkG6hM_LPyJYvwOIg47_stWqXbKv1a__rwafd5QF_TA-TD0D8DZexTruB48VacutJ4JRSVTJD1Zqx0Q0qHvui7DRFvg0p_w"
$vercelEndpoint = "https://vasatey-notify-msg.vercel.app/api/sendNotification"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing FCM Notification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Token: $($fcmToken.Substring(0, 30))..." -ForegroundColor Yellow
Write-Host "Endpoint: $vercelEndpoint" -ForegroundColor Yellow
Write-Host ""

# Create the request body
$body = @{
    token = $fcmToken
    title = "🧪 Test Notification"
    body = "This is a test notification from PowerShell script"
    email = "test@example.com"
    isSelfAlert = $false
    fullName = "Test User"
    phoneNumber = "0000000000"
    lastKnownLatitude = $null
    lastKnownLongitude = $null
    frontPhotoUrl = ""
    backPhotoUrl = ""
} | ConvertTo-Json

Write-Host "Request Body:" -ForegroundColor Green
Write-Host $body -ForegroundColor Gray
Write-Host ""

try {
    Write-Host "Sending notification..." -ForegroundColor Yellow
    
    $response = Invoke-WebRequest -Uri $vercelEndpoint -Method Post -Body $body -ContentType "application/json"
    
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✅ SUCCESS!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Status Code: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Green
    Write-Host $response.Content -ForegroundColor White
    
} catch {
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "❌ FAILED!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    
    if ($_.Exception.Response) {
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response Body:" -ForegroundColor Yellow
            Write-Host $responseBody -ForegroundColor White
        } catch {
            Write-Host "Could not read response body" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "This likely means:" -ForegroundColor Yellow
    Write-Host "  - The FCM token is invalid or expired" -ForegroundColor White
    Write-Host "  - The device needs to log in again to get a fresh token" -ForegroundColor White
}

Write-Host ""
Write-Host "Test complete!" -ForegroundColor Cyan
