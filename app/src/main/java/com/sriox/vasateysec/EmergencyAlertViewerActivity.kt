package com.sriox.vasateysec

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sriox.vasateysec.databinding.ActivityHelpRequestBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Standalone activity to view emergency alerts from notifications
 * Does NOT require login - works independently
 */
class EmergencyAlertViewerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityHelpRequestBinding
    private var googleMap: GoogleMap? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var phoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d("EmergencyViewer", "========================================")
        Log.d("EmergencyViewer", "EmergencyAlertViewerActivity CREATED!")
        Log.d("EmergencyViewer", "Intent action: ${intent.action}")
        Log.d("EmergencyViewer", "Intent flags: ${intent.flags}")
        Log.d("EmergencyViewer", "========================================")
        
        // Load alert data directly - no login check
        setupUI()
    }
    
    private fun setupUI() {
        // Get data from intent
        val fullName = intent.getStringExtra("fullName") ?: "Unknown"
        val email = intent.getStringExtra("email") ?: "Unknown"
        phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
        
        // Get location as strings and convert to Double
        val latStr = intent.getStringExtra("latitude") ?: ""
        val lonStr = intent.getStringExtra("longitude") ?: ""
        
        latitude = latStr.toDoubleOrNull()
        longitude = lonStr.toDoubleOrNull()
        
        val timestamp = intent.getStringExtra("timestamp") ?: ""
        
        Log.d("EmergencyViewer", "Alert data: name=$fullName, lat=$latStr, lon=$lonStr")

        // Set data to views
        binding.userName.text = fullName
        binding.userEmail.text = email
        binding.userPhone.text = phoneNumber

        // Format timestamp
        if (timestamp.isNotEmpty()) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                val date = inputFormat.parse(timestamp)
                binding.alertTime.text = date?.let { outputFormat.format(it) } ?: "Just now"
            } catch (e: Exception) {
                binding.alertTime.text = "Just now"
            }
        } else {
            binding.alertTime.text = "Just now"
        }

        // Set location coordinates
        if (latitude != null && longitude != null) {
            binding.locationCoordinates.text = "Lat: $latitude, Long: $longitude"
            Log.d("EmergencyViewer", "Displaying location: $latitude, $longitude")
        } else {
            binding.locationCoordinates.text = "Location not available"
            Log.w("EmergencyViewer", "No location to display")
        }

        // Setup map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Setup buttons
        binding.callButton.setOnClickListener {
            if (!phoneNumber.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(intent)
            }
        }

        binding.navigateButton.setOnClickListener {
            if (latitude != null && longitude != null) {
                val uri = Uri.parse("google.navigation:q=$latitude,$longitude")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Fallback to browser
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
                    )
                    startActivity(browserIntent)
                }
            }
        }
        
        // Load emergency photos if available
        val frontPhotoUrl = intent.getStringExtra("frontPhotoUrl") ?: ""
        val backPhotoUrl = intent.getStringExtra("backPhotoUrl") ?: ""
        
        Log.d("EmergencyViewer", "Photo URLs: front=$frontPhotoUrl, back=$backPhotoUrl")
        
        var hasPhotos = false
        
        if (frontPhotoUrl.isNotEmpty() && frontPhotoUrl != "null") {
            Log.d("EmergencyViewer", "Loading front camera photo...")
            binding.frontPhotoCard.visibility = View.VISIBLE
            Glide.with(this)
                .load(frontPhotoUrl)
                .into(binding.frontPhoto)
            
            // Click to view full image
            binding.frontPhoto.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(frontPhotoUrl))
                startActivity(intent)
            }
            hasPhotos = true
        }
        
        if (backPhotoUrl.isNotEmpty() && backPhotoUrl != "null") {
            Log.d("EmergencyViewer", "Loading back camera photo...")
            binding.backPhotoCard.visibility = View.VISIBLE
            Glide.with(this)
                .load(backPhotoUrl)
                .into(binding.backPhoto)
            
            // Click to view full image
            binding.backPhoto.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(backPhotoUrl))
                startActivity(intent)
            }
            hasPhotos = true
        }
        
        if (hasPhotos) {
            binding.photosHeader.visibility = View.VISIBLE
            binding.photosContainer.visibility = View.VISIBLE
            Log.d("EmergencyViewer", "Emergency photos displayed")
        } else {
            Log.d("EmergencyViewer", "No emergency photos available")
        }
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("EmergencyViewer", "Map ready")
        googleMap = map
        
        // Set satellite map type
        googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE

        if (latitude != null && longitude != null) {
            Log.d("EmergencyViewer", "Setting map location: $latitude, $longitude")
            val location = LatLng(latitude!!, longitude!!)
            
            // Add marker
            googleMap?.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Emergency Location")
                    .snippet("Person needs help here")
            )

            // Move camera
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(location, 15f)
            )

            // Enable zoom controls
            googleMap?.uiSettings?.isZoomControlsEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            
            Log.d("EmergencyViewer", "Map configured successfully")
        } else {
            Log.w("EmergencyViewer", "Cannot show map: latitude or longitude is null")
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Just finish this activity
        finish()
    }
}
