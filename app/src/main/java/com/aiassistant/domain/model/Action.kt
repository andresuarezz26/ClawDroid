package com.aiassistant.domain.model

data class Action(
    val type: ActionType,
    val index: Int? = null,
    val packageName: String? = null,
    val text: String? = null,
    val direction: ScrollDirection? = null
)

enum class ActionType { LAUNCH, CLICK, SET_TEXT, SCROLL, BACK, HOME, PRESS_ENTER}
enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }