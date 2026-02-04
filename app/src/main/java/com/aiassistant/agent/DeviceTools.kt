package com.aiassistant.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import android.util.Log
import com.aiassistant.data.mapper.UINodeFormatter
import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.ActionType
import com.aiassistant.domain.model.ScrollDirection
import com.aiassistant.domain.repository.ScreenRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

private const val TAG = "Agent"

@LLMDescription("Android device automation tools")
class DeviceTools @Inject constructor(
  private val screenRepository: ScreenRepository,
  private val uiNodeFormatter: UINodeFormatter
) : ToolSet {

  @Tool
  @LLMDescription("Get current screen UI tree with element indices")
  suspend fun getScreen(): String {
    Log.i(TAG, "Tool: getScreen() called")
    val nodes = screenRepository.captureScreen()
    val result = if (nodes.isEmpty()) {
      "ERROR: Cannot read screen"
    } else {
      uiNodeFormatter.format(nodes)
    }
    Log.i(TAG, "Tool: getScreen() returned ${nodes.size} nodes. Result: ${if(nodes.isEmpty()) result else "" }")
    return result
  }

  @Tool
  @LLMDescription("Click element by index")
  suspend fun click(@LLMDescription("Element index") index: Int): String {
    Log.i(TAG, "Tool: click(index=$index) called")
    val action = Action(type = ActionType.CLICK, index = index)
    val success = screenRepository.performAction(action)
    val result = if (success) "Clicked [$index]" else "FAILED: Click [$index]"
    Log.i(TAG, "Tool: click(index=$index) -> ${if(success) "" else result}")
    return result
  }

  @Tool
  @LLMDescription("Type text into editable field")
  suspend fun setText(
    @LLMDescription("Field index") index: Int,
    @LLMDescription("Text to type") text: String
  ): String {
    Log.i(TAG, "Tool: setText(index=$index, text='$text') called")
    val action = Action(type = ActionType.SET_TEXT, index = index, text = text)
    val success = screenRepository.performAction(action)
    val result = if (success) "Typed '$text' into [$index]" else "FAILED"
    Log.i(TAG, "Tool: setText() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Scroll element in direction")
  suspend fun scroll(
    @LLMDescription("Element index") index: Int,
    @LLMDescription("up/down/left/right") direction: String
  ): String {
    Log.i(TAG, "Tool: scroll(index=$index, direction='$direction') called")
    val scrollDir = try {
      ScrollDirection.valueOf(direction.uppercase())
    } catch (e: IllegalArgumentException) {
      Log.i(TAG, "Tool: scroll() -> FAILED: Invalid direction")
      return "FAILED: Invalid direction '$direction'. Use: up, down, left, right"
    }
    val action = Action(type = ActionType.SCROLL, index = index, direction = scrollDir)
    val result = if (screenRepository.performAction(action)) "Scrolled $direction" else "FAILED"
    Log.i(TAG, "Tool: scroll() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Launch app by name")
  suspend fun launchApp(@LLMDescription("App name") appName: String): String {
    Log.i(TAG, "Tool: launchApp(appName='$appName') called")
    val action = Action(type = ActionType.LAUNCH, packageName = appName)
    val result =
      if (screenRepository.performAction(action)) "Launched $appName" else "FAILED: Could not launch $appName"
    Log.i(TAG, "Tool: launchApp() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Press back button")
  suspend fun pressBack(): String {
    Log.i(TAG, "Tool: pressBack() called")
    val action = Action(type = ActionType.BACK)
    val result = if (screenRepository.performAction(action)) "Pressed back" else "FAILED"
    Log.i(TAG, "Tool: pressBack() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Press home button")
  suspend fun pressHome(): String {
    Log.i(TAG, "Tool: pressHome() called")
    val action = Action(type = ActionType.HOME)
    val result = if (screenRepository.performAction(action)) "Pressed home" else "FAILED"
    Log.i(TAG, "Tool: pressHome() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Wait for screen to settle after action")
  suspend fun waitForUpdate(@LLMDescription("Milliseconds") ms: Int = 1500): String {
    Log.i(TAG, "Tool: waitForUpdate(ms=$ms) called")
    delay(ms.coerceIn(100, 5000).toLong())
    Log.i(TAG, "Tool: waitForUpdate() -> Waited ${ms}ms")
    return "Waited ${ms}ms"
  }

  @Tool
  @LLMDescription("Signal task completion")
  fun taskComplete(@LLMDescription("Summary") summary: String): String {
    Log.i(TAG, "Tool: taskComplete(summary='$summary')")
    return "TASK_COMPLETE: $summary"
  }

  @Tool
  @LLMDescription("Signal task failure")
  fun taskFailed(@LLMDescription("Reason") reason: String): String {
    Log.i(TAG, "Tool: taskFailed(reason='$reason')")
    return "TASK_FAILED: $reason"
  }

  @Tool
  @LLMDescription(
    "Press the Enter/Search key on the keyboard. Use after typing text " +
        "in a search field to submit the search query."
  )
  suspend fun pressEnter(): String {
    Log.i(TAG, "Tool: pressEnter() called")
    val action = Action(type = ActionType.PRESS_ENTER)
    val result = if (screenRepository.performAction(action)) "Pressed Enter" else "FAILED"
    Log.i(TAG, "Tool: pressEnter() -> $result")
    return result
  }
}
