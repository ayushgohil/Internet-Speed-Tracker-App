package com.ayush.netpulse.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import com.ayush.netpulse.data.model.LiveSpeed

/** A single, non-focusable window owned by the existing foreground service. */
internal class FloatingSpeedOverlay(private val context: Context) {
    private val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences("netpulse_overlay", Context.MODE_PRIVATE)
    private var view: TextView? = null
    private var positionRevision = prefs.getInt("position_revision", 0)
    private var styleKey = ""
    @Volatile private var closed = false
    private val density = context.resources.displayMetrics.density

    fun update(speed: LiveSpeed, interactive: Boolean) {
        handler.post {
            if (closed) return@post
            if (!interactive || !prefs.getBoolean("enabled", false) || !Settings.canDrawOverlays(context)) {
                remove()
                return@post
            }
            try {
                val revision = prefs.getInt("position_revision", 0)
                if (revision != positionRevision) {
                    remove()
                    positionRevision = revision
                }
                val label = view ?: create()
                val font = prefs.getInt("font_size", 12).coerceIn(10, 22)
                val opacity = prefs.getInt("opacity", 90).coerceIn(35, 100)
                val style = "$font:$opacity"
                if (styleKey != style) {
                    label.textSize = font.toFloat()
                    label.background = GradientDrawable().apply {
                        setColor(Color.argb(opacity * 255 / 100, 23, 33, 59))
                        cornerRadius = 10 * density
                    }
                    styleKey = style
                }
                label.text = if (prefs.getBoolean("download_only", false)) "↓ ${speed.formattedDownload}"
                    else "↓ ${speed.formattedDownload}\n↑ ${speed.formattedUpload}"
            } catch (_: RuntimeException) {
                remove()
            }
        }
    }

    private fun create(): TextView {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", 16)
            y = prefs.getInt("y", (100 * density).toInt())
        }
        val label = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
            background = GradientDrawable().apply {
                setColor(0xE617213B.toInt())
                cornerRadius = 10 * density
            }
            contentDescription = "Live internet speed. Drag to reposition."
        }
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        label.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val metrics = context.resources.displayMetrics
                    params.x = (initialX + (event.rawX - touchX).toInt()).coerceIn(0, (metrics.widthPixels - label.width).coerceAtLeast(0))
                    params.y = (initialY + (event.rawY - touchY).toInt()).coerceIn(0, (metrics.heightPixels - label.height).coerceAtLeast(0))
                    try { manager.updateViewLayout(label, params) } catch (_: RuntimeException) { }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    prefs.edit().putInt("x", params.x).putInt("y", params.y).apply()
                    true
                }
                else -> false
            }
        }
        manager.addView(label, params)
        view = label
        return label
    }

    private fun remove() {
        view?.let { try { manager.removeView(it) } catch (_: RuntimeException) { } }
        view = null
        styleKey = ""
    }

    fun close() {
        closed = true
        handler.post { remove() }
    }
}
