package com.mnmyounus.ypi.data

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * RetentionPolicy
 *
 * User-selectable auto-delete window for the encrypted sensor activity log.
 * The selected window itself is not sensitive data, so it's stored in plain
 * SharedPreferences — only the log content is encrypted (see SensorLogStore).
 */
enum class RetentionPolicy(val months: Int) {
    ONE_MONTH(1),
    THREE_MONTHS(3),
    SIX_MONTHS(6);

    /** Approximate month length (30 days) — fine for a coarse retention window. */
    val millis: Long get() = TimeUnit.DAYS.toMillis(months * 30L)

    companion object {
        private const val PREFS_NAME    = "ypi_settings"
        private const val KEY_RETENTION = "retention_months"
        private val DEFAULT = THREE_MONTHS

        fun get(context: Context): RetentionPolicy {
            val prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val months = prefs.getInt(KEY_RETENTION, DEFAULT.months)
            return values().firstOrNull { it.months == months } ?: DEFAULT
        }

        fun set(context: Context, policy: RetentionPolicy) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_RETENTION, policy.months)
                .apply()
        }
    }
}
