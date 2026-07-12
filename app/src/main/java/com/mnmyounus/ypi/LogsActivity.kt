package com.mnmyounus.ypi

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.mnmyounus.ypi.data.ForegroundAppResolver
import com.mnmyounus.ypi.data.SensorLogEntry
import com.mnmyounus.ypi.data.SensorLogStore
import com.mnmyounus.ypi.data.SensorType
import com.mnmyounus.ypi.databinding.ActivityLogsBinding

/**
 * LogsActivity
 *
 * One of YPI's 3 bottom-nav destinations. Shows the encrypted, on-device
 * sensor activity log: which app was likely (or, for WiFi, genuinely
 * measured) responsible for each of the 6 sensors activating, and for
 * how long. Filterable by sensor type. Auto-delete retention lives in
 * Settings, not here — this screen is purely for browsing.
 */
class LogsActivity : AppCompatActivity() {

    private lateinit var b: ActivityLogsBinding
    private lateinit var resolver: ForegroundAppResolver
    private val adapter = LogAdapter()

    private var allEntries: List<SensorLogEntry> = emptyList()
    private var activeFilter: SensorType? = null   // null = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(b.root)
        resolver = ForegroundAppResolver(this)

        b.recyclerLog.layoutManager = LinearLayoutManager(this)
        b.recyclerLog.adapter = adapter

        b.btnGrantUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        b.btnClearLog.setOnClickListener {
            SensorLogStore.get(this).clearAll()
            allEntries = emptyList()
            applyFilter()
        }

        setUpFilterChips()
        setUpBottomNav()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionBanner()
        reloadLog()
    }

    // ── Sensor-type filter ───────────────────────────────────────

    private fun setUpFilterChips() {
        addFilterChip(getString(R.string.filter_all), null)
        SensorType.values().forEach { type -> addFilterChip("${type.emoji} ${type.displayLabel}", type) }
        (b.filterChips.getChildAt(0) as? Chip)?.isChecked = true
    }

    private fun addFilterChip(label: String, type: SensorType?) {
        val chip = Chip(this).apply {
            text = label
            isCheckable = true
            isClickable = true
            setOnClickListener {
                activeFilter = type
                applyFilter()
            }
        }
        b.filterChips.addView(chip)
    }

    private fun applyFilter() {
        val filtered = activeFilter
            ?.let { type -> allEntries.filter { it.sensorType == type } }
            ?: allEntries
        val sorted = filtered.sortedByDescending { it.startMillis }
        adapter.submit(sorted)
        b.emptyState.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    // ── Data refresh ──────────────────────────────────────────────

    private fun refreshPermissionBanner() {
        b.usageAccessBanner.visibility = if (resolver.hasPermission()) View.GONE else View.VISIBLE
    }

    private fun reloadLog() {
        SensorLogStore.get(this).loadAll { entries ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                allEntries = entries
                applyFilter()
            }
        }
    }

    // ── Bottom navigation ─────────────────────────────────────────

    private fun setUpBottomNav() {
        val nav = b.bottomNavInclude.bottomNav
        nav.selectedItemId = R.id.nav_logs
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logs -> true
                R.id.nav_home -> { navigateTo(MainActivity::class.java); true }
                R.id.nav_settings -> { navigateTo(SettingsActivity::class.java); true }
                else -> false
            }
        }
    }

    private fun navigateTo(target: Class<*>) {
        startActivity(Intent(this, target))
        overridePendingTransition(0, 0)
        finish()
    }
}
