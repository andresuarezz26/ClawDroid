package com.aiassistant.data.mapper

import com.aiassistant.domain.model.UINode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UINodeFormatter @Inject constructor() {
    fun format(nodes: List<UINode>): String {
        val sb = StringBuilder()
        flattenToText(nodes, sb)
        return sb.toString()
    }

    private fun flattenToText(nodes: List<UINode>, sb: StringBuilder) {
        for (node in nodes) {
            val props = buildList {
                if (node.isClickable) add("clickable")
                if (node.isScrollable) add("scrollable")
                if (node.isEditable) add("editable")
                node.isChecked?.let { add(if (it) "checked" else "unchecked") }
            }
            val label = node.text ?: node.contentDescription ?: node.resourceId ?: ""
            val b = node.bounds
            sb.appendLine(
                "[${node.index}] ${node.className.substringAfterLast('.')} " +
                "\"$label\" (${props.joinToString()}) [${b.left},${b.top},${b.right},${b.bottom}]"
            )
            flattenToText(node.children, sb)
        }
    }
}