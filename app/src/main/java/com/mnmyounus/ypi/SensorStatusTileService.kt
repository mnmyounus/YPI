package com.mnmyounus.ypi

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * SensorStatusTileService
 *
 * A Quick Settings tile (pull down the notification shade) showing which
 * sensors are active right now, without opening the app. This is
 * read-only — there's nothing to toggle here, since the badges reflect
 * real hardware state, not a setting. Tapping the tile opens YPI.
 */
class SensorStatusTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val isRunning = PrivacyAccessibilityService.isRunning
        val active = PrivacyAccessibilityService.currentActive

        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_UNAVAILABLE
        tile.label = getString(R.string.app_name)

        // Tile.setSubtitle() was only added in API 29 — guard it for minSdk 26.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !isRunning -> getString(R.string.tile_service_off)
                active.isEmpty() -> getString(R.string.tile_none_active)
                else -> active.joinToString(" ") { it.emoji }
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        // TileService.startActivityAndCollapse(Intent) was removed on API 34+ in
        // favor of the PendingIntent overload — the old call throws there, not
        // just warns, so this is a real version branch, not cosmetic cleanup.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
