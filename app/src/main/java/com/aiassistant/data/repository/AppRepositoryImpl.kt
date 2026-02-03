package com.aiassistant.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.aiassistant.domain.model.AppTarget
import com.aiassistant.domain.model.InstalledApp
import com.aiassistant.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {

    private var cachedApps: List<InstalledApp>? = null

    override fun getInstalledLaunchableApps(): List<InstalledApp> {
        cachedApps?.let { return it }

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList: List<ResolveInfo> = context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val apps = resolveInfoList.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
            val displayName = resolveInfo.loadLabel(context.packageManager)?.toString() ?: packageName

            InstalledApp(
                packageName = packageName,
                displayName = displayName
            )
        }.distinctBy { it.packageName }

        cachedApps = apps
        return apps
    }

    override fun findAppByName(query: String): InstalledApp? {
        val normalizedQuery = query.lowercase(Locale.getDefault()).trim()

        // First check AppTarget for known apps (Tier 1)
        val knownApp = AppTarget.entries.find { appTarget ->
            appTarget.displayName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
            normalizedQuery.contains(appTarget.displayName.lowercase(Locale.getDefault()))
        }
        if (knownApp != null && isAppInstalled(knownApp.packageName)) {
            return InstalledApp(knownApp.packageName, knownApp.displayName)
        }

        // Then search installed apps (Tier 2)
        val installedApps = getInstalledLaunchableApps()

        // Exact match first
        installedApps.find {
            it.displayName.lowercase(Locale.getDefault()) == normalizedQuery
        }?.let { return it }

        // Contains match
        installedApps.find {
            it.displayName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
            normalizedQuery.contains(it.displayName.lowercase(Locale.getDefault()))
        }?.let { return it }

        // Fuzzy match - check if words match
        val queryWords = normalizedQuery.split(" ", "-", "_")
        installedApps.find { app ->
            val appWords = app.displayName.lowercase(Locale.getDefault()).split(" ", "-", "_")
            queryWords.any { queryWord ->
                appWords.any { appWord -> appWord.contains(queryWord) || queryWord.contains(appWord) }
            }
        }?.let { return it }

        return null
    }

    override fun findAppByPackage(packageName: String): InstalledApp? {
        // Check AppTarget first
        AppTarget.entries.find { it.packageName == packageName }?.let {
            if (isAppInstalled(it.packageName)) {
                return InstalledApp(it.packageName, it.displayName)
            }
        }

        // Then check installed apps
        return getInstalledLaunchableApps().find { it.packageName == packageName }
    }

    override fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (e: Exception) {
            false
        }
    }

    fun clearCache() {
        cachedApps = null
    }
}
