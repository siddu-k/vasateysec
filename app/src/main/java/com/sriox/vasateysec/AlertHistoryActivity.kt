package com.sriox.vasateysec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sriox.vasateysec.databinding.ActivityAlertHistoryBinding
import com.sriox.vasateysec.models.AlertHistory
import com.sriox.vasateysec.models.AlertRecipient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertHistoryBinding
    private lateinit var alertAdapter: AlertAdapter
    private val alerts = mutableListOf<AlertHistory>()
    private var currentOffset = 0
    private val pageSize = 10
    private var isLoading = false
    private var hasMore = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setSupportActionBar(binding.toolbar) // Removed - using gradient header
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupLoadMore()
        setupBottomNavigation()
        setupFilterButtons()
        
        // Highlight History nav item
        com.sriox.vasateysec.utils.BottomNavHelper.highlightActiveItem(
            this,
            com.sriox.vasateysec.utils.BottomNavHelper.NavItem.HISTORY
        )
        
        // Back button
        binding.backButton.setOnClickListener {
            finish()
        }
        
        loadAlerts(refresh = true)
    }
    
    private fun setupBottomNavigation() {
        val navGuardians = findViewById<android.widget.LinearLayout>(R.id.navGuardians)
        val navHistory = findViewById<android.widget.LinearLayout>(R.id.navHistory)
        val sosButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.sosButton)
        val navGhistory = findViewById<android.widget.LinearLayout>(R.id.navGhistory)
        val navProfile = findViewById<android.widget.LinearLayout>(R.id.navProfile)
        
        navGuardians?.setOnClickListener {
            startActivity(Intent(this, AddGuardianActivity::class.java))
        }
        navHistory?.setOnClickListener { /* Already here */ }
        sosButton?.setOnClickListener {
            com.sriox.vasateysec.utils.SOSHelper.showSOSConfirmation(this)
        }
        navGhistory?.setOnClickListener { /* Already here */ }
        navProfile?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }
    
    private fun setupFilterButtons() {
        binding.btnAllAlerts.setOnClickListener {
            updateFilterSelection(it.id)
            loadAlerts(refresh = true)
        }
        binding.btnSentAlerts.setOnClickListener {
            updateFilterSelection(it.id)
            loadAlerts(refresh = true, filterSent = true)
        }
        binding.btnReceivedAlerts.setOnClickListener {
            updateFilterSelection(it.id)
            loadAlerts(refresh = true, filterReceived = true)
        }
    }
    
    private fun updateFilterSelection(selectedId: Int) {
        // Reset all buttons
        binding.btnAllAlerts.apply {
            setBackgroundColor(getColor(R.color.card_elevated))
            setTextColor(getColor(R.color.text_secondary))
        }
        binding.btnSentAlerts.apply {
            setBackgroundColor(getColor(R.color.card_elevated))
            setTextColor(getColor(R.color.text_secondary))
        }
        binding.btnReceivedAlerts.apply {
            setBackgroundColor(getColor(R.color.card_elevated))
            setTextColor(getColor(R.color.text_secondary))
        }
        
        // Highlight selected
        when (selectedId) {
            R.id.btnAllAlerts -> binding.btnAllAlerts.apply {
                setBackgroundColor(getColor(R.color.violet))
                setTextColor(getColor(R.color.text_primary))
            }
            R.id.btnSentAlerts -> binding.btnSentAlerts.apply {
                setBackgroundColor(getColor(R.color.violet))
                setTextColor(getColor(R.color.text_primary))
            }
            R.id.btnReceivedAlerts -> binding.btnReceivedAlerts.apply {
                setBackgroundColor(getColor(R.color.violet))
                setTextColor(getColor(R.color.text_primary))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Auto-refresh alerts when returning to this activity
        loadAlerts(refresh = true)
    }

    private fun setupLoadMore() {
        binding.loadMoreButton.setOnClickListener {
            if (!isLoading && hasMore) {
                loadAlerts(refresh = false)
            }
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
        alertAdapter = AlertAdapter(alerts, currentUserId) { alert ->
            openAlertDetails(alert)
        }
        binding.alertsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AlertHistoryActivity)
            adapter = alertAdapter
        }
    }

    private fun loadAlerts(refresh: Boolean = false, filterSent: Boolean = false, filterReceived: Boolean = false) {
        if (isLoading) return
        
        lifecycleScope.launch {
            try {
                isLoading = true
                binding.loadingProgress.visibility = View.VISIBLE
                binding.loadMoreButton.visibility = View.GONE
                
                if (refresh) {
                    currentOffset = 0
                    hasMore = true
                    alerts.clear()
                    Log.d("AlertHistory", "Refreshing alerts from start")
                }
                
                Log.d("AlertHistory", "Loading alerts with offset: $currentOffset, limit: $pageSize")
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser == null) {
                    Log.e("AlertHistory", "No user logged in")
                    Toast.makeText(this@AlertHistoryActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    binding.loadingProgress.visibility = View.GONE
                    return@launch
                }

                Log.d("AlertHistory", "Current user: ${currentUser.id}")

                // Fetch both sent and received alerts in parallel
                val sentAlertsDeferred = async {
                    try {
                        Log.d("AlertHistory", "Fetching sent alerts...")
                        SupabaseClient.client.from("alert_history")
                            .select {
                                filter {
                                    eq("user_id", currentUser.id)
                                }
                            }
                            .decodeList<AlertHistory>()
                    } catch (e: Exception) {
                        Log.e("AlertHistory", "Failed to load sent alerts: ${e.message}", e)
                        emptyList()
                    }
                }

                val receivedAlertsDeferred = async {
                    try {
                        Log.d("AlertHistory", "Fetching received alerts (as guardian)...")
                        
                        // First get alert_recipients where current user is the guardian
                        val recipients = SupabaseClient.client.from("alert_recipients")
                            .select {
                                filter {
                                    eq("guardian_user_id", currentUser.id)
                                }
                            }
                            .decodeList<AlertRecipient>()
                        
                        Log.d("AlertHistory", "Found ${recipients.size} alerts where user is guardian")
                        
                        // Get the alert IDs
                        val alertIds = recipients.mapNotNull { it.alert_id }.distinct()
                        
                        if (alertIds.isEmpty()) {
                            emptyList()
                        } else {
                            // Fetch the actual alerts
                            val alerts = SupabaseClient.client.from("alert_history")
                                .select {
                                    filter {
                                        isIn("id", alertIds)
                                    }
                                }
                                .decodeList<AlertHistory>()
                            Log.d("AlertHistory", "Loaded ${alerts.size} received alerts")
                            alerts
                        }
                    } catch (e: Exception) {
                        Log.e("AlertHistory", "Failed to load received alerts: ${e.message}", e)
                        emptyList()
                    }
                }

                // Wait for both queries to complete
                val sentAlerts = sentAlertsDeferred.await()
                val receivedAlerts = receivedAlertsDeferred.await()
                
                Log.d("AlertHistory", "Sent alerts: ${sentAlerts.size}, Received alerts: ${receivedAlerts.size}")

                // Filter based on user selection
                val filteredAlerts = when {
                    filterSent -> sentAlerts
                    filterReceived -> receivedAlerts
                    else -> (sentAlerts + receivedAlerts).distinctBy { it.id }
                }
                
                Log.d("AlertHistory", "Filtered alerts: ${filteredAlerts.size} (sent=$filterSent, received=$filterReceived)")

                // Sort all alerts by timestamp
                val allAlerts = filteredAlerts
                    .sortedByDescending { it.created_at }
                    .drop(currentOffset)
                    .take(pageSize)

                // Add new alerts to the list
                alerts.addAll(allAlerts)
                
                // Log first alert to see what we got from DB
                if (allAlerts.isNotEmpty()) {
                    val first = allAlerts[0]
                    Log.d("AlertHistory", "First alert from DB: id=${first.id}, lat=${first.latitude}, lon=${first.longitude}")
                }
                
                // Check if there are more alerts to load
                hasMore = allAlerts.size >= pageSize
                currentOffset += allAlerts.size

                Log.d("AlertHistory", "Total alerts: ${alerts.size}, hasMore: $hasMore")

                // Update UI
                if (alerts.isEmpty()) {
                    Log.d("AlertHistory", "No alerts found, showing empty view")
                    binding.emptyView.visibility = View.VISIBLE
                    binding.alertsRecyclerView.visibility = View.GONE
                    binding.loadMoreButton.visibility = View.GONE
                } else {
                    Log.d("AlertHistory", "Showing ${alerts.size} alerts")
                    binding.emptyView.visibility = View.GONE
                    binding.alertsRecyclerView.visibility = View.VISIBLE
                    binding.loadMoreButton.visibility = if (hasMore) View.VISIBLE else View.GONE
                }

                alertAdapter.notifyDataSetChanged()
                Log.d("AlertHistory", "Alert list updated")

            } catch (e: Exception) {
                Log.e("AlertHistory", "Failed to load alerts", e)
                Toast.makeText(this@AlertHistoryActivity, "Failed to load alerts: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
                binding.loadingProgress.visibility = View.GONE
            }
        }
    }

    private fun openAlertDetails(alert: AlertHistory) {
        Log.d("AlertHistory", "Opening alert details: lat=${alert.latitude}, lon=${alert.longitude}")
        Log.d("AlertHistory", "Photo URLs: front=${alert.front_photo_url}, back=${alert.back_photo_url}")
        val intent = Intent(this, HelpRequestActivity::class.java).apply {
            putExtra("fullName", alert.user_name)
            putExtra("email", alert.user_email)
            putExtra("phoneNumber", alert.user_phone)
            // Pass as strings to avoid null/0.0 confusion
            putExtra("latitude", alert.latitude?.toString() ?: "")
            putExtra("longitude", alert.longitude?.toString() ?: "")
            putExtra("timestamp", alert.created_at)
            // Pass photo URLs
            putExtra("frontPhotoUrl", alert.front_photo_url ?: "")
            putExtra("backPhotoUrl", alert.back_photo_url ?: "")
        }
        startActivity(intent)
    }

    // Removed - using back button in gradient header
    // override fun onSupportNavigateUp(): Boolean {
    //     finish()
    //     return true
    // }
}

// Alert Adapter
class AlertAdapter(
    private val alerts: List<AlertHistory>,
    private val currentUserId: String,
    private val onClick: (AlertHistory) -> Unit
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    class AlertViewHolder(val binding: com.sriox.vasateysec.databinding.ItemAlertBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): AlertViewHolder {
        val binding = com.sriox.vasateysec.databinding.ItemAlertBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        
        // Determine if this is a sent or received alert
        val isSentByMe = alert.user_id == currentUserId
        
        holder.binding.alertUserName.text = alert.user_name
        holder.binding.alertEmail.text = alert.user_email
        holder.binding.alertPhone.text = alert.user_phone
        holder.binding.alertStatus.text = alert.status.uppercase()
        
        // Set alert type badge
        if (isSentByMe) {
            holder.binding.alertType.text = "📤 Sent"
            holder.binding.alertType.setBackgroundColor(
                android.graphics.Color.parseColor("#2196F3") // Blue
            )
        } else {
            holder.binding.alertType.text = "📥 Received"
            holder.binding.alertType.setBackgroundColor(
                android.graphics.Color.parseColor("#4CAF50") // Green
            )
        }

        if (alert.latitude != null && alert.longitude != null) {
            holder.binding.alertLocation.text = "📍 Lat: %.4f, Long: %.4f".format(alert.latitude, alert.longitude)
        } else {
            holder.binding.alertLocation.text = "📍 Location unavailable"
        }

        // Format time
        alert.created_at?.let { timestamp ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")  // Parse as UTC
                val date = inputFormat.parse(timestamp)
                val now = Date()
                val diff = now.time - (date?.time ?: 0)
                
                val timeText = when {
                    diff < 60000 -> "Just now"
                    diff < 3600000 -> "${diff / 60000} minutes ago"
                    diff < 86400000 -> "${diff / 3600000} hours ago"
                    else -> "${diff / 86400000} days ago"
                }
                holder.binding.alertTime.text = timeText
            } catch (e: Exception) {
                holder.binding.alertTime.text = "Recently"
            }
        }
        
        // Load emergency photos if available
        val hasFrontPhoto = !alert.front_photo_url.isNullOrEmpty()
        val hasBackPhoto = !alert.back_photo_url.isNullOrEmpty()
        
        if (hasFrontPhoto || hasBackPhoto) {
            holder.binding.photosContainer.visibility = View.VISIBLE
            
            if (hasFrontPhoto) {
                com.bumptech.glide.Glide.with(holder.itemView.context)
                    .load(alert.front_photo_url)
                    .into(holder.binding.frontPhotoThumb)
                holder.binding.frontPhotoThumb.visibility = View.VISIBLE
            } else {
                holder.binding.frontPhotoThumb.visibility = View.GONE
            }
            
            if (hasBackPhoto) {
                com.bumptech.glide.Glide.with(holder.itemView.context)
                    .load(alert.back_photo_url)
                    .into(holder.binding.backPhotoThumb)
                holder.binding.backPhotoThumb.visibility = View.VISIBLE
            } else {
                holder.binding.backPhotoThumb.visibility = View.GONE
            }
        } else {
            holder.binding.photosContainer.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            Log.d("AlertAdapter", "Alert clicked: id=${alert.id}, lat=${alert.latitude}, lon=${alert.longitude}")
            onClick(alert)
        }
    }

    override fun getItemCount() = alerts.size
}
