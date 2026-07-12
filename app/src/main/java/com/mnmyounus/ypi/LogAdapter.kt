package com.mnmyounus.ypi

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mnmyounus.ypi.data.SensorLogEntry
import com.mnmyounus.ypi.data.SensorType
import com.mnmyounus.ypi.databinding.ItemLogEntryBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * LogAdapter
 *
 * Renders completed sensor-usage sessions: the same soft-tinted emoji
 * badge shown in the live overlay, the (best-effort) app label, and a
 * "time · duration · sensor" caption.
 */
class LogAdapter : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    private var items: List<SensorLogEntry> = emptyList()
    private val timeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    fun submit(newItems: List<SensorLogEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(items[position], timeFormat)
    }

    override fun getItemCount() = items.size

    class LogViewHolder(private val b: ItemLogEntryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(entry: SensorLogEntry, timeFormat: SimpleDateFormat) {
            val type = entry.sensorType

            // Soft ~14% tint behind the glyph — a colored icon tile, not a solid dot.
            b.sensorBadgeBg.backgroundTintList = ColorStateList.valueOf(type.color)
            b.sensorBadgeBg.background.alpha = 36
            b.sensorEmoji.text = emojiFor(type, entry.transport)

            b.tvAppLabel.text = entry.appLabel
            val seconds = (entry.durationMillis / 1000).coerceAtLeast(1)
            b.tvMeta.text = "${timeFormat.format(entry.startMillis)} · ${seconds}s · ${type.displayLabel}"
        }

        private fun emojiFor(type: SensorType, transport: String?): String = when (type) {
            SensorType.NETWORK -> when (transport) {
                "MOBILE" -> "📱"
                else     -> type.emoji   // "WIFI" or older entries with no stored transport
            }
            else -> type.emoji
        }
    }
}
