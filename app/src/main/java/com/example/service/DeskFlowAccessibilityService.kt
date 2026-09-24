package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val screenX = (event.xRatio * metrics.widthPixels).coerceIn(0f, metrics.widthPixels.toFloat())
        val screenY = (event.yRatio * metrics.heightPixels).coerceIn(0f, metrics.heightPixels.toFloat())

        if (event.action == "down" || event.action == "click") {
            // Method 1: Hardware-level simulated touch gesture via AccessibilityService (Android 7+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val clickPath = Path().apply {
                    moveTo(screenX, screenY)
                }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(clickPath, 0, 60))
                    .build()

                dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        // Fallback to accessibility node click if gesture was cancelled
                        clickNodeAtCoordinates(screenX.toInt(), screenY.toInt())
                    }
                }, null)
            }

            // Method 2: High-reliability Accessibility Node Inspection & Direct Click
            clickNodeAtCoordinates(screenX.toInt(), screenY.toInt())
        }
    }

    private fun clickNodeAtCoordinates(x: Int, y: Int) {
        Handler(Looper.getMainLooper()).post {
            val root = rootInActiveWindow ?: return@post
            val target = findClickableNodeAt(root, x, y)
            if (target != null) {
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    private fun findClickableNodeAt(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.contains(x, y)) return null

        // Search children first from topmost to bottommost
        for (i in node.childCount - 1 downTo 0) {
            val child = node.getChild(i) ?: continue
            val found = findClickableNodeAt(child, x, y)
            if (found != null) return found
        }

        if (node.isClickable) return node
        return null
    }

    private fun handleRemoteKeyEvent(event: RemoteKeyEvent) {
        val action = event.actionName.lowercase().trim()

        when (action) {
            "back" -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
                return
            }
            "home" -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
            "recents" -> {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
                return
            }
            "power" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else {
                    performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
                }
                return
            }
            "notifications" -> {
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                return
            }
        }

        // Text & Typing Accessibility Dispatch
        Handler(Looper.getMainLooper()).post {
            val root = rootInActiveWindow ?: return@post
            val targetNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: findEditableNode(root)

            if (event.text.isNotEmpty()) {
                if (targetNode != null) {
                    val currentText = targetNode.text?.toString() ?: ""
                    val updatedText = currentText + event.text
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updatedText)
                    }
                    val handled = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    if (!handled) {
                        pasteViaClipboard(targetNode, event.text)
                    }
                } else {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("RemoteText", event.text))
                }
            } else if (action == "backspace") {
                if (targetNode != null) {
                    val currentText = targetNode.text?.toString() ?: ""
                    if (currentText.isNotEmpty()) {
                        val updatedText = currentText.dropLast(1)
                        val args = Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updatedText)
                        }
                        targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    }
                }
            } else if (action == "paste") {
                targetNode?.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            }
        }
    }

    private fun pasteViaClipboard(node: AccessibilityNodeInfo, text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("RemoteInput", text))
        node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableNode(child)
            if (found != null) return found
        }
        return null
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
