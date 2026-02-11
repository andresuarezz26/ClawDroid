package com.aiassistant.domain.model

data class Action(
    val type: ActionType,
    val index: Int? = null,
    val packageName: String? = null,
    val text: String? = null,
    val direction: ScrollDirection? = null,
    // Gesture fields for SWIPE and DRAG actions
    val startX: Int? = null,
    val startY: Int? = null,
    val endX: Int? = null,
    val endY: Int? = null,
    val duration: Long? = null,
    // Quick action fields
    val phoneNumber: String? = null,
    val url: String? = null,
    val query: String? = null,
    val section: String? = null,
    val hour: Int? = null,
    val minutes: Int? = null,
    val label: String? = null,
    val title: String? = null,
    val eventStartTime: Long? = null,
    val eventEndTime: Long? = null,
    val description: String? = null,
    val address: String? = null
)

enum class ActionType {
    LAUNCH, CLICK, SET_TEXT, SCROLL, BACK, HOME, PRESS_ENTER,
    // Phase 1: Global Actions
    RECENT_APPS, NOTIFICATIONS, QUICK_SETTINGS,
    // Phase 2: Node Actions
    LONG_CLICK, FOCUS, CLEAR_TEXT,
    // Phase 3: Gesture Actions
    SWIPE, DRAG,
    // Quick Action Intents
    SEND_SMS, MAKE_CALL, OPEN_URL, SET_ALARM, PLAY_MUSIC,
    OPEN_SETTINGS, WEB_SEARCH, CREATE_CALENDAR_EVENT, START_NAVIGATION
}
enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }