package com.aiassistant.domain.model

data class UINode(
    val index: Int,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isChecked: Boolean?,
    val bounds: Bounds,
    val children: List<UINode>
)