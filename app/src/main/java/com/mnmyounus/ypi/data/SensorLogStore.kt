package com.mnmyounus.ypi.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import org.json.JSONArray
import java.io.File
import java.util.concurrent.Executors

/**
 * SensorLogStore
 *
 * Persists the sensor activity log as a single AES-256-GCM encrypted file,
 * keyed by an Android Keystore-backed master key (Jetpack Security /
 * Tink under the hood — same key-management model as Android Keystore use
 * elsewhere in the Y-suite). No internet, no cloud, no plaintext on disk.
 *
 * EncryptedFile is write-once-then-read by design (no in-place append), so
 * this store keeps a small in-memory cache and rewrites the whole file on
 * each change. That's intentionally fine here: entries are only written on
 * sensor start/stop transitions (sparse events, not continuous), so even a
 * few thousand entries re-encrypt in well under 100ms.
 *
 * All I/O runs on a single background thread — callers never block.
 */
class SensorLogStore private constructor(context: Context) {

    private val appContext  = context.applicationContext
    private val ioExecutor  = Executors.newSingleThreadExecutor()
    private val logFile     = File(appContext.filesDir, "sensor_log.enc")

    private val masterKeyAlias by lazy { MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC) }

    /** Mutated only on ioExecutor — never touch this from another thread. */
    private var cache: MutableList<SensorLogEntry>? = null

    companion object {
        @Volatile private var instance: SensorLogStore? = null

        fun get(context: Context): SensorLogStore =
            instance ?: synchronized(this) {
                instance ?: SensorLogStore(context).also { instance = it }
            }
    }

    /** Appends one completed session and prunes anything past [retentionMillis]. */
    fun append(entry: SensorLogEntry, retentionMillis: Long) {
        ioExecutor.execute {
            val list = loadLocked()
            list.add(entry)
            pruneLocked(list, retentionMillis)
            saveLocked(list)
        }
    }

    /** Drops entries older than [retentionMillis] without adding anything new. */
    fun pruneNow(retentionMillis: Long) {
        ioExecutor.execute {
            val list = loadLocked()
            if (pruneLocked(list, retentionMillis)) saveLocked(list)
        }
    }

    /** Delivers a snapshot of all entries on [callback], invoked on the background thread. */
    fun loadAll(callback: (List<SensorLogEntry>) -> Unit) {
        ioExecutor.execute { callback(loadLocked().toList()) }
    }

    fun clearAll() {
        ioExecutor.execute {
            cache = mutableListOf()
            logFile.delete()
        }
    }

    // ── Internal — only ever touched from ioExecutor ────────────────

    private fun loadLocked(): MutableList<SensorLogEntry> {
        cache?.let { return it }
        val loaded = try {
            if (!logFile.exists()) {
                mutableListOf()
            } else {
                buildEncryptedFile().openFileInput().use { input ->
                    val text = String(input.readBytes(), Charsets.UTF_8)
                    val arr  = JSONArray(text)
                    val out  = mutableListOf<SensorLogEntry>()
                    for (i in 0 until arr.length()) out.add(SensorLogEntry.fromJson(arr.getJSONObject(i)))
                    out
                }
            }
        } catch (_: Exception) {
            mutableListOf()   // corrupted or unreadable file → start fresh, never crash
        }
        cache = loaded
        return loaded
    }

    private fun saveLocked(list: List<SensorLogEntry>) {
        try {
            // EncryptedFile refuses to write over an existing file — delete first.
            if (logFile.exists()) logFile.delete()
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            buildEncryptedFile().openFileOutput().use { output ->
                output.write(arr.toString().toByteArray(Charsets.UTF_8))
            }
        } catch (_: Exception) {
            // Best-effort persistence — in-memory cache stays correct for this process lifetime
        }
    }

    private fun pruneLocked(list: MutableList<SensorLogEntry>, retentionMillis: Long): Boolean {
        if (retentionMillis <= 0L) return false
        val cutoff = System.currentTimeMillis() - retentionMillis
        val before = list.size
        list.removeAll { it.endMillis < cutoff }
        return list.size != before
    }

    private fun buildEncryptedFile(): EncryptedFile =
        EncryptedFile.Builder(
            logFile,
            appContext,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
}
