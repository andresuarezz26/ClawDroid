package com.aiassistant.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import android.util.Log
import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.ActionType
import com.aiassistant.domain.repository.ScreenRepository
import javax.inject.Inject

private const val TAG = "Agent"

@LLMDescription("Quick action tools using Android intents")
class QuickActionTools @Inject constructor(
    private val screenRepository: ScreenRepository
) : ToolSet {

    @Tool
    @LLMDescription("Send an SMS text message")
    suspend fun sendSms(
        @LLMDescription("Phone number to send to") phoneNumber: String,
        @LLMDescription("Message text") message: String
    ): String {
        Log.i(TAG, "Tool: sendSms(phoneNumber='$phoneNumber', message='$message') called")
        val action = Action(type = ActionType.SEND_SMS, phoneNumber = phoneNumber, text = message)
        val success = screenRepository.performAction(action)
        val result = if (success) "Opened SMS compose to $phoneNumber with message" else "FAILED: Could not open SMS compose"
        Log.i(TAG, "Tool: sendSms() -> $result")
        return result
    }

    @Tool
    @LLMDescription("Start a phone call")
    suspend fun makeCall(
        @LLMDescription("Phone number to call") phoneNumber: String
    ): String {
        Log.i(TAG, "Tool: makeCall(phoneNumber='$phoneNumber') called")
        val action = Action(type = ActionType.MAKE_CALL, phoneNumber = phoneNumber)
        val success = screenRepository.performAction(action)
        val result = if (success) "Calling $phoneNumber" else "FAILED: Could not call $phoneNumber"
        Log.i(TAG, "Tool: makeCall() -> $result")
        return result
    }

    @Tool
    @LLMDescription("Open a URL in the default browser")
    suspend fun openUrl(
        @LLMDescription("URL to open") url: String
    ): String {
        Log.i(TAG, "Tool: openUrl(url='$url') called")
        val action = Action(type = ActionType.OPEN_URL, url = url)
        val success = screenRepository.performAction(action)
        val result = if (success) "Opened $url" else "FAILED: Could not open URL"
        Log.i(TAG, "Tool: openUrl() -> $result")
        return result
    }

    @Tool
    @LLMDescription("Set an alarm. Uses 24-hour format.")
    suspend fun setAlarm(
        @LLMDescription("Hour in 24-hour format (0-23)") hour: Int,
        @LLMDescription("Minutes (0-59)") minutes: Int,
        @LLMDescription("Optional alarm label") label: String = ""
    ): String {
        Log.i(TAG, "Tool: setAlarm(hour=$hour, minutes=$minutes, label='$label') called")
        val action = Action(type = ActionType.SET_ALARM, hour = hour, minutes = minutes, label = label)
        val success = screenRepository.performAction(action)
        val formattedTime = String.format("%02d:%02d", hour, minutes)
        val result = if (success) {
            "Alarm set for $formattedTime${if (label.isNotBlank()) " ($label)" else ""}"
        } else {
            "FAILED: Could not set alarm"
        }
        Log.i(TAG, "Tool: setAlarm() -> $result")
        return result
    }

    @Tool
    @LLMDescription("Play music by searching for a song, artist, album, or genre")
    suspend fun playMusic(
        @LLMDescription("Search query (song name, artist, etc.)") query: String
    ): String {
        Log.i(TAG, "Tool: playMusic(query='$query') called")
        val action = Action(type = ActionType.PLAY_MUSIC, query = query)
        val success = screenRepository.performAction(action)
        val result = if (success) "Playing '$query'" else "FAILED: Could not play music"
        Log.i(TAG, "Tool: playMusic() -> $result")
        return result
    }

    @Tool
    @LLMDescription("Open device settings. Sections: wifi, bluetooth, display, sound, battery, apps, location, security, accounts, or empty for main settings.")
    suspend fun openSettings(
        @LLMDescription("Settings section name (wifi, bluetooth, display, sound, battery, apps, location, security, accounts) or empty for main") section: String = ""
    ): String {
        Log.i(TAG, "Tool: openSettings(section='$section') called")
        val action = Action(type = ActionType.OPEN_SETTINGS, section = section)
        val success = screenRepository.performAction(action)
        val result = if (success) {
            if (section.isBlank()) "Opened Settings" else "Opened ${section.trim()} settings"
        } else {
            "FAILED: Could not open settings"
        }
        Log.i(TAG, "Tool: openSettings() -> $result")
        return result
    }

    @Tool
    @LLMDescription("Search the web. Opens the browser with search results.")
    suspend fun webSearch(
        @LLMDescription("Search query") query: String
    ): String {
        Log.i(TAG, "Tool: webSearch(query='$query') called")
        val action = Action(type = ActionType.WEB_SEARCH, query = query)
        val success = screenRepository.performAction(action)
        val result = if (success) "Opened web search for '$query'" else "FAILED: Could not search"
        Log.i(TAG, "Tool: webSearch() -> $result")
        return result
    }

    @Tool
    @LLMDescription("Create a calendar event. Times are epoch milliseconds.")
    suspend fun createCalendarEvent(
        @LLMDescription("Event title") title: String,
        @LLMDescription("Start time in epoch milliseconds") startTime: Long,
        @LLMDescription("End time in epoch milliseconds") endTime: Long,
        @LLMDescription("Optional event description") description: String = ""
    ): String {
        Log.i(TAG, "Tool: createCalendarEvent(title='$title', startTime=$startTime, endTime=$endTime) called")
        val action = Action(
            type = ActionType.CREATE_CALENDAR_EVENT,
            title = title,
            eventStartTime = startTime,
            eventEndTime = endTime,
            description = description
        )
        val success = screenRepository.performAction(action)
        val result = if (success) "Opened calendar to create event '$title'" else "FAILED: Could not create calendar event"
        Log.i(TAG, "Tool: createCalendarEvent() -> $result")
        return result
    }

    @Tool
    @LLMDescription("Open navigation to a destination address or place")
    suspend fun startNavigation(
        @LLMDescription("Destination address or place name") address: String
    ): String {
        Log.i(TAG, "Tool: startNavigation(address='$address') called")
        val action = Action(type = ActionType.START_NAVIGATION, address = address)
        val success = screenRepository.performAction(action)
        val result = if (success) "Started navigation to '$address'" else "FAILED: Could not start navigation"
        Log.i(TAG, "Tool: startNavigation() -> $result")
        return result
    }
}
