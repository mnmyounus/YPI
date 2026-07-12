package com.mnmyounus.ypi.data

import android.graphics.Color

/**
 * SensorType
 *
 * The six categories YPI watches. Camera, Mic, and Audio are detected via
 * exact system event callbacks. Network and Bluetooth are detected via a
 * light poll / system broadcast respectively (see NetworkMonitor and
 * BluetoothMonitor). Location is detected via GnssStatus.Callback, which
 * reports GPS-only positioning — apps using pure WiFi/cell-tower location
 * without GPS won't trigger it (see LocationMonitor).
 *
 * [emoji] is the one glyph used everywhere this sensor is represented —
 * the overlay badge, the log list, the legend, and Settings — so every
 * screen stays visually consistent by construction, not by convention.
 * NETWORK's default emoji is the WiFi state; SensorIndicatorView and
 * LogAdapter both special-case it to 📱 when the transport is MOBILE.
 */
enum class SensorType(
    val storageKey: String,
    val color: Int,
    val displayLabel: String,
    val emoji: String
) {
    CAMERA("CAMERA", Color.parseColor("#34C759"), "Camera", "📷"),
    MIC("MIC", Color.parseColor("#FF9500"), "Microphone", "🎤"),
    AUDIO("AUDIO", Color.parseColor("#007AFF"), "Audio", "🔊"),
    NETWORK("NETWORK", Color.parseColor("#AF52DE"), "Network", "📶"),
    BLUETOOTH("BLUETOOTH", Color.parseColor("#00BFA5"), "Bluetooth", "🎧"),
    LOCATION("LOCATION", Color.parseColor("#FF3B30"), "Location", "📍");

    companion object {
        fun fromStorageKey(key: String): SensorType =
            values().firstOrNull { it.storageKey == key } ?: CAMERA
    }
}
