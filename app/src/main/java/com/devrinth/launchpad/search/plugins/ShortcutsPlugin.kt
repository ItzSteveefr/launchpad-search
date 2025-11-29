package com.devrinth.launchpad.search.plugins

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.devrinth.launchpad.BuildConfig
import com.devrinth.launchpad.R
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.search.SearchPlugin
import com.devrinth.launchpad.utils.IntentUtils
import com.devrinth.launchpad.utils.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ShortcutsPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "shortcuts"

    private lateinit var mPackageManager: PackageManager
    private lateinit var mLauncherApps: LauncherApps
    private var searchJob: Job? = null

    private data class Shortcut(
        val label: CharSequence,
        val appLabel: CharSequence,
        val icon: Drawable?,
        val intent: Intent
    )

    override fun pluginInit() {
        mPackageManager = mContext.packageManager
        mLauncherApps = mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        super.pluginInit()
    }

    override fun pluginProcess(query: String) {
        if (!INIT || query.length < 2) {
            pluginResult(emptyList(), "")
            return
        }
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            pluginResult(filterShortcuts(query), query)
        }
    }

    private suspend fun filterShortcuts(query: String): List<ResultAdapter> {
        return withContext(Dispatchers.Default) {
            val queryLower = query.lowercase()
            val shortcuts = mutableListOf<Shortcut>()

            // Dynamic and Pinned Shortcuts
            val queryParams = LauncherApps.ShortcutQuery()
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                            or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            mLauncherApps.getProfiles()?.forEach { profile ->
                mLauncherApps.getShortcuts(queryParams, profile)?.forEach { shortcut ->
                    if (shortcut.isEnabled) {
                        try {
                            val appInfo =
                                mLauncherApps.getApplicationInfo(shortcut.packageName, 0, profile)
                            shortcuts.add(
                                Shortcut(
                                    shortcut.shortLabel ?: shortcut.longLabel ?: "",
                                    appInfo.loadLabel(mPackageManager),
                                    mLauncherApps.getShortcutIconDrawable(shortcut, 0),
                                    mLauncherApps.getMainActivityLaunchIntent(shortcut.packageName, null, shortcut.userHandle)
                                )
                            )
                        } catch (_: PackageManager.NameNotFoundException) {
                        }
                    }
                }
            }


            // Legacy Shortcuts
            val legacyIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
            mPackageManager.queryIntentActivities(legacyIntent, 0).forEach { ri ->
                if (ri.activityInfo.packageName != BuildConfig.APPLICATION_ID) {
                    try {
                        shortcuts.add(
                            Shortcut(
                                ri.loadLabel(mPackageManager),
                                mPackageManager.getApplicationLabel(
                                    mPackageManager.getApplicationInfo(
                                        ri.activityInfo.packageName,
                                        0
                                    )
                                ),
                                ri.loadIcon(mPackageManager),
                                IntentUtils.getShortcutIntent(
                                    ri.activityInfo.packageName,
                                    ri.activityInfo.name
                                )
                            )
                        )
                    } catch (_: PackageManager.NameNotFoundException) {
                    }
                }
            }
            shortcuts
                .filter {
                    StringUtils.fuzzyContains(queryLower, it.label.toString()) ||
                            StringUtils.fuzzyContains(queryLower, it.appLabel.toString())
                }
                .map {
                    ResultAdapter(
                        it.label.toString(),
                        mContext.getString(R.string.plugin_shortcuts_query)
                            .format(it.appLabel),
                        it.icon,
                        it.intent,
                        null
                    )
                }
        }
    }
}