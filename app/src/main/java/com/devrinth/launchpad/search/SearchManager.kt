package com.devrinth.launchpad.search


import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import androidx.core.view.get
import androidx.core.view.isNotEmpty
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devrinth.launchpad.BuildConfig
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.adapters.ResultScrollAdapter
import com.devrinth.launchpad.search.external.ExternalSearch
import com.devrinth.launchpad.search.plugins.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchManager(
    private val mContext: Context,
    private val searchTextBox: EditText,
    private var resultRecyclerView: RecyclerView,
    private var searchCardLayout: LinearLayout
) {

    private var searchQuery: String = ""

    private var pluginList = arrayListOf<SearchPlugin>()
    private var pluginsMap = mapOf(
        "apps" to AppsPlugin(mContext),
        "contacts" to ContactsPlugin(mContext),
        "calculator" to CalculatorPlugin(mContext),
        "units" to UnitConversionPlugin(mContext),
        "shortcuts" to ShortcutsPlugin(mContext),
    )

    private var actionSearchOpen: Boolean = true

    private var resultArray = ArrayList<ResultAdapter>()
    private var resultScrollAdapter: ResultScrollAdapter

    private var displayedResults = mutableSetOf<ResultAdapter>()


    private var externalSearch: ExternalSearch = ExternalSearch(mContext)

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(mContext)

    private var enabledPlugins: Set<String>? = null

    private val TAG: String = "PLUGIN MANAGER"
    @VisibleForTesting
    lateinit var textWatcher: TextWatcher

    init {
        resultRecyclerView.layoutManager = LinearLayoutManager(mContext)

        reloadPlugins()

        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim()
                sharedPreferences.edit { putString("LAST_SEARCH_QUERY", searchQuery) }
                processQuery()
            }
        }
        searchTextBox.addTextChangedListener(textWatcher)
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

        if (!sharedPreferences.getBoolean("setting_clear_search", true)) {
            searchTextBox.setText(sharedPreferences.getString("LAST_SEARCH_QUERY", ""))
            searchTextBox.setSelection(searchTextBox.text.length)
        }
        searchCardLayout.post {
            processQuery()
        }
        externalSearch.listener = object : ExternalSearch.ExternalSearchListener {
            override fun onExternalSearchResult(
                result: ResultAdapter,
                query: String,
                pluginPackage: String?
            ) {
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
    }

    /*
    *  PLUGIN LOADING
    *
    *  -> This may look complicated, but this was built to load the plugins asynchronously, and add listeners for the loaded plugins.
    *  -> Might add a less messy way to load plugins in the future.
    *
    *  */
    private fun loadPlugin(pluginName: String, isInternalPlugin: Boolean = false) {

        val plugin = pluginsMap[pluginName] ?: return

        try {
            plugin.pluginInit()

        } catch (e: Exception) {
            Log.e(pluginName, e.localizedMessage ?: "Unknown error")

        } finally {

            pluginList.add(plugin)
            plugin.onPluginResult { results, query ->
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "${pluginName.uppercase()} returned ${results.size} values")

                if (!isInternalPlugin) {

                    if (plugin.PRIORITY > 0) {
                        CoroutineScope(Dispatchers.Main).launch {
                            kotlinx.coroutines.delay((80 * (plugin.PRIORITY).toLong()))
                            addResults(results, query, pluginName)
                        }
                    } else {
                        addResults(results, query, pluginName)
                    }
                } else {
                    // TODO: Internal Plugins
                }
            }

        }
    }

    fun reloadPlugins() {
        actionSearchOpen = sharedPreferences.getBoolean("setting_top_result_default", true)
        enabledPlugins =
            sharedPreferences.getStringSet("setting_search_plugins", pluginsMap.keys)
                ?: pluginsMap.keys

        externalSearch.unloadPlugins()
        pluginList = arrayListOf()

        CoroutineScope(Dispatchers.Main).launch {
            pluginsMap.forEach { plugin ->
                val isInternalPlugin = plugin.key.contains("int-")
                if (enabledPlugins!!.contains(plugin.key) || (isInternalPlugin)) {
                    loadPlugin(plugin.key, isInternalPlugin)
                }
            }
        }

        externalSearch.bindAvailablePlugins()
    }

    private fun addResults(
        results: List<ResultAdapter>,
        query: String,
        plugin: String? = "default"
    ) {
        if (!searchQuery.equals(query, ignoreCase = true)) return

        val newResults = results.filter { newResult ->
            !displayedResults.any { existingResult ->
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
        TransitionManager.beginDelayedTransition(searchCardLayout)
        if (searchQuery.isEmpty()) {
            resultRecyclerView.visibility = View.GONE
            clearAllResults()
            return
        } else {
            resultRecyclerView.visibility = View.VISIBLE
        }

        clearAllResults()

        externalSearch.sendQuery(searchQuery)

        pluginList.forEach { mPlugin ->
            mPlugin.pluginProcess(searchQuery)
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

    private fun hideKeyboard() {
        val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchTextBox.windowToken, 0)
    }
}