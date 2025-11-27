package com.devrinth.launchpad.search.plugins

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.devrinth.launchpad.BuildConfig
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.search.SearchPlugin
import com.devrinth.launchpad.utils.IntentUtils
import com.devrinth.launchpad.utils.StringUtils
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

class AppsPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "apps"
    private val CACHE_FILE = "app_cache.txt"
    private val TAG = "AppsPlugin"
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(mContext)

    private data class AppInfo(
        val label: String,
        val packageName: String
    )

    private var appList: List<AppInfo> = emptyList()
    private lateinit var mPackageManager: PackageManager
    private var searchJob: Job? = null

    override fun pluginInit() {
        mPackageManager = mContext.packageManager
        CoroutineScope(Dispatchers.IO).launch {
            loadCache()
            val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfoList = mPackageManager.queryIntentActivities(intent, 0)
            val freshAppList = resolveInfoList
                .filter { it.activityInfo.packageName != BuildConfig.APPLICATION_ID }
                .map {
                    AppInfo(
                        label = it.loadLabel(mPackageManager).toString(),
                        packageName = it.activityInfo.packageName
                    )
                }
            if (freshAppList.isNotEmpty()) {
                appList = freshAppList
                saveCache(freshAppList)
            }
        }
        super.pluginInit()
    }

    private fun loadCache() {
        val cacheFile = File(mContext.cacheDir, CACHE_FILE)
        if (!cacheFile.exists()) return
        try {
            val cachedApps = cacheFile.readLines().mapNotNull { line ->
                val parts = line.split("\t", limit = 2)
                if (parts.size == 2) AppInfo(
                    label = parts[1],
                    packageName = parts[0]
                ) else null
            }
            if (cachedApps.isNotEmpty()) {
                appList = cachedApps
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading app cache", e)
        }
    }

    private fun saveCache(apps: List<AppInfo>) {
        val cacheFile = File(mContext.cacheDir, CACHE_FILE)
        try {
            cacheFile.writer().use { writer ->
                apps.forEach { app ->
                    writer.write("${app.packageName}\t${app.label}\n")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving app cache", e)
        }
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

    private fun incrementUsageCount(packageName: String) {
        val currentCount = sharedPreferences.getInt("usage_count_$packageName", 0)
        sharedPreferences.edit().putInt("usage_count_$packageName", currentCount + 1).apply()
    }

    private fun getUsageCount(packageName: String): Int {
        return sharedPreferences.getInt("usage_count_$packageName", 0)
    }

    private suspend fun filterApps(query: String): List<ResultAdapter> {
        return withContext(Dispatchers.Default) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) {
                return@withContext emptyList()
            }

            val filteredList = appList.filter { appInfo ->
                StringUtils.fuzzyContains(normalizedQuery, appInfo.label) ||
                        StringUtils.simpleContains(normalizedQuery, appInfo.packageName)
            }

            if (query.length in 1..2) {
                filteredList.sortedWith(compareByDescending<AppInfo> { getUsageCount(it.packageName) }.thenBy { it.label })
            } else {
                filteredList
            }.mapNotNull { appInfo ->
                try {
                    ResultAdapter(
                        appInfo.label,
                        appInfo.packageName,
                        mPackageManager.getApplicationIcon(appInfo.packageName),
                        action1 = {
                            incrementUsageCount(appInfo.packageName)
                            mContext.startActivity(
                                IntentUtils.getAppIntent(
                                    mPackageManager,
                                    appInfo.packageName
                                )
                            )
                        },
                        action2 = null
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
        }
    }
}
