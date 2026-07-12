package com.mnmyounus.ypi.data

import org.json.JSONObject

/**
 * SensorLogEntry
 *
 * One completed sensor-usage session: a sensor that turned on and later
 * turned off, plus a best-effort guess at which app was active.
 *
 * [packageName] is null when Usage Access hasn't been granted, or when no
 * app could be resolved at the moment the sensor activated. [transport]
 * is only meaningful for NETWORK entries ("WIFI" or "MOBILE"); it's null
 * for every other sensor and for NETWORK entries logged before this field
 * existed (kept backward-compatible on read).
 */
data class SensorLogEntry(
    val id: Long,
    val sensor: String,          // SensorType.storageKey
    val packageName: String?,
    val appLabel: String,
    val startMillis: Long,
    val endMillis: Long,
    val transport: String? = null
) {
    val sensorType: SensorType get() = SensorType.fromStorageKey(sensor)
    val durationMillis: Long get() = (endMillis - startMillis).coerceAtLeast(0L)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("sensor", sensor)
        put("pkg", packageName ?: JSONObject.NULL)
        put("label", appLabel)
        put("start", startMillis)
        put("end", endMillis)
        put("transport", transport ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): SensorLogEntry = SensorLogEntry(
            id          = o.getLong("id"),
            sensor      = o.getString("sensor"),
            packageName = if (o.isNull("pkg")) null else o.getString("pkg"),
            appLabel    = o.getString("label"),
            startMillis = o.getLong("start"),
            endMillis   = o.getLong("end"),
            transport   = if (!o.has("transport") || o.isNull("transport")) null else o.getString("transport")
        )
    }
}
