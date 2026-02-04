package com.aiassistant.framework.accessibility

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.ActionType
import com.aiassistant.domain.model.AppTarget
import com.aiassistant.domain.model.LaunchResult
import com.aiassistant.domain.model.ScrollDirection
import com.aiassistant.domain.repository.AppRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionPerformer @Inject constructor(
  private val serviceBridge: AccessibilityServiceBridge,
  private val appRepository: AppRepository
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
}
