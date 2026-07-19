package com.mnmyounus.ypi

import android.content.Intent
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import com.mnmyounus.ypi.data.BatteryReader
import com.mnmyounus.ypi.data.DataUsageReader
import com.mnmyounus.ypi.data.ForegroundAppResolver
import com.mnmyounus.ypi.data.UsageCounters
import com.mnmyounus.ypi.databinding.ActivityInsightsBinding

/**
 * InsightsActivity
 *
 * The 4th bottom-nav destination: app-open/lock/unlock counters, overall
 * device battery, and today's real per-app data usage (WiFi genuinely
 * measured; mobile data measured where the OS/OEM combination allows it).
 */
class InsightsActivity : AppCompatActivity() {

    private lateinit var b: ActivityInsightsBinding
    private lateinit var usageResolver: ForegroundAppResolver
    private lateinit var batteryReader: BatteryReader
    private lateinit var dataUsageReader: DataUsageReader
    private val adapter = DataUsageAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityInsightsBinding.inflate(layoutInflater)
        setContentView(b.root)

        usageResolver = ForegroundAppResolver(this)
        batteryReader = BatteryReader(this)
        dataUsageReader = DataUsageReader(this)

        b.recyclerDataUsage.layoutManager = LinearLayoutManager(this)
        b.recyclerDataUsage.adapter = adapter

        b.btnGrantUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        setUpBottomNav()
    }

    override fun onResume() {
        super.onResume()
        refreshActivityCounters()
        refreshBattery()
        refreshDataUsage()
    }

    private fun refreshActivityCounters() {
        b.tvAppOpens.text = UsageCounters.appOpenCount(this).toString()
        b.tvLockCount.text = UsageCounters.lockCount(this).toString()
        b.tvUnlockCount.text = UsageCounters.unlockCount(this).toString()
    }

    private fun refreshBattery() {
        val state = batteryReader.currentState()
        val levelText = if (state.percent >= 0) "${state.percent}%" else getString(R.string.insights_battery_unknown)
        b.tvBatteryLevel.text = if (state.isCharging) {
            getString(R.string.insights_battery_charging, levelText)
        } else {
            levelText
        }
    }

    private fun refreshDataUsage() {
        val hasAccess = usageResolver.hasPermission()
        b.usageAccessBanner.visibility = if (hasAccess) View.GONE else View.VISIBLE

        if (!hasAccess) {
            adapter.submit(emptyList())
            b.recyclerDataUsage.visibility = View.GONE
            b.emptyDataUsage.visibility = View.GONE
            return
        }

        val usage = dataUsageReader.todayUsageByApp()
        adapter.submit(usage)
        b.recyclerDataUsage.visibility = if (usage.isEmpty()) View.GONE else View.VISIBLE
        b.emptyDataUsage.visibility = if (usage.isEmpty()) View.VISIBLE else View.GONE
    }

    // ── Bottom navigation ─────────────────────────────────────────

    private fun setUpBottomNav() {
        val nav = b.bottomNavInclude.bottomNav
        nav.selectedItemId = R.id.nav_insights
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_insights -> true
                R.id.nav_home -> { navigateTo(MainActivity::class.java); true }
                R.id.nav_logs -> { navigateTo(LogsActivity::class.java); true }
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
