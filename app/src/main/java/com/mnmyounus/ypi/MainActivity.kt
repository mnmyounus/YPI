package com.mnmyounus.ypi

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mnmyounus.ypi.data.ForegroundAppResolver
import com.mnmyounus.ypi.data.PermissionStatus
import com.mnmyounus.ypi.databinding.ActivityMainBinding

/**
 * MainActivity — Home
 *
 * The launcher screen and first of YPI's 3 bottom-nav destinations.
 * Deliberately light: branding, the core active/inactive status banner
 * (gated only on overlay + accessibility service, same as always — the
 * other 3 permissions only improve log attribution, they don't affect
 * whether the badges themselves work), a nudge toward Settings if setup
 * isn't finished, and the emoji legend. All 5 permission switches live
 * in Settings now, not here.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var usageResolver: ForegroundAppResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        usageResolver = ForegroundAppResolver(this)

        b.btnGoToSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        setUpBottomNav()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun refreshUI() {
        val coreReady = PermissionStatus.coreReady(this)

        b.statusBanner.backgroundTintList = ColorStateList.valueOf(
            getColor(if (coreReady) R.color.color_camera else R.color.color_inactive)
        )
        b.tvStatus.setText(if (coreReady) R.string.status_active else R.string.status_incomplete)

        val granted = listOf(
            PermissionStatus.overlayGranted(this),
            PermissionStatus.accessibilityServiceEnabled(this),
            usageResolver.hasPermission(),
            PermissionStatus.bluetoothGranted(this),
            PermissionStatus.locationGranted(this)
        ).count { it }

        val allFive = granted == 5
        b.cardSetupProgress.visibility = if (allFive) View.GONE else View.VISIBLE
        if (!allFive) {
            b.tvSetupProgress.text = getString(R.string.setup_progress_count, granted, 5)
        }
    }

    // ── Bottom navigation ─────────────────────────────────────────

    private fun setUpBottomNav() {
        val nav = b.bottomNavInclude.bottomNav
        nav.selectedItemId = R.id.nav_home
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
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
