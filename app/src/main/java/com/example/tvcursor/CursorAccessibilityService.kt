package com.example.tvcursor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.Toast

class CursorAccessibilityService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var cursorView: ImageView
    private lateinit var params: WindowManager.LayoutParams

    private var cursorAdded = false
    private var cursorVisible = false

    private var cursorX = 0
    private var cursorY = 0
    private var screenWidth = 0
    private var screenHeight = 0

    // Base movement step in pixels; grows the longer a direction is held (via repeatCount)
    private val baseStep = 22

    // Long-press-BACK-to-toggle bookkeeping
    private val handler = Handler(Looper.getMainLooper())
    private var backLongPressTriggered = false
    private val longPressThresholdMs = 550L
    private val backLongPressRunnable = Runnable {
        backLongPressTriggered = true
        toggleCursor()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        cursorX = screenWidth / 2
        cursorY = screenHeight / 2

        cursorView = ImageView(this).apply {
            setImageResource(R.drawable.cursor)
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = cursorX
            y = cursorY
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — we only need key events, not content events.
    }

    override fun onInterrupt() {}

    @SuppressLint("ClickableViewAccessibility")
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // --- Long-press BACK toggles cursor mode, short-press BACK behaves normally ---
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        backLongPressTriggered = false
                        handler.postDelayed(backLongPressRunnable, longPressThresholdMs)
                    }
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    handler.removeCallbacks(backLongPressRunnable)
                    if (!backLongPressTriggered) {
                        // Short press: perform the normal system Back action ourselves,
                        // since we consumed the DOWN event above.
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                    return true
                }
            }
            return true
        }

        // If cursor mode is off, let every other key behave normally.
        if (!cursorVisible) {
            return false
        }

        // --- Cursor mode is ON: D-pad moves the cursor, center/enter clicks ---
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    moveCursor(event.keyCode, event.repeatCount)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    performClick(cursorX, cursorY)
                }
                return true
            }
        }

        // Let media keys, volume, etc. pass through untouched.
        return false
    }

    private fun moveCursor(keyCode: Int, repeatCount: Int) {
        // Accelerate the further a direction is held down.
        val step = baseStep + (repeatCount * 6).coerceAtMost(60)

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> cursorY -= step
            KeyEvent.KEYCODE_DPAD_DOWN -> cursorY += step
            KeyEvent.KEYCODE_DPAD_LEFT -> cursorX -= step
            KeyEvent.KEYCODE_DPAD_RIGHT -> cursorX += step
        }

        cursorX = cursorX.coerceIn(0, screenWidth - 1)
        cursorY = cursorY.coerceIn(0, screenHeight - 1)

        params.x = cursorX
        params.y = cursorY
        if (cursorAdded) {
            windowManager.updateViewLayout(cursorView, params)
        }
    }

    private fun performClick(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 60)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun toggleCursor() {
        cursorVisible = !cursorVisible
        if (cursorVisible) {
            showCursor()
        } else {
            hideCursor()
        }
    }

    private fun showCursor() {
        if (!cursorAdded) {
            windowManager.addView(cursorView, params)
            cursorAdded = true
        } else {
            cursorView.visibility = android.view.View.VISIBLE
        }
        Toast.makeText(this, "Cursor ON", Toast.LENGTH_SHORT).show()
    }

    private fun hideCursor() {
        if (cursorAdded) {
            cursorView.visibility = android.view.View.GONE
        }
        Toast.makeText(this, "Cursor OFF", Toast.LENGTH_SHORT).show()
    }
}
