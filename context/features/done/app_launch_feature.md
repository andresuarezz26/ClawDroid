# App Launch Feature: Hybrid Discovery & Fallback Strategy

## Problem Statement
Currently, `AppTarget.kt` defines a fixed list of 10 apps, but:
1. The enum is **not actually used** in the launch flow
2. Apps are hardcoded in `SystemPrompt.kt` which the LLM reads
3. If a user asks to open an unlisted app, the LLM may guess wrong package names
4. No validation or fallback when `packageManager.getLaunchIntentForPackage()` fails

---

## Solution: 3-Tier Fallback System

| Tier | Strategy | When Used |
|------|----------|-----------|
| 1 | Check `AppTarget` registry | Known reliable apps (YouTube, WhatsApp, etc.) |
| 2 | Query `PackageManager` | Dynamically discover installed apps |
| 3 | UI Search Fallback | Navigate to app drawer and search by name |

---

## Files to Create

### 1. `domain/model/InstalledApp.kt`
```kotlin
package com.aiassistant.domain.model

data class InstalledApp(
    val packageName: String,
    val displayName: String
)
```

### 2. `domain/repository/AppRepository.kt`
```kotlin
package com.aiassistant.domain.repository

import com.aiassistant.domain.model.InstalledApp

interface AppRepository {
    fun getInstalledLaunchableApps(): List<InstalledApp>
    fun findAppByName(query: String): InstalledApp?
    fun isAppInstalled(packageName: String): Boolean
}
```

### 3. `data/repository/AppRepositoryImpl.kt`
- Query `PackageManager` with `Intent(ACTION_MAIN).addCategory(CATEGORY_LAUNCHER)`
- Cache results to avoid repeated queries
- Fuzzy match app names (e.g., "youtube" matches "YouTube")

### 4. `domain/model/LaunchResult.kt`
```kotlin
package com.aiassistant.domain.model

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class AppNotFound(val packageName: String) : LaunchResult
    data class SearchSuggested(val appName: String) : LaunchResult
}
```

---

## Files to Modify

### 1. `framework/accessibility/ActionPerformer.kt`
- Change `launchApp()` to return `LaunchResult` instead of `Boolean`
- On failure, attempt to find app via `AppRepository.findAppByName()`
- Return detailed failure info for LLM feedback

### 2. `data/repository/ScreenRepositoryImpl.kt`
- Handle `LaunchResult` and provide feedback to the use case

### 3. `domain/usecase/ExecuteTaskUseCase.kt`
- When launch fails, add message to conversation history:
  ```
  "Launch failed: app 'X' not found. Try searching for it in the app drawer."
  ```

### 4. `data/repository/SystemPrompt.kt`
- Add instruction for handling unknown apps:
  ```
  FALLBACK FOR UNKNOWN APPS:
  If an app package is unknown or launch fails, try:
  1. Press HOME
  2. Swipe up to open app drawer
  3. Type the app name in search
  4. Click the matching app icon
  ```

### 5. `data/di/DataModule.kt`
- Add binding for `AppRepository` → `AppRepositoryImpl`

---

## Implementation Order
1. Create `InstalledApp` and `LaunchResult` models
2. Create `AppRepository` interface and implementation
3. Update `ActionPerformer` to use `AppRepository` for fallback
4. Update `ExecuteTaskUseCase` to handle launch failures
5. Update `SystemPrompt` with fallback instructions

---

## Verification Scenarios

| Test Case | Expected Behavior |
|-----------|-------------------|
| "Open YouTube" | Direct launch via `AppTarget` (Tier 1) |
| "Open Slack" | Discovered via `PackageManager` (Tier 2) |
| "Open the music app" | Fuzzy match finds Spotify |
| "Open FakeApp" | LLM receives failure, suggests search (Tier 3) |
| Search fallback | LLM navigates to app drawer and searches |

---

## Architecture Diagram

```
User Command: "Open Slack"
        │
        ▼
┌─────────────────────────────────────────────────────┐
│                  ActionPerformer                     │
├─────────────────────────────────────────────────────┤
│  1. Check AppTarget enum                            │
│     └─ Not found → continue                         │
│                                                     │
│  2. Try packageManager.getLaunchIntentForPackage()  │
│     └─ Returns null → continue                      │
│                                                     │
│  3. Query AppRepository.findAppByName("Slack")      │
│     └─ Found: "com.Slack" → Launch it!              │
│     └─ Not found → Return LaunchResult.AppNotFound  │
└─────────────────────────────────────────────────────┘
        │
        ▼ (if AppNotFound)
┌─────────────────────────────────────────────────────┐
│              ExecuteTaskUseCase                      │
│  Add to history: "Launch failed, try app drawer"    │
└─────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────┐
│                    LLM (GPT-4o)                      │
│  Receives failure message → Plans UI navigation     │
│  Response: HOME → swipe up → search "Slack" → click │
└─────────────────────────────────────────────────────┘
```