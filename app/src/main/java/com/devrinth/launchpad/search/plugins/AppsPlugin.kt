package com.devrinth.launchpad.search.plugins

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.devrinth.launchpad.BuildConfig
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.search.SearchPlugin
import com.devrinth.launchpad.utils.IntentUtils
import com.devrinth.launchpad.utils.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "apps"

    private lateinit var appList: List<ResolveInfo>
    private lateinit var mPackageManager: PackageManager
    private var lastFilteredList: List<ResultAdapter> = emptyList()
    private var lastQuery = ""

    private var searchJob: Job? = null

    override fun pluginInit() {
        mPackageManager = mContext.packageManager
        appList = mPackageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
        super.pluginInit()
    }

    override fun pluginProcess(query: String) {
        searchJob?.cancel()

        when {
            !INIT || query.length < 2 -> {
                pluginResult(emptyList(), "")
                return
            }
        }

        searchJob = CoroutineScope(Dispatchers.Main).launch {
            val results = filterApps(query)
            pluginResult(results, query)
        }
    }

    private suspend fun filterApps(query: String): List<ResultAdapter> {
        return withContext(Dispatchers.Default) {
            val filteredList = appList.filter { ri ->
                if (ri.activityInfo.packageName == BuildConfig.APPLICATION_ID) {
                    return@filter false
                }
                val label = ri.loadLabel(mPackageManager).toString()
                StringUtils.fuzzyContains(query, label) || StringUtils.simpleContains(
                    query,
                    ri.activityInfo.packageName
                )
            }

            val results = filteredList.map { ri ->
                ResultAdapter(
                    ri.loadLabel(mPackageManager).toString(),
                    ri.activityInfo.packageName,
                    ri.activityInfo.loadIcon(mPackageManager),
                    IntentUtils.getAppIntent(mPackageManager, ri.activityInfo.packageName),
                    null
                )
            }

            lastFilteredList = results
            lastQuery = query

            results
        }
    }
}
