package com.devrinth.launchpad.search.plugins
import android.content.Context
import com.devrinth.launchpad.R
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.search.SearchPlugin
import com.devrinth.launchpad.utils.IntentUtils
import com.devrinth.launchpad.utils.StringUtils
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class SearchSuggestionsPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "search_suggestions"

    private val client = OkHttpClient()
    private val GOOGLE_API =
        "https://suggestqueries.google.com/complete/search?client=firefox&q=%s"

    private lateinit var searchEngineQ: String
    private var searchJob: Job? = null

    override fun pluginInit() {
        super.pluginInit()
        updateSearchEngine()
    }

    override fun pluginResume() {
        super.pluginResume()
        updateSearchEngine()
    }

    private fun updateSearchEngine() {
        val search = (getPluginSetting(
            "engine",
            mContext.resources.getString(R.string.search_google_query)
        ) as String).split("|")
        searchEngineQ =
            if (search.getOrNull(0) != "custom") search.getOrNull(1) ?: "" else (getPluginSetting(
                "custom_engine",
                mContext.resources.getString(R.string.search_google_query)
            ) as String).split("|").getOrNull(1) ?: ""
    }


    override fun pluginProcess(query: String) {
        super.pluginProcess(query)
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            pluginResult(processSuggestions(query), query)
        }
    }

    private suspend fun processSuggestions(query: String): List<ResultAdapter> {
        if (query.isBlank()) {
            return emptyList()
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(GOOGLE_API.format(query.lowercase()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {

                    return@withContext emptyList()
                }

                val mainObj = JSONArray(responseBody)
                val suggestionArray = mainObj.getJSONArray(1)
                val searchSuggestions = ArrayList<ResultAdapter>()
                val maxResults = getPluginSetting("max", 5) as Int

                for (i in 0 until suggestionArray.length()) {
                    if (i >= maxResults) break
                    val suggestion = suggestionArray.getString(i)
                    searchSuggestions.add(
                        ResultAdapter(
                            suggestion,
                            null,
                            null,
                            IntentUtils.getLinkIntent(searchEngineQ.format(StringUtils.encodeUrl(query))),
                            null
                        )
                    )
                }
                searchSuggestions
            } catch (e: CancellationException) {
                // Don't log cancellations
                emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}