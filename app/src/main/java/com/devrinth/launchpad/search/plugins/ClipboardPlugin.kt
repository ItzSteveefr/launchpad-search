package com.devrinth.launchpad.search.plugins

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.devrinth.launchpad.Launchpad
import com.devrinth.launchpad.R
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.data.ClipboardEntity
import com.devrinth.launchpad.search.SearchPlugin
import com.devrinth.launchpad.utils.ClipboardHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Plugin for clipboard history functionality.
 * Triggers on "clip" or "copy" keywords and shows recent clipboard entries.
 * Also provides zero-state "Recent Copies" when search is empty.
 */
class ClipboardPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "clipboard"
    override var ACTIVATION_SHORTCUT = "clip|copy"
    override var PRIORITY = 1

    private var searchJob: Job? = null
    private val clipboardDao by lazy {
        (mContext.applicationContext as Launchpad).database.clipboardDao()
    }
    
    private val clipboardIcon by lazy {
        ContextCompat.getDrawable(mContext, R.drawable.ic_clipboard_24)
    }

    override fun pluginInit() {
        super.pluginInit()
    }

    override fun pluginProcess(query: String) {
        if (!INIT) {
            pluginResult(emptyList(), "")
            return
        }

        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            val results = when {
                // Empty query - show recent copies for zero-state
                query.isEmpty() -> {
                    getRecentClipboardResults(3)
                }
                // Query matches activation shortcut
                query.lowercase().startsWith("clip") || query.lowercase().startsWith("copy") -> {
                    val searchTerm = query.lowercase()
                        .removePrefix("clip")
                        .removePrefix("copy")
                        .trim()
                    
                    if (searchTerm.isEmpty()) {
                        getRecentClipboardResults(10)
                    } else {
                        searchClipboardHistory(searchTerm, 10)
                    }
                }
                else -> emptyList()
            }
            pluginResult(results, query)
        }
    }

    private suspend fun getRecentClipboardResults(limit: Int): List<ResultAdapter> {
        return try {
            val entries = clipboardDao.getRecentList(limit)
            entries.map { entry -> createResultAdapter(entry) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun searchClipboardHistory(query: String, limit: Int): List<ResultAdapter> {
        return try {
            val entries = clipboardDao.search(query, limit)
            entries.map { entry -> createResultAdapter(entry) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun createResultAdapter(entry: ClipboardEntity): ResultAdapter {
        val preview = ClipboardHelper.truncateForPreview(entry.content, 80)
        val timeAgo = ClipboardHelper.getRelativeTimeString(entry.timestamp)
        
        // Create an intent that will copy the content back to clipboard
        // Using a custom action that SearchManager/Activity can handle
        val copyIntent = Intent().apply {
            action = ACTION_COPY_CLIPBOARD
            putExtra(EXTRA_CLIPBOARD_CONTENT, entry.content)
            putExtra(EXTRA_CLIPBOARD_ID, entry.id)
        }
        
        return ResultAdapter(
            value = preview,
            extra = timeAgo,
            image = clipboardIcon,
            action1 = copyIntent,
            action2 = null
        )
    }

    override fun pluginUnInit() {
        searchJob?.cancel()
        super.pluginUnInit()
    }

    companion object {
        const val ACTION_COPY_CLIPBOARD = "com.devrinth.launchpad.ACTION_COPY_CLIPBOARD"
        const val EXTRA_CLIPBOARD_CONTENT = "clipboard_content"
        const val EXTRA_CLIPBOARD_ID = "clipboard_id"
    }
}
