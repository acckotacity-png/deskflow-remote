package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.model.RemoteKeyEvent
import com.example.model.RemoteTouchEvent

class DeskFlowAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        ScreenStreamManager.onDispatchTouch = { event ->
            handleRemoteTouchEvent(event)
        }

        ScreenStreamManager.onDispatchKey = { event ->
            handleRemoteKeyEvent(event)
        }
    }

    private fun handleRemoteTouchEvent(event: RemoteTouchEvent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val screenX = event.xRatio * metrics.widthPixels
        val screenY = event.yRatio * metrics.heightPixels

        if (event.action == "down") {
            // Tap gesture
            val clickPath = Path().apply {
                moveTo(screenX, screenY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(clickPath, 0, 50))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    private fun handleRemoteKeyEvent(event: RemoteKeyEvent) {
        when (event.actionName.lowercase()) {
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "power" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else {
                    performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
                }
            }
            "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance == this) instance = null
        super.onDestroy()
    }

    companion object {
        var instance: DeskFlowAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }
}
