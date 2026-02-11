Plan: Implement Tier 1 Intent-Based Quick Action Tools

Context

The system prompt in SystemPrompts.kt already references quick action tools (playMusic, setAlarm, sendSms, makeCall, openUrl, openSettings, webSearch) but none of them exist yet. The LLM is
told about tools it can't call. This plan implements all referenced tools plus two more high-value Tier 1 tools (createCalendarEvent, startNavigation).

These tools use Android Intents directly — no accessibility service needed — making them instant and reliable compared to UI automation.

Files to Create/Modify
┌────────────────────────────────────┬────────┬──────────────────────────────────────────────────────┐
│                File                │ Action │                       Purpose                        │
├────────────────────────────────────┼────────┼──────────────────────────────────────────────────────┤
│ agent/QuickActionTools.kt          │ CREATE │ New ToolSet with 9 intent-based tools                │
├────────────────────────────────────┼────────┼──────────────────────────────────────────────────────┤
│ agent/di/AgentModule.kt            │ MODIFY │ Add DI provider for QuickActionTools                 │
├────────────────────────────────────┼────────┼──────────────────────────────────────────────────────┤
│ agent/AndroidAgentFactory.kt       │ MODIFY │ Accept and register QuickActionTools in ToolRegistry │
├────────────────────────────────────┼────────┼──────────────────────────────────────────────────────┤
│ AndroidManifest.xml                │ MODIFY │ Add permissions and <queries> entries                │
├────────────────────────────────────┼────────┼──────────────────────────────────────────────────────┤
│ agent/SystemPrompts.kt             │ MODIFY │ Add createCalendarEvent and startNavigation docs     │
├────────────────────────────────────┼────────┼──────────────────────────────────────────────────────┤
│ presentation/chat/ChatViewModel.kt │ MODIFY │ Remove accessibility service gate (lines 81-87)      │
└────────────────────────────────────┴────────┴──────────────────────────────────────────────────────┘
All paths relative to app/src/main/java/com/aiassistant/ (or app/src/main/ for manifest).

Step 1: Create QuickActionTools.kt

New file at agent/QuickActionTools.kt. Follows the exact same pattern as DeviceTools.kt — ToolSet interface, @Tool + @LLMDescription annotations, returns status strings.

Injected with @ApplicationContext context: Context (always available, no service dependency).

9 tools:

1. sendSms(phoneNumber, message) — Uses ACTION_SENDTO with smsto: URI (opens SMS compose, no SEND_SMS permission needed)
2. makeCall(phoneNumber) — Tries ACTION_CALL first (direct call), catches SecurityException and falls back to ACTION_DIAL (opens dialer)
3. openUrl(url) — Uses ACTION_VIEW with auto-prepend https:// if missing
4. setAlarm(hour, minutes, label) — Uses AlarmClock.ACTION_SET_ALARM with EXTRA_SKIP_UI = true
5. playMusic(query) — Uses MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH, falls back to YouTube Music web URL
6. openSettings(section) — Maps section strings ("wifi", "bluetooth", etc.) to Settings.ACTION_* constants
7. webSearch(query) — Uses ACTION_WEB_SEARCH intent, falls back to Google search URL. (Interim browser-based version; API-based version is a separate feature per
   create_new_tool_web_search.md)
8. createCalendarEvent(title, startTime, endTime, description) — Uses ACTION_INSERT with CalendarContract.Events.CONTENT_URI; times as epoch millis
9. startNavigation(address) — Uses google.navigation:q= URI, falls back to geo:0,0?q=

All methods: try/catch with "FAILED: ..." error messages, FLAG_ACTIVITY_NEW_TASK on every intent, resolveActivity checks where appropriate.

Step 2: Update DI — AgentModule.kt

- Add provideQuickActionTools(@ApplicationContext context: Context): QuickActionTools
- Update provideAndroidAgentFactory to accept quickActionTools: QuickActionTools and pass it to the factory constructor

Step 3: Update AndroidAgentFactory.kt

- Add private val quickActionTools: QuickActionTools to constructor
- Register in ToolRegistry:
  toolRegistry = ToolRegistry {
  tools(deviceTools)
  tools(quickActionTools)
  }

Step 4: Update AndroidManifest.xml

Permissions:
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="com.android.alarm.permission.SET_ALARM" />

Queries (for Android 11+ package visibility):
<intent><action android:name="android.media.action.MEDIA_PLAY_FROM_SEARCH" /></intent>
<intent><action android:name="android.intent.action.WEB_SEARCH" /></intent>
<intent>
<action android:name="android.intent.action.VIEW" />
<data android:scheme="geo" />
</intent>
<intent>
<action android:name="android.intent.action.VIEW" />
<data android:scheme="google.navigation" />
</intent>

Step 5: Update SystemPrompts.kt

Add to the QUICK ACTION TOOLS section:
- createCalendarEvent(title, startTime, endTime, description?) — Creates a calendar event. Times are epoch milliseconds.
- startNavigation(address) — Opens navigation to a destination.

Add examples:
- "Create a meeting at 3pm" → createCalendarEvent(...)
- "Navigate to the airport" → startNavigation("airport")

Update webSearch description to note it opens the browser (doesn't return text results yet).

Step 6: Remove Accessibility Service Gate — ChatViewModel.kt

Lines 81-87: Currently blocks ALL command execution when the accessibility service is disconnected. This prevents quick actions and direct answers from working.

Change: Replace the hard block with a soft warning. Remove the early return, optionally add an informational chat message noting UI automation is unavailable.

Verification

Build the project and test on a physical device:
1. With accessibility service disabled: verify quick actions work (SMS, call, URL, alarm, settings)
2. With accessibility service enabled: verify UI automation still works alongside quick actions
3. Test each of the 9 tools with natural language commands through the chat
4. Verify no tool name collisions between DeviceTools and QuickActionTools
5. Check resolveActivity fallback paths work on Android 11+ devices
