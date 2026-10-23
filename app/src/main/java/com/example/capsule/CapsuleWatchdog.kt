package com.example.capsule

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

// Keep-alive. When the user has turned on navigation info, it also reads text from the Google Maps
// screen only. It never reads other apps' screens, and it doesn't save or send what it reads.
class CapsuleWatchdog : AccessibilityService() {

    private var lastRead = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        NotificationListenerService.requestRebind(ComponentName(this, CapsuleService::class.java))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (e.packageName?.toString() != CapsulePrefs.MAPS_PKG) {
            if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) MapsNav.active = false
            return
        }
        val on = getSharedPreferences("capsule", MODE_PRIVATE).getBoolean("mapsInfo", false)
        if (!on) return
        val now = SystemClock.uptimeMillis()
        if (now - lastRead < 500) return
        lastRead = now
        val root = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        collect(root, texts)
        MapsNav.update(texts)
    }

    private fun collect(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        node.text?.let { out.add(it.toString()) }
        node.contentDescription?.let { out.add(it.toString()) }
        for (i in 0 until node.childCount) collect(node.getChild(i), out)
    }

    override fun onInterrupt() {}
}
