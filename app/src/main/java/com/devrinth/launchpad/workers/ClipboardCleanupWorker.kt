package com.devrinth.launchpad.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devrinth.launchpad.data.LaunchpadDatabase

/**
 * WorkManager worker that runs daily to clean up old clipboard entries.
 * Deletes non-pinned entries older than 5 days.
 */
class ClipboardCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "clipboard_cleanup_worker"
        const val TAG = "ClipboardCleanupWorker"
        
        // 5 days in milliseconds
        const val RETENTION_PERIOD_MS = 5L * 24L * 60L * 60L * 1000L
    }

    override suspend fun doWork(): Result {
        return try {
            val database = LaunchpadDatabase.getInstance(applicationContext)
            val clipboardDao = database.clipboardDao()
            
            val cutoffTimestamp = System.currentTimeMillis() - RETENTION_PERIOD_MS
            clipboardDao.deleteOlderThan(cutoffTimestamp)
            
            Log.d(TAG, "Clipboard cleanup completed. Deleted entries older than 5 days.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Clipboard cleanup failed: ${e.message}", e)
            Result.retry()
        }
    }
}
