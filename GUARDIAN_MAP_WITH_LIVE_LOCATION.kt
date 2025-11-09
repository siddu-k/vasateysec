// REFERENCE IMPLEMENTATION
// This shows how to integrate LiveLocationHelper into GuardianMapActivity
// Copy the relevant parts into your actual GuardianMapActivity.kt

// Add this import at the top:
import com.sriox.vasateysec.utils.LiveLocationHelper

// Replace the loadTrackedUsersLocations() function with this:

private fun loadTrackedUsersLocations() {
    lifecycleScope.launch {
        try {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            if (currentUser == null) {
                Toast.makeText(
                    this@GuardianMapActivity,
                    "Please login to view guardian locations",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            // Get current user's email
            val currentUserEmail = currentUser.email
            
            // Find all users who added ME as their guardian (reverse lookup)
            val usersWhoAddedMe = SupabaseClient.client.from("guardians")
                .select {
                    filter {
                        eq("guardian_email", currentUserEmail ?: "")
                    }
                }
                .decodeList<com.sriox.vasateysec.models.Guardian>()

            android.util.Log.d("GuardianMap", "Found ${usersWhoAddedMe.size} users who added me as guardian")

            // ========== NEW: Request live locations ==========
            android.util.Log.d("GuardianMap", "📍 Requesting live locations...")
            val requestedUserIds = LiveLocationHelper.requestLiveLocations(this@GuardianMapActivity)
            android.util.Log.d("GuardianMap", "📍 Sent requests to ${requestedUserIds.size} users")
            
            // Wait for users to respond (3 seconds)
            Toast.makeText(this@GuardianMapActivity, "Requesting live locations...", Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(3000)
            
            // Fetch live locations
            val liveLocations = LiveLocationHelper.fetchLiveLocations(requestedUserIds)
            android.util.Log.d("GuardianMap", "📍 Received ${liveLocations.size} live locations")
            // ==================================================

            // For each user who added me, fetch their location
            usersWhoAddedMe.forEach { guardianRelation ->
                val userId = guardianRelation.user_id
                
                try {
                    // Get user details
                    val users = SupabaseClient.client.from("users")
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeList<com.sriox.vasateysec.models.User>()

                    if (users.isNotEmpty()) {
                        val user = users[0]
                        val userName = user.name
                        
                        // ========== NEW: Try live location first ==========
                        val liveLocation = liveLocations[userId]
                        
                        if (liveLocation != null) {
                            // Use LIVE location (green marker)
                            addUserMarker(
                                userName, 
                                liveLocation.latitude, 
                                liveLocation.longitude,
                                isLive = true
                            )
                            android.util.Log.d("GuardianMap", "🟢 Added LIVE marker for $userName")
                        } else {
                            // Fallback to alert history (orange marker)
                            val alerts = SupabaseClient.client.from("alert_history")
                                .select {
                                    filter {
                                        eq("user_id", userId)
                                    }
                                }
                                .decodeList<com.sriox.vasateysec.models.AlertHistory>()

                            if (alerts.isNotEmpty()) {
                                val recentAlert = alerts
                                    .sortedByDescending { it.created_at }
                                    .firstOrNull { it.latitude != null && it.longitude != null }

                                recentAlert?.let { alert ->
                                    val lat = alert.latitude
                                    val lng = alert.longitude

                                    if (lat != null && lng != null) {
                                        addUserMarker(userName, lat, lng, isLive = false)
                                        android.util.Log.d("GuardianMap", "🟠 Added alert history marker for $userName")
                                    }
                                }
                            }
                        }
                        // ==================================================
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GuardianMap", "Error loading location for user $userId", e)
                }
            }

            // If we have markers, adjust camera to show all
            if (markers.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.Builder()
                markers.forEach { marker ->
                    boundsBuilder.include(marker.position)
                }
                val bounds = boundsBuilder.build()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } else {
                Toast.makeText(
                    this@GuardianMapActivity,
                    "No tracked users found. Users need to add you as their guardian.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("GuardianMap", "Error loading guardians", e)
            Toast.makeText(
                this@GuardianMapActivity,
                "Failed to load guardian locations: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

// Update addUserMarker to support live/old distinction:
private fun addUserMarker(name: String, latitude: Double, longitude: Double, isLive: Boolean = false) {
    val position = LatLng(latitude, longitude)
    
    // Green for live, orange for old
    val markerColor = if (isLive) {
        BitmapDescriptorFactory.HUE_GREEN
    } else {
        BitmapDescriptorFactory.HUE_ORANGE
    }
    
    val snippet = if (isLive) {
        "Live location (just now)"
    } else {
        "Last known location"
    }
    
    val marker = googleMap.addMarker(
        MarkerOptions()
            .position(position)
            .title(name)
            .snippet(snippet)
            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
    )
    
    marker?.let { markers.add(it) }
    
    // Add a circle around the marker
    googleMap.addCircle(
        CircleOptions()
            .center(position)
            .radius(500.0) // 500 meters
            .strokeColor(Color.parseColor(if (isLive) "#00FF00" else "#FF6B35"))
            .fillColor(Color.parseColor(if (isLive) "#2200FF00" else "#22FF6B35"))
            .strokeWidth(2f)
    )
}
