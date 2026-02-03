enum class ActionType {
// App lifecycle
LAUNCH,
RECENT_APPS,

    // Node interactions
    CLICK,
    LONG_CLICK,
    SET_TEXT,
    CLEAR_TEXT,
    COPY,
    PASTE,
    FOCUS,

    // Gestures
    SCROLL,
    SWIPE,
    DRAG,

    // Global actions
    BACK,
    HOME,
    NOTIFICATIONS,
    QUICK_SETTINGS,

    // Control flow
    WAIT,
    SCREENSHOT,
    DONE,
    FAIL
}