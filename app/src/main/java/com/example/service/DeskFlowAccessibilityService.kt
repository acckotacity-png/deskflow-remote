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

        if (event.action == "down" || event.action == "click" || event.action == "up") {
            // 1. Android OS hardware-level simulated touch tap gesture
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val clickPath = Path().apply {
                    moveTo(screenX, screenY)
                    // CRITICAL FIX: Android GestureDescription REQUIRES non-zero length path!
                    // Without lineTo, Android drops zero-length paths and clicks do not register!
                    lineTo(screenX + 1f, screenY + 1f)
                }

                // 100ms duration is optimal for Android app icons & buttons
                val stroke = GestureDescription.StrokeDescription(clickPath, 0, 100)
                val gesture = GestureDescription.Builder()
                    .addStroke(stroke)
                    .build()

                dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        // Fallback: direct Accessibility Node click
                        clickNodeHierarchyAt(screenX.toInt(), screenY.toInt())
                    }
                }, null)
            }

            // 2. Also invoke direct accessibility node click on the targeted app icon/button
            clickNodeHierarchyAt(screenX.toInt(), screenY.toInt())
        }
    }

    private fun clickNodeHierarchyAt(x: Int, y: Int) {
        Handler(Looper.getMainLooper()).post {
            val root = rootInActiveWindow ?: return@post
            val leaf = findDeepestNodeAt(root, x, y) ?: return@post

            // Traverse from the tapped leaf node upwards to find the first clickable element/app icon
            var current: AccessibilityNodeInfo? = leaf
            var clicked = false

            while (current != null) {
                val hasClickAction = current.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }
                if (current.isClickable || hasClickAction) {
                    clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) break
                }
                current = current.parent
            }

            if (!clicked) {
                // If standard click didn't trigger, try ACTION_SELECT or ACTION_FOCUS
                leaf.performAction(AccessibilityNodeInfo.ACTION_SELECT)
            }
        }
    }

    private fun findDeepestNodeAt(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.contains(x, y)) return null

        // Check children from top-most to bottom-most
        for (i in node.childCount - 1 downTo 0) {
            val child = node.getChild(i) ?: continue
            val found = findDeepestNodeAt(child, x, y)
            if (found != null) return found
        }

        return node
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
