package com.mnmyounus.ypi.data

import android.content.Context

/**
 * UsageCounters
 *
 * Three simple all-time running totals, persisted in SharedPreferences:
 *  - appOpenCount: incremented once per genuine app open — see
 *    YpiApplication, which uses ProcessLifecycleOwner so switching between
 *    Home/Logs/Settings/Insights tabs never inflates this; only actually
 *    leaving YPI and coming back to it does.
 *  - lockCount / unlockCount: incremented by ScreenLockMonitor on
 *    ACTION_SCREEN_OFF / ACTION_USER_PRESENT respectively.
 */
object UsageCounters {
    private const val PREFS_NAME = "ypi_usage_counters"
    private const val KEY_APP_OPENS = "app_opens"
    private const val KEY_LOCKS = "locks"
    private const val KEY_UNLOCKS = "unlocks"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun incrementAppOpen(context: Context) = increment(context, KEY_APP_OPENS)
    fun incrementLock(context: Context) = increment(context, KEY_LOCKS)
    fun incrementUnlock(context: Context) = increment(context, KEY_UNLOCKS)

    fun appOpenCount(context: Context): Int = prefs(context).getInt(KEY_APP_OPENS, 0)
    fun lockCount(context: Context): Int = prefs(context).getInt(KEY_LOCKS, 0)
    fun unlockCount(context: Context): Int = prefs(context).getInt(KEY_UNLOCKS, 0)

    private fun increment(context: Context, key: String) {
        val p = prefs(context)
        p.edit().putInt(key, p.getInt(key, 0) + 1).apply()
    }
}
