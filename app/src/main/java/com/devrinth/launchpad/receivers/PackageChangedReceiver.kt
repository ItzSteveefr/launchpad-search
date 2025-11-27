package com.devrinth.launchpad.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

class PackageChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_PACKAGE_ADDED ||
            intent.action == Intent.ACTION_PACKAGE_REMOVED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED
        ) {
            val cacheFile = File(context.cacheDir, "app_cache.txt")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        }
    }
}
