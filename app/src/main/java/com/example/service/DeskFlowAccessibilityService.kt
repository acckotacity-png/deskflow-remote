package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val screenX = (event.xRatio * metrics.widthPixels).coerceIn(0f, metrics.widthPixels.toFloat())
        val screenY = (event.yRatio * metrics.heightPixels).coerceIn(0f, metrics.heightPixels.toFloat())

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
                // Type text into the remote target input field
                if (targetNode != null) {
                    val currentText = targetNode.text?.toString() ?: ""
                    val updatedText = currentText + event.text
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updatedText)
                    }
                    val handled = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    if (!handled) {
                        // Fallback: Copy to clipboard and paste
                        pasteViaClipboard(targetNode, event.text)
                    }
                } else {
                    // Copy to device clipboard so user can paste anywhere
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
