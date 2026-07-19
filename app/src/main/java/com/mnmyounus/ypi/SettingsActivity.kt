package com.mnmyounus.ypi

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mnmyounus.ypi.data.ForegroundAppResolver
import com.mnmyounus.ypi.data.IconOption
import com.mnmyounus.ypi.data.PermissionStatus
import com.mnmyounus.ypi.data.RetentionPolicy
import com.mnmyounus.ypi.data.SensorLogStore
import com.mnmyounus.ypi.data.ThemePreference
import com.mnmyounus.ypi.databinding.ActivitySettingsBinding

/**
 * SettingsActivity
 *
 * One of YPI's 4 bottom-nav destinations. Holds every configuration
 * surface: the 5 permission steps (overlay, accessibility service,
 * Usage Access, Bluetooth, Location) — now consolidated into one
 * bordered section instead of 5 separate cards — plus theme (light/
 * dark/system), app icon (a fixed set of alternates, see IconOption.kt
 * for why that's the only mechanism Android actually allows), and the
 * log's auto-delete window.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding
    private lateinit var usageResolver: ForegroundAppResolver

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> refreshUI() }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> refreshUI() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        usageResolver = ForegroundAppResolver(this)

        attachPermissionListeners()
        setUpThemeChips()
        setUpIconChips()
        setUpRetentionChips()
        setUpBottomNav()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    // ── Permission steps ───────────────────────────────────────────

    private fun attachPermissionListeners() {
        b.btnGrantOverlay.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        b.btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        b.btnGrantUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        b.btnGrantBluetooth.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                refreshUI()   // already implicitly available pre-31
            }
        }
        b.btnGrantLocation.setOnClickListener {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun refreshUI() {
        val overlayOk = PermissionStatus.overlayGranted(this)
        val serviceOk = PermissionStatus.accessibilityServiceEnabled(this)

        b.icStatus1.setImageResource(statusIcon(overlayOk))
        b.btnGrantOverlay.apply {
            isEnabled = !overlayOk
            setText(if (overlayOk) R.string.granted else R.string.grant)
        }

        b.rowStep2.alpha = if (overlayOk) 1f else 0.38f
        b.icStatus2.setImageResource(statusIcon(serviceOk))
        b.btnEnableService.apply {
            isEnabled = overlayOk && !serviceOk
            setText(if (serviceOk) R.string.enabled else R.string.enable)
        }

        val usageOk = usageResolver.hasPermission()
        b.icStatus3.setImageResource(statusIcon(usageOk))
        b.btnGrantUsageAccess.apply {
            isEnabled = !usageOk
            setText(if (usageOk) R.string.granted else R.string.grant)
        }

        val bluetoothOk = PermissionStatus.bluetoothGranted(this)
        b.icStatus4.setImageResource(statusIcon(bluetoothOk))
        b.btnGrantBluetooth.apply {
            isEnabled = !bluetoothOk
            setText(if (bluetoothOk) R.string.granted else R.string.grant)
        }

        val locationOk = PermissionStatus.locationGranted(this)
        b.icStatus5.setImageResource(statusIcon(locationOk))
        b.btnGrantLocation.apply {
            isEnabled = !locationOk
            setText(if (locationOk) R.string.granted else R.string.grant)
        }
    }

    private fun statusIcon(done: Boolean) =
        if (done) R.drawable.ic_check else R.drawable.ic_pending

    // ── Theme ─────────────────────────────────────────────────────

    private fun setUpThemeChips() {
        updateThemeChipSelection(ThemePreference.get(this))
        b.chipThemeLight.setOnClickListener  { setTheme(ThemePreference.LIGHT) }
        b.chipThemeDark.setOnClickListener   { setTheme(ThemePreference.DARK) }
        b.chipThemeSystem.setOnClickListener { setTheme(ThemePreference.SYSTEM) }
    }

    private fun setTheme(pref: ThemePreference) {
        ThemePreference.set(this, pref)
        pref.apply()   // AppCompatDelegate recreates every visible AppCompatActivity automatically
        updateThemeChipSelection(pref)
    }

    private fun updateThemeChipSelection(pref: ThemePreference) {
        b.chipThemeLight.isChecked  = pref == ThemePreference.LIGHT
        b.chipThemeDark.isChecked   = pref == ThemePreference.DARK
        b.chipThemeSystem.isChecked = pref == ThemePreference.SYSTEM
    }

    // ── App icon ──────────────────────────────────────────────────

    private fun setUpIconChips() {
        updateIconChipSelection(IconOption.get(this))
        b.chipIconDefault.setOnClickListener { setIcon(IconOption.DEFAULT) }
        b.chipIconStealth.setOnClickListener { setIcon(IconOption.STEALTH) }
        b.chipIconMinimal.setOnClickListener { setIcon(IconOption.MINIMAL) }
    }

    private fun setIcon(option: IconOption) {
        IconOption.apply(this, option)
        updateIconChipSelection(option)
    }

    private fun updateIconChipSelection(option: IconOption) {
        b.chipIconDefault.isChecked = option == IconOption.DEFAULT
        b.chipIconStealth.isChecked = option == IconOption.STEALTH
        b.chipIconMinimal.isChecked = option == IconOption.MINIMAL
    }

    // ── Retention ─────────────────────────────────────────────────

    private fun setUpRetentionChips() {
        updateRetentionChipSelection(RetentionPolicy.get(this))
        b.chip1Month.setOnClickListener  { setRetention(RetentionPolicy.ONE_MONTH) }
        b.chip3Months.setOnClickListener { setRetention(RetentionPolicy.THREE_MONTHS) }
        b.chip6Months.setOnClickListener { setRetention(RetentionPolicy.SIX_MONTHS) }
    }

    private fun setRetention(policy: RetentionPolicy) {
        RetentionPolicy.set(this, policy)
        updateRetentionChipSelection(policy)
        SensorLogStore.get(this).pruneNow(policy.millis)
    }

    private fun updateRetentionChipSelection(policy: RetentionPolicy) {
        b.chip1Month.isChecked  = policy == RetentionPolicy.ONE_MONTH
        b.chip3Months.isChecked = policy == RetentionPolicy.THREE_MONTHS
        b.chip6Months.isChecked = policy == RetentionPolicy.SIX_MONTHS
    }

    // ── Bottom navigation ─────────────────────────────────────────

    private fun setUpBottomNav() {
        val nav = b.bottomNavInclude.bottomNav
        nav.selectedItemId = R.id.nav_settings
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> true
                R.id.nav_home -> { navigateTo(MainActivity::class.java); true }
                R.id.nav_logs -> { navigateTo(LogsActivity::class.java); true }
                R.id.nav_insights -> { navigateTo(InsightsActivity::class.java); true }
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
