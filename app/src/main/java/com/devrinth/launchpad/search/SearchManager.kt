package com.devrinth.launchpad.search


import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import androidx.core.view.get
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devrinth.launchpad.BuildConfig
import com.devrinth.launchpad.Launchpad
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.adapters.ResultScrollAdapter
import com.devrinth.launchpad.data.ClipboardEntity
import com.devrinth.launchpad.search.external.ExternalSearch
import com.devrinth.launchpad.utils.ClipboardHelper

import com.devrinth.launchpad.search.plugins.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.edit
import androidx.core.view.isNotEmpty

class SearchManager(
    private val mContext: Context,
    private val searchTextBox: EditText,
    private var resultRecyclerView: RecyclerView,
    private var searchCardLayout: LinearLayout
) {

    private var searchQuery: String = ""

    private var previousQuery: String = ""

    private var pluginList = arrayListOf<SearchPlugin>()
    private var pluginsMap = mapOf(

        "apps" to AppsPlugin(mContext),
        "contacts" to ContactsPlugin(mContext),
        "calculator" to CalculatorPlugin(mContext),
        "units" to UnitConversionPlugin(mContext),
        "shortcuts" to ShortcutsPlugin(mContext),
        "clipboard" to ClipboardPlugin(mContext),

    )

    private var actionSearchOpen : Boolean = true

    private var resultArray = ArrayList<ResultAdapter>()
    private var resultScrollAdapter: ResultScrollAdapter

    private var displayedResults = mutableSetOf<ResultAdapter>()


    private var externalSearch : ExternalSearch = ExternalSearch(mContext)

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(mContext)

    private var enabledPlugins: MutableSet<String>? = null

    private var firstQuery: Boolean = true

    private val TAG : String = "PLUGIN MANAGER"

    init {
        resultRecyclerView.layoutManager = LinearLayoutManager(mContext)

        reloadPlugins()

        searchTextBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim()
                sharedPreferences.edit { putString("LAST_SEARCH_QUERY", searchQuery) }
                processQuery()
            }
        })
        searchTextBox.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if ((resultRecyclerView.isNotEmpty()) && actionSearchOpen) {
                    resultRecyclerView[0].performClick()
                    hideKeyboard()
                }
                return@OnEditorActionListener true
            }
            false
        })

        resultScrollAdapter = ResultScrollAdapter(resultArray, mContext)
        resultRecyclerView.adapter = resultScrollAdapter
        
        // Apply cascade animation for premium staggered effect
        resultRecyclerView.itemAnimator = com.devrinth.launchpad.utils.AnimUtils.cascadeItemAnimator()

        if (!sharedPreferences.getBoolean("setting_clear_search", true)) {
            searchTextBox.setText(sharedPreferences.getString("LAST_SEARCH_QUERY", ""))
            searchTextBox.setSelection(searchTextBox.text.length)
        }
        searchCardLayout.post {
            processQuery()
        }
        externalSearch.listener = object : ExternalSearch.ExternalSearchListener {
            override fun onExternalSearchResult(result: ResultAdapter, query: String, pluginPackage: String?) {
                addResults(listOf(result), query, pluginPackage)
            }
        }
    }

    fun unloadPlugins() {
        enabledPlugins?.forEach {
            val plugin = pluginsMap[it]
            if (plugin != null) {
                try {
                    plugin.pluginUnInit()
                } catch (e: Exception) {
                    Log.e(TAG, "Error unloading plugin $it: ${e.localizedMessage}")
                }
            } else {
                Log.w(TAG, "Plugin $it not found in pluginsMap")
            }
        }
        enabledPlugins = null
        externalSearch.unloadPlugins()
    }

    fun resumePlugins() {
        pluginList.forEach {
            it.pluginResume()
        }
        
        // Check and save new clipboard content on resume
        checkAndSaveClipboard()
    }
    
    private fun checkAndSaveClipboard() {
        val clipboardContent = ClipboardHelper.getCurrentClipboardContent(mContext) ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = mContext.applicationContext as Launchpad
                val clipboardDao = app.database.clipboardDao()
                
                val contentHash = ClipboardHelper.computeContentHash(clipboardContent)
                
                // Only insert if this is new content
                if (!clipboardDao.existsByHash(contentHash)) {
                    val entity = ClipboardEntity(
                        content = clipboardContent,
                        contentHash = contentHash,
                        timestamp = System.currentTimeMillis(),
                        type = "text/plain",
                        isPinned = false,
                        preview = ClipboardHelper.truncateForPreview(clipboardContent)
                    )
                    clipboardDao.insert(entity)
                    
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Saved new clipboard entry: ${entity.preview}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save clipboard: ${e.message}")
            }
        }
    }

    /*
    *  PLUGIN LOADING
    *
    *  -> This may look complicated, but this was built to load the plugins asynchronously, and add listeners for the loaded plugins.
    *  -> Might add a less messy way to load plugins in the future.
    *
    *  */
    private fun loadPlugin(pluginName: String, isInternalPlugin: Boolean = false) {

        val plugin = pluginsMap[pluginName] ?: run {
            Log.e(TAG, "Plugin $pluginName not found in pluginsMap")
            return
        }

        try {
            plugin.pluginInit()

        } catch (e : Exception) {
            Log.e(pluginName, e.localizedMessage!!)

        } finally {

            pluginList.add(plugin)
            plugin.onPluginResult { resultArray, query ->
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "${pluginName.uppercase()} returned ${resultArray.size} values")

                if (!isInternalPlugin) {

                    if (plugin.PRIORITY > 0) {
                        CoroutineScope(Dispatchers.Main).launch {
                            kotlinx.coroutines.delay((80 * (plugin.PRIORITY).toLong()))
                            addResults(resultArray, query, pluginName)
                        }
                    } else {
                        addResults(resultArray, query, pluginName)
                    }
                } else {
                    // TODO: Internal Plugins
                }
            }

        }
    }
    fun reloadPlugins() {
        actionSearchOpen = sharedPreferences.getBoolean("setting_top_result_default", true)
        enabledPlugins = sharedPreferences.getStringSet("setting_search_plugins", pluginsMap.keys)

        externalSearch.unloadPlugins()
        pluginList = arrayListOf()

        CoroutineScope(Dispatchers.Main).launch {
            pluginsMap.forEach { plugin ->
                val isInternalPlugin = plugin.key.contains("int-")
                if (enabledPlugins!!.contains(plugin.key) || enabledPlugins!!.isEmpty() || (isInternalPlugin)) {
                    loadPlugin(plugin.key, isInternalPlugin)
                }
            }
        }

        externalSearch.bindAvailablePlugins()

    }

    private fun addResults(results: List<ResultAdapter>, query: String, plugin: String? = "default") {
        if (!searchQuery.equals(query, ignoreCase = true)) return

        val newResults = results.filter { newResult ->
            !displayedResults.contains(newResult) &&
            !resultArray.any { existingResult ->
                isDuplicateResult(existingResult, newResult)
            }
        }

        if (newResults.isNotEmpty()) {
            val startIndex = resultArray.size
            resultArray.addAll(newResults)
            displayedResults.addAll(newResults)
            resultScrollAdapter.notifyItemRangeInserted(startIndex, newResults.size)
        }
    }

    private fun isDuplicateResult(existing: ResultAdapter, new: ResultAdapter): Boolean {
        return existing.value == new.value &&
               existing.extra == new.extra &&
               existing.action1?.toString() == new.action1?.toString()
    }


    private fun processQuery() {
        val isTypingForward = searchQuery.length > previousQuery.length &&
                             searchQuery.startsWith(previousQuery, ignoreCase = true)

        TransitionManager.beginDelayedTransition(searchCardLayout)
        if (searchQuery.isEmpty()) {
            resultRecyclerView.visibility = View.GONE
            clearAllResults()
            previousQuery = searchQuery
            return
        } else {
            resultRecyclerView.visibility = View.VISIBLE
        }

        if (isTypingForward) {
            filterExistingResultsForward()
            externalSearch.sendQuery(searchQuery)
            pluginList.forEach { mPlugin ->
                mPlugin.pluginProcess(searchQuery)
            }
        } else {
            clearAllResults()
            externalSearch.sendQuery(searchQuery)
            pluginList.forEach { mPlugin ->
                mPlugin.pluginProcess(searchQuery)
            }
        }

        if (firstQuery && searchQuery.isNotEmpty()) {
            firstQuery = false
        }

        previousQuery = searchQuery
    }

    private fun clearAllResults() {
        if (resultArray.isNotEmpty()) {
            val count = resultArray.size

            resultArray.clear()
            displayedResults.clear()
            resultScrollAdapter.notifyItemRangeRemoved(0, count)
        }
    }

    private fun filterExistingResultsForward() {
        val iterator = resultArray.iterator()

        var index = 0
        while (iterator.hasNext()) {
            val result = iterator.next()

            if (!resultMatchesQuery(result, searchQuery)) {

                iterator.remove()
                displayedResults.remove(result)
                resultScrollAdapter.notifyItemRemoved(index)
            } else {
                index++
            }
        }
    }

    private fun resultMatchesQuery(result: ResultAdapter, query: String): Boolean {
        return result.value.contains(query, ignoreCase = true)
    }

    private fun hideKeyboard() {
        val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchTextBox.windowToken, 0)
    }
}