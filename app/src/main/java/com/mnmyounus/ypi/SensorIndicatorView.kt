package com.mnmyounus.ypi

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.mnmyounus.ypi.data.SensorType

/**
 * SensorIndicatorView
 *
 * Renders up to 6 small badges in a fixed-position row — one per
 * SensorType, always in the same slot regardless of which others are
 * active, so badges never "jump" as siblings toggle. Each badge is a
 * soft tinted circle with that sensor's real emoji glyph centered on
 * top (see SensorType.emoji) — not a flat color dot. Color emoji are
 * pre-hinted by the system font for small sizes, which is what actually
 * fixes legibility here; a hand-rasterized vector icon blurred into an
 * indistinct blob at this diameter, an emoji glyph doesn't.
 *
 * The view's width is fixed at 6 slots wide, anchored to the screen's
 * right edge by the hosting WindowManager params. Empty slots are fully
 * transparent and non-touchable, so allocating the full width up front
 * costs nothing visually even when 0–5 sensors are active.
 *
 * Call updateState() only from the main thread.
 */
class SensorIndicatorView(context: Context) : View(context) {

    companion object {
        const val SLOT_WIDTH_DP = 23
        const val BADGE_DIAMETER_DP = 20
        private const val SHOW_DURATION_MS = 380L
        private const val HIDE_DURATION_MS = 200L
        private const val PULSE_DURATION_MS = 680L
    }

    private val slots = SensorType.values()

    private class Badge {
        var animScale = 0f
        var pulseScale = 1f
        var isActive = false
        val showAnim = ValueAnimator().apply {
            interpolator = OvershootInterpolator(1.7f)
            duration = SHOW_DURATION_MS
        }
        val hideAnim = ValueAnimator().apply {
            interpolator = AccelerateInterpolator()
            duration = HIDE_DURATION_MS
        }
        var pulseAnim: ValueAnimator? = null
    }

    private val badges: Map<SensorType, Badge> = slots.associateWith { Badge() }
    private var transport: NetworkMonitor.Transport = NetworkMonitor.Transport.NONE

    // Soft tinted circle behind each glyph — not solid, so it reads as an
    // icon tile rather than a color dot even before the emoji is factored in.
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; alpha = 235 }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(45, 0, 0, 0)
    }
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE   // no-op for true color-emoji glyphs; a safety net if a device ever falls back to monochrome
    }

    // ── Public API ───────────────────────────────────────────────

    /** Sets which sensors are currently active. Triggers per-badge in/out animation on change. */
    fun updateState(active: Set<SensorType>) {
        slots.forEach { type ->
            val badge = badges[type] ?: return@forEach
            val shouldBeActive = type in active
            if (shouldBeActive != badge.isActive) {
                badge.isActive = shouldBeActive
                if (shouldBeActive) animateIn(badge) else animateOut(badge)
            }
        }
    }

    /** Updates which glyph the NETWORK badge shows — WiFi (📶) or mobile (📱). */
    fun setNetworkTransport(newTransport: NetworkMonitor.Transport) {
        if (transport != newTransport) { transport = newTransport; invalidate() }
    }

    // ── Animation ────────────────────────────────────────────────

    private fun animateIn(badge: Badge) {
        badge.hideAnim.cancel(); badge.showAnim.cancel()
        badge.showAnim.removeAllUpdateListeners()
        badge.showAnim.setFloatValues(badge.animScale, 1f)
        badge.showAnim.addUpdateListener { badge.animScale = it.animatedValue as Float; invalidate() }
        badge.showAnim.start()
        triggerPulse(badge)
    }

    private fun animateOut(badge: Badge) {
        badge.pulseAnim?.cancel(); badge.showAnim.cancel(); badge.hideAnim.cancel()
        badge.hideAnim.removeAllUpdateListeners()
        badge.hideAnim.setFloatValues(badge.animScale, 0f)
        badge.hideAnim.addUpdateListener { badge.animScale = it.animatedValue as Float; invalidate() }
        badge.hideAnim.start()
    }

    private fun triggerPulse(badge: Badge) {
        badge.pulseAnim?.cancel()
        badge.pulseAnim = ValueAnimator.ofFloat(1f, 1.20f, 1f).apply {
            duration = PULSE_DURATION_MS
            addUpdateListener { badge.pulseScale = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    // ── Drawing ───────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        val slotWidthPx = dpToPx(SLOT_WIDTH_DP)
        val baseR = dpToPx(BADGE_DIAMETER_DP) / 2f
        val centerY = height / 2f

        slots.forEachIndexed { index, type ->
            val badge = badges[type] ?: return@forEachIndexed
            if (badge.animScale <= 0f) return@forEachIndexed

            val r = baseR * badge.animScale * badge.pulseScale
            if (r <= 0f) return@forEachIndexed

            // Slot 0 (Camera) sits nearest the right edge; later slots sit further left.
            val cx = width - (index * slotWidthPx) - (slotWidthPx / 2f)

            canvas.drawCircle(cx, centerY + 2.5f, r + 0.5f, shadowPaint)
            circlePaint.color = type.color
            canvas.drawCircle(cx, centerY, r, circlePaint)

            // Glyph size tracks the badge's own animated radius, so it scales
            // in/out and pulses in lockstep with the circle behind it.
            emojiPaint.textSize = r * 1.35f
            val textY = centerY - (emojiPaint.descent() + emojiPaint.ascent()) / 2f
            canvas.drawText(emojiFor(type), cx, textY, emojiPaint)
        }
    }

    private fun emojiFor(type: SensorType): String = when (type) {
        SensorType.NETWORK -> if (transport == NetworkMonitor.Transport.MOBILE) "📱" else type.emoji
        else -> type.emoji
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density + 0.5f).toInt()
}
