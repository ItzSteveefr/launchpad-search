package com.devrinth.launchpad.search.plugins

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.devrinth.launchpad.BuildConfig
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.search.SearchPlugin
import com.devrinth.launchpad.utils.IntentUtils
import com.devrinth.launchpad.utils.StringUtils
import java.io.BufferedWriter
import java.io.File
import kotlinx.coroutines.*

class AppsPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "apps"

    private data class AppInfo(
        val label: String,
        val packageName: String,
        val icon: Drawable
    )

    private var appList: List<AppInfo> = emptyList()
    private lateinit var mPackageManager: PackageManager
    private var searchJob: Job? = null
    private val cacheFile by lazy {
        File(mContext.cacheDir, "app_cache.txt")
    }

    override fun pluginInit() {
        mPackageManager = mContext.packageManager
        CoroutineScope(Dispatchers.IO).launch {
            if (cacheFile.exists()) {
                val cachedApps = mutableListOf<AppInfo>()
                cacheFile.forEachLine { line ->
                    val parts = line.split("|", limit = 2)
                    if (parts.size == 2) {
                        val label = parts[0]
                        val packageName = parts[1]
                        try {
                            cachedApps.add(
                                AppInfo(
                                    label,
                                    packageName,
                                    mPackageManager.getApplicationIcon(packageName)
                                )
                            )
                        } catch (_: PackageManager.NameNotFoundException) {
                        }
                    }
                }
                appList = cachedApps
            } else {
                val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
                val resolveInfoList = mPackageManager.queryIntentActivities(intent, 0)
                val freshAppList = resolveInfoList
                    .filter { it.activityInfo.packageName != BuildConfig.APPLICATION_ID }
                    .map {
                        AppInfo(
                            label = it.loadLabel(mPackageManager).toString(),
                            packageName = it.activityInfo.packageName,
                            icon = it.activityInfo.loadIcon(mPackageManager)
                        )
                    }
                appList = freshAppList
                cacheFile.bufferedWriter().use { out: BufferedWriter ->
                    freshAppList.forEach { app ->
                        out.write("${app.label}|${app.packageName}\n")
                    }
                }
            }
        }
        super.pluginInit()
    }

    override fun pluginProcess(query: String) {
        if (!INIT || query.length < 2) {
            pluginResult(emptyList(), "")
            return
        }

        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            delay(150) // Debounce
            val results = filterApps(query)
            pluginResult(results, query)
        }
    }

    private suspend fun filterApps(query: String): List<ResultAdapter> {
        return withContext(Dispatchers.Default) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) {
                return@withContext emptyList()
            }

            appList.filter { appInfo ->
                StringUtils.fuzzyContains(normalizedQuery, appInfo.label) ||
                        StringUtils.simpleContains(normalizedQuery, appInfo.packageName)
            }.map { appInfo ->
                ResultAdapter(
                    appInfo.label,
                    appInfo.packageName,
                    appInfo.icon,
                    IntentUtils.getAppIntent(mPackageManager, appInfo.packageName),
                    null
                )
            }
        }
    }
}
