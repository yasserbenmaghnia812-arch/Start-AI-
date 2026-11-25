package com.dark.tool_neuron.ultra.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow

class UltraAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance.value = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        foregroundPackage.value = event.packageName?.toString()
        rootSnapshot.value = summarize(rootInActiveWindow)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance.value === this) instance.value = null
        super.onDestroy()
    }

    private fun summarize(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val result = mutableListOf<String>()
        fun visit(node: AccessibilityNodeInfo) {
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let(result::add)
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        visit(root)
        return result.take(MAX_SNAPSHOT_ITEMS)
    }

    companion object {
        private const val MAX_SNAPSHOT_ITEMS = 200
        val instance = MutableStateFlow<UltraAccessibilityService?>(null)
        val foregroundPackage = MutableStateFlow<String?>(null)
        val rootSnapshot = MutableStateFlow<List<String>>(emptyList())
    }
}
