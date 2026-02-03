package com.aiassistant.data.mapper

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.aiassistant.domain.model.Bounds
import com.aiassistant.domain.model.UINode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenParser @Inject constructor() {
    private val _nodeMap = ConcurrentHashMap<Int, AccessibilityNodeInfo>()
    val nodeMap: Map<Int, AccessibilityNodeInfo> = _nodeMap

    fun parse(rootNode: AccessibilityNodeInfo): List<UINode> {
        val nodeIndex = AtomicInteger(0)
        _nodeMap.clear()
        return walkTree(rootNode, nodeIndex)
    }

    private fun walkTree(
        node: AccessibilityNodeInfo,
        nodeIndex: AtomicInteger
    ): List<UINode> {
        if (!node.isVisibleToUser) return emptyList()

        val rect = Rect()
        node.getBoundsInScreen(rect)

        val children = mutableListOf<UINode>()
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { children.addAll(walkTree(it, nodeIndex)) }
        }

        val isRelevant = node.isClickable || node.isScrollable || node.isEditable ||
                !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()

        return if (isRelevant) {
            val idx = nodeIndex.getAndIncrement()
            _nodeMap[idx] = node
            listOf(UINode(
                index = idx,
                className = node.className?.toString() ?: "",
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                resourceId = node.viewIdResourceName,
                isClickable = node.isClickable,
                isScrollable = node.isScrollable,
                isEditable = node.isEditable,
                isChecked = if (node.isCheckable) node.isChecked else null,
                bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
                children = children
            ))
        } else children
    }
}