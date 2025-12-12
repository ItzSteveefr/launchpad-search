package com.devrinth.launchpad

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.devrinth.launchpad.data.LaunchpadDatabase
import com.devrinth.launchpad.utils.ThemeManager
import com.devrinth.launchpad.workers.ClipboardCleanupWorker
import com.google.android.material.color.DynamicColors
import java.util.concurrent.TimeUnit

class Launchpad : Application() {
    
    // Lazy database instance accessible throughout the app
    val database: LaunchpadDatabase by lazy {
        LaunchpadDatabase.getInstance(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyThemeFromPreferences(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
        
        // Schedule daily clipboard cleanup
        scheduleClipboardCleanup()
    }
    
    private fun scheduleClipboardCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<ClipboardCleanupWorker>(
            1, TimeUnit.DAYS
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ClipboardCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
}
