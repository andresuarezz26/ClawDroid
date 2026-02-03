package com.aiassistant.domain.repository

import com.aiassistant.domain.model.InstalledApp

interface AppRepository {
    fun getInstalledLaunchableApps(): List<InstalledApp>
    fun findAppByName(query: String): InstalledApp?
    fun findAppByPackage(packageName: String): InstalledApp?
    fun isAppInstalled(packageName: String): Boolean
}
