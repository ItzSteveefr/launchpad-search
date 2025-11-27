package com.devrinth.launchpad.search.plugins

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.preference.PreferenceManager
import com.devrinth.launchpad.BuildConfig
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.search.SearchPlugin
import com.devrinth.launchpad.utils.IntentUtils
import com.devrinth.launchpad.utils.StringUtils
import java.io.File
import kotlinx.coroutines.*

class AppsPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "apps"

    companion object {
        private const val PACKAGE_MATCH_BONUS = 15
        private const val LAUNCH_COUNT_MULTIPLIER = 20
    }

    private data class AppInfo(
        val label: String,
        val packageName: String,
        val icon: Drawable
    )

    private var appList: List<AppInfo> = emptyList()
    private lateinit var mPackageManager: PackageManager
    private var searchJob: Job? = null
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)


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
                cacheFile.bufferedWriter().use { out ->
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

            val scoredApps = appList.mapNotNull { appInfo ->
                val fuzzyScore = StringUtils.fuzzySearch(normalizedQuery, appInfo.label)
                val packageScore = if (StringUtils.simpleContains(normalizedQuery, appInfo.packageName)) PACKAGE_MATCH_BONUS else 0
                val launchCount = sharedPreferences.getInt("launch_count_${appInfo.packageName}", 0)
                val totalScore = fuzzyScore + packageScore + (launchCount * LAUNCH_COUNT_MULTIPLIER)

                if (totalScore > 0) Pair(appInfo, totalScore) else null
            }

            scoredApps.sortedByDescending { it.second }.map { (appInfo, _) ->
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
