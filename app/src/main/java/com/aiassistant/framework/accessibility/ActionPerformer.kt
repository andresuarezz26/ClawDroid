package com.aiassistant.framework.accessibility

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.GestureDescription
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri
import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.ActionType
import com.aiassistant.domain.model.AppTarget
import com.aiassistant.domain.model.LaunchResult
import com.aiassistant.domain.model.ScrollDirection
import com.aiassistant.domain.repository.AppRepository
import com.aiassistant.framework.permission.PermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "ActionPerformer"

@Singleton
class ActionPerformer @Inject constructor(
  private val serviceBridge: AccessibilityServiceBridge,
  private val appRepository: AppRepository,
  @ApplicationContext private val appContext: Context,
  private val permissionManager: PermissionManager
) {
  private var lastLaunchResult: LaunchResult = LaunchResult.Success

  suspend fun execute(action: Action, nodeMap: Map<Int, AccessibilityNodeInfo>): Boolean {
    return when (action.type) {
      ActionType.LAUNCH -> {
        lastLaunchResult = launchAppWithFallback(action.packageName!!)
        lastLaunchResult is LaunchResult.Success || lastLaunchResult is LaunchResult.FoundAndLaunched
      }

      ActionType.CLICK -> clickNode(nodeMap, action.index!!)
      ActionType.SET_TEXT -> setTextOnNode(nodeMap, action.index!!, action.text!!)
      ActionType.SCROLL -> scrollNode(nodeMap, action.index!!, action.direction!!)
      ActionType.BACK -> serviceBridge.performGlobalAction(GLOBAL_ACTION_BACK)
      ActionType.HOME -> serviceBridge.performGlobalAction(GLOBAL_ACTION_HOME)
      ActionType.PRESS_ENTER -> pressEnter(nodeMap, action.index!!)

      // Phase 1: Global Actions
      ActionType.RECENT_APPS -> serviceBridge.performGlobalAction(GLOBAL_ACTION_RECENTS)
      ActionType.NOTIFICATIONS -> serviceBridge.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
      ActionType.QUICK_SETTINGS -> serviceBridge.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

      // Phase 2: Node Actions
      ActionType.LONG_CLICK -> longClickNode(nodeMap, action.index!!)
      ActionType.FOCUS -> focusNode(nodeMap, action.index!!)
      ActionType.CLEAR_TEXT -> clearTextOnNode(nodeMap, action.index!!)

      // Phase 3: Gesture Actions
      ActionType.SWIPE -> performSwipeGesture(
        action.startX!!, action.startY!!, action.endX!!, action.endY!!, action.duration ?: 300L
      )
      ActionType.DRAG -> performDragGesture(
        action.startX!!, action.startY!!, action.endX!!, action.endY!!, action.duration ?: 500L
      )

      // Quick Action Intents
      ActionType.SEND_SMS -> sendSms(action.phoneNumber!!, action.text ?: "")
      ActionType.MAKE_CALL -> makeCall(action.phoneNumber!!)
      ActionType.OPEN_URL -> openUrl(action.url!!)
      ActionType.SET_ALARM -> setAlarm(action.hour!!, action.minutes!!, action.label ?: "")
      ActionType.PLAY_MUSIC -> playMusic(action.query!!)
      ActionType.OPEN_SETTINGS -> openSettings(action.section ?: "")
      ActionType.WEB_SEARCH -> webSearch(action.query!!)
      ActionType.CREATE_CALENDAR_EVENT -> createCalendarEvent(
        action.title!!, action.eventStartTime!!, action.eventEndTime!!, action.description ?: ""
      )
      ActionType.START_NAVIGATION -> startNavigation(action.address!!)
    }
  }

  fun getLastLaunchResult(): LaunchResult = lastLaunchResult

  private fun clickNode(nodeMap: Map<Int, AccessibilityNodeInfo>, index: Int): Boolean {
    val node = nodeMap[index] ?: return false
    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
  }

  private fun setTextOnNode(
    nodeMap: Map<Int, AccessibilityNodeInfo>,
    index: Int,
    text: String
  ): Boolean {
    val node = nodeMap[index] ?: return false
    node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    val args = Bundle().apply {
      putCharSequence(
        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
        text
      )
    }
    return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
  }

  private fun scrollNode(
    nodeMap: Map<Int, AccessibilityNodeInfo>,
    index: Int,
    direction: ScrollDirection
  ): Boolean {
    val node = nodeMap[index] ?: return false
    val scrollAction = when (direction) {
      ScrollDirection.DOWN, ScrollDirection.RIGHT ->
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD

      ScrollDirection.UP, ScrollDirection.LEFT ->
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
    }
    return node.performAction(scrollAction)
  }

  /**
   * 3-Tier App Launch Strategy:
   * 1. Try direct package name launch (from LLM)
   * 2. Check AppTarget registry for known apps
   * 3. Search installed apps by name using AppRepository
   */
  private fun launchAppWithFallback(packageNameOrAppName: String): LaunchResult {
    val context =
      serviceBridge.getContext() ?: return LaunchResult.AppNotFound(packageNameOrAppName)

    // Tier 1: Try direct package name launch
    val directIntent = context.packageManager
      .getLaunchIntentForPackage(packageNameOrAppName)
      ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    if (directIntent != null) {
      context.startActivity(directIntent)
      return LaunchResult.Success
    }

    // Tier 2: Check AppTarget registry (known reliable apps)
    val knownApp = AppTarget.entries.find {
      it.packageName.equals(packageNameOrAppName, ignoreCase = true) ||
          it.displayName.equals(packageNameOrAppName, ignoreCase = true)
    }

    if (knownApp != null) {
      val knownIntent = context.packageManager
        .getLaunchIntentForPackage(knownApp.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

      if (knownIntent != null) {
        context.startActivity(knownIntent)
        return LaunchResult.FoundAndLaunched(knownApp.packageName, knownApp.displayName)
      }
    }

    // Tier 3: Search installed apps by name
    val foundApp = appRepository.findAppByName(packageNameOrAppName)
      ?: appRepository.findAppByPackage(packageNameOrAppName)

    if (foundApp != null) {
      val foundIntent = context.packageManager
        .getLaunchIntentForPackage(foundApp.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

      if (foundIntent != null) {
        context.startActivity(foundIntent)
        return LaunchResult.FoundAndLaunched(foundApp.packageName, foundApp.displayName)
      }
    }

    // All tiers failed
    return LaunchResult.AppNotFound(packageNameOrAppName)
  }

  private fun pressEnter(nodeMap: Map<Int, AccessibilityNodeInfo>, index: Int): Boolean {

    val node = nodeMap[index]

    if (node != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val result =
          node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
        return result
      }
    }

    val rootNode = serviceBridge.getRootNode()

    if (rootNode != null) {
      val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
      if (focused != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          val result = focused.performAction(
            AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
          )
          focused.recycle()
          rootNode.recycle()
          if(result) return true
        }
      } else {
        rootNode.recycle()
      }
    }

    // Tier 3: Shell command fallback (works on all API levels)
    return try {
      Runtime.getRuntime().exec(arrayOf("input", "keyevent", "66"))
      true
    } catch (e: Exception) {
      false
    }


  }

  // Phase 2: Node Action helpers
  private fun longClickNode(nodeMap: Map<Int, AccessibilityNodeInfo>, index: Int): Boolean {
    val node = nodeMap[index] ?: return false
    return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
  }

  private fun focusNode(nodeMap: Map<Int, AccessibilityNodeInfo>, index: Int): Boolean {
    val node = nodeMap[index] ?: return false
    return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
  }

  private fun clearTextOnNode(nodeMap: Map<Int, AccessibilityNodeInfo>, index: Int): Boolean {
    val node = nodeMap[index] ?: return false
    node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    val args = Bundle().apply {
      putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
    }
    return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
  }

  // Phase 3: Gesture helpers
  private suspend fun performSwipeGesture(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    duration: Long
  ): Boolean {
    val path = Path().apply {
      moveTo(startX.toFloat(), startY.toFloat())
      lineTo(endX.toFloat(), endY.toFloat())
    }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
      .build()
    return dispatchGestureWithCallback(gesture)
  }

  private suspend fun performDragGesture(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    duration: Long
  ): Boolean {
    // Drag is essentially a slower swipe - longer duration for precision
    val path = Path().apply {
      moveTo(startX.toFloat(), startY.toFloat())
      lineTo(endX.toFloat(), endY.toFloat())
    }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
      .build()
    return dispatchGestureWithCallback(gesture)
  }

  private suspend fun dispatchGestureWithCallback(gesture: GestureDescription): Boolean {
    return suspendCancellableCoroutine { continuation ->
      val callback = object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
          if (continuation.isActive) {
            continuation.resume(true)
          }
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
          if (continuation.isActive) {
            continuation.resume(false)
          }
        }
      }
      val dispatched = serviceBridge.dispatchGesture(gesture, callback)
      if (!dispatched) {
        continuation.resume(false)
      }
    }
  }

  // --- Quick Action Intent Helpers ---
  // Use service context (privileged, works from background) with app context fallback

  private fun getAvailableContext(): Context = serviceBridge.getContext() ?: appContext

  private fun sendSms(phoneNumber: String, message: String): Boolean {
    return try {
      val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "smsto:$phoneNumber".toUri()
        putExtra("sms_body", message)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      getAvailableContext().startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "sendSms failed", e)
      false
    }
  }

  private suspend fun makeCall(phoneNumber: String): Boolean {
    val ctx = getAvailableContext()
    // Check/request CALL_PHONE permission before attempting ACTION_CALL
    val hasCallPermission = permissionManager.requestPermission(android.Manifest.permission.CALL_PHONE)
    if (hasCallPermission) {
      try {
        val intent = Intent(Intent.ACTION_CALL).apply {
          data = "tel:$phoneNumber".toUri()
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
        return true
      } catch (e: SecurityException) {
        Log.w(TAG, "ACTION_CALL failed despite permission, falling back to ACTION_DIAL", e)
      } catch (e: Exception) {
        Log.e(TAG, "makeCall ACTION_CALL failed", e)
      }
    }
    // Fallback to ACTION_DIAL (no permission needed, user presses call button)
    return try {
      val dialIntent = Intent(Intent.ACTION_DIAL).apply {
        data = "tel:$phoneNumber".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      ctx.startActivity(dialIntent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "makeCall dial fallback failed", e)
      false
    }
  }

  private fun openUrl(url: String): Boolean {
    return try {
      val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
      } else {
        url
      }
      val intent = Intent(Intent.ACTION_VIEW).apply {
        data = fullUrl.toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      getAvailableContext().startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "openUrl failed", e)
      false
    }
  }

  private fun setAlarm(hour: Int, minutes: Int, label: String): Boolean {
    return try {
      val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_HOUR, hour)
        putExtra(AlarmClock.EXTRA_MINUTES, minutes)
        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        if (label.isNotBlank()) {
          putExtra(AlarmClock.EXTRA_MESSAGE, label)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      getAvailableContext().startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "setAlarm failed", e)
      false
    }
  }

  private fun playMusic(query: String): Boolean {
    val ctx = getAvailableContext()
    // Try known music apps by package to avoid the app chooser dialog
    val musicApps = listOf(
      "com.google.android.apps.youtube.music",
      "com.spotify.music",
      "com.apple.android.music",
      "com.amazon.mp3"
    )
    for (pkg in musicApps) {
      try {
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
          putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
          putExtra(SearchManager.QUERY, query)
          setPackage(pkg)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(ctx.packageManager) != null) {
          ctx.startActivity(intent)
          return true
        }
      } catch (e: Exception) {
        Log.w(TAG, "playMusic: $pkg failed, trying next", e)
      }
    }
    // No known music app found — fall back to YouTube Music web URL (no chooser)
    return try {
      Log.w(TAG, "No known music app resolved, falling back to YouTube Music URL")
      val fallback = Intent(Intent.ACTION_VIEW).apply {
        data = "https://music.youtube.com/search?q=${Uri.encode(query)}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      ctx.startActivity(fallback)
      true
    } catch (e: Exception) {
      Log.e(TAG, "playMusic failed entirely", e)
      false
    }
  }

  private fun openSettings(section: String): Boolean {
    return try {
      val action = when (section.lowercase().trim()) {
        "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
        "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
        "display" -> Settings.ACTION_DISPLAY_SETTINGS
        "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
        "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
        "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
        "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
        "security" -> Settings.ACTION_SECURITY_SETTINGS
        "accounts" -> Settings.ACTION_SYNC_SETTINGS
        "" -> Settings.ACTION_SETTINGS
        else -> Settings.ACTION_SETTINGS
      }
      val intent = Intent(action).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      getAvailableContext().startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "openSettings failed", e)
      false
    }
  }

  private fun webSearch(query: String): Boolean {
    val ctx = getAvailableContext()
    return try {
      val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, query)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (intent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(intent)
      } else {
        Log.w(TAG, "No search handler found, falling back to Google URL")
        val fallback = Intent(Intent.ACTION_VIEW).apply {
          data = "https://www.google.com/search?q=${Uri.encode(query)}".toUri()
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(fallback)
      }
      true
    } catch (e: Exception) {
      Log.e(TAG, "webSearch failed", e)
      false
    }
  }

  private fun createCalendarEvent(
    title: String, startTime: Long, endTime: Long, description: String
  ): Boolean {
    return try {
      val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
        if (description.isNotBlank()) {
          putExtra(CalendarContract.Events.DESCRIPTION, description)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      getAvailableContext().startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "createCalendarEvent failed", e)
      false
    }
  }

  private fun startNavigation(address: String): Boolean {
    val ctx = getAvailableContext()
    return try {
      val gmmIntent = Intent(Intent.ACTION_VIEW).apply {
        data = "google.navigation:q=${Uri.encode(address)}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (gmmIntent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(gmmIntent)
      } else {
        Log.w(TAG, "Google Maps not found, falling back to geo URI")
        val fallback = Intent(Intent.ACTION_VIEW).apply {
          data = "geo:0,0?q=${Uri.encode(address)}".toUri()
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(fallback)
      }
      true
    } catch (e: Exception) {
      Log.e(TAG, "startNavigation failed", e)
      false
    }
  }
}
