package com.mnmyounus.ypi.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * LogExporter
 *
 * Writes the currently-visible (filtered) log entries to a CSV file in
 * the app's cache dir, then hands back a share Intent using a FileProvider
 * content:// URI — the standard, sandboxed way to let another app read a
 * file without granting broad storage access.
 *
 * This stays inside YPI's "zero internet" guarantee: YPI itself never
 * sends this file anywhere. The share sheet lets the PERSON pick which
 * app receives it — if they choose an email app, THAT app may use its
 * own internet access to send it, exactly like sharing any file from any
 * app's share sheet. YPI's own manifest still has no INTERNET permission.
 */
object LogExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun buildShareIntent(context: Context, entries: List<SensorLogEntry>): Intent {
        val csv = buildString {
            append("App,Sensor,Start,Duration (s)\n")
            entries.forEach { entry ->
                val app = entry.appLabel.replace("\"", "'")
                val sensor = entry.sensorType.displayLabel
                val start = dateFormat.format(entry.startMillis)
                val seconds = (entry.durationMillis / 1000).coerceAtLeast(1)
                append("\"$app\",$sensor,$start,$seconds\n")
            }
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "ypi_activity_log.csv")
        file.writeText(csv)

        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
