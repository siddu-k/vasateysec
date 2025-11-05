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
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
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

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupLoadMore()
        loadAlerts(refresh = true)
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
        alertAdapter = AlertAdapter(alerts) { alert ->
            openAlertDetails(alert)
        }
        binding.alertsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AlertHistoryActivity)
            adapter = alertAdapter
        }
    }

    private fun loadAlerts(refresh: Boolean = false) {
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

                // Get alerts sent by current user with pagination
                val sentAlerts = try {
                    Log.d("AlertHistory", "Fetching sent alerts...")
                    
                    val result = SupabaseClient.client.from("alert_history")
                        .select {
                            filter {
                                eq("user_id", currentUser.id)
                            }
                        }
                        .decodeList<AlertHistory>()
                        .sortedByDescending { it.created_at }
                        .drop(currentOffset)
                        .take(pageSize)
                    Log.d("AlertHistory", "Loaded ${result.size} sent alerts")
                    result
                } catch (e: Exception) {
                    Log.e("AlertHistory", "Failed to load sent alerts: ${e.message}", e)
                    Toast.makeText(this@AlertHistoryActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    emptyList()
                }

                // Add new alerts to the list
                alerts.addAll(sentAlerts)
                
                // Log first alert to see what we got from DB
                if (sentAlerts.isNotEmpty()) {
                    val first = sentAlerts[0]
                    Log.d("AlertHistory", "First alert from DB: id=${first.id}, lat=${first.latitude}, lon=${first.longitude}")
                }
                
                // Check if there are more alerts to load
                hasMore = sentAlerts.size >= pageSize
                currentOffset += sentAlerts.size

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
        val intent = Intent(this, HelpRequestActivity::class.java).apply {
            putExtra("fullName", alert.user_name)
            putExtra("email", alert.user_email)
            putExtra("phoneNumber", alert.user_phone)
            // Pass as strings to avoid null/0.0 confusion
            putExtra("latitude", alert.latitude?.toString() ?: "")
            putExtra("longitude", alert.longitude?.toString() ?: "")
            putExtra("timestamp", alert.created_at)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

// Alert Adapter
class AlertAdapter(
    private val alerts: List<AlertHistory>,
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
        
        holder.binding.alertUserName.text = alert.user_name
        holder.binding.alertEmail.text = alert.user_email
        holder.binding.alertPhone.text = alert.user_phone
        holder.binding.alertStatus.text = alert.status.uppercase()

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

        holder.itemView.setOnClickListener {
            Log.d("AlertAdapter", "Alert clicked: id=${alert.id}, lat=${alert.latitude}, lon=${alert.longitude}")
            onClick(alert)
        }
    }

    override fun getItemCount() = alerts.size
}
