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
import java.io.BufferedWriter
import java.io.File
import kotlinx.coroutines.*

class AppsPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "apps"

    internal data class AppInfo(
        val label: String,
        val packageName: String,
        val icon: Drawable? = null
    )

    internal var appList: List<AppInfo> = emptyList()
    private lateinit var mPackageManager: PackageManager
    private var searchJob: Job? = null
    private val cacheFile by lazy {
        File(mContext.cacheDir, "app_cache.txt")
    }
    internal val customKeywords = mutableMapOf<String, String>()

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
            withContext(Dispatchers.Main) {
                loadCustomKeywords()
            }
        }
        super.pluginInit()
    }

    private fun loadCustomKeywords() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        customKeywords.clear()
        for (app in appList) {
            val keyword = prefs.getString(app.packageName, null)
            if (!keyword.isNullOrEmpty()) {
                customKeywords[app.packageName] = keyword
            }
        }
    }

    override fun pluginResume() {
        loadCustomKeywords()
    }

    override fun pluginProcess(query: String) {
        if (!INIT || query.length < 2) {
            pluginResult(emptyList(), "")
            return
        }

        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            val results = filterApps(query)
            pluginResult(results, query)
        }
    }

    internal suspend fun filterApps(query: String): List<ResultAdapter> {
        return withContext(Dispatchers.Default) {
            val normalizedQuery = query.trim().lowercase()
            if (normalizedQuery.isEmpty()) {
                return@withContext emptyList()
            }

            appList.filter { app ->
                val customKeyword = customKeywords[app.packageName]
                (customKeyword != null && customKeyword.startsWith(normalizedQuery)) ||
                app.label.lowercase().startsWith(normalizedQuery) ||
                app.label.split(" ").any { it.lowercase().startsWith(normalizedQuery) } ||
                StringUtils.fuzzyContains(normalizedQuery, app.label)
            }
            .distinctBy { it.packageName }
            .map { appInfo ->
                ResultAdapter(
                    appInfo.label,
                    null,
                    appInfo.icon,
                    IntentUtils.getAppIntent(mPackageManager, appInfo.packageName),
                    null
                )
            }
        }
    }
}
