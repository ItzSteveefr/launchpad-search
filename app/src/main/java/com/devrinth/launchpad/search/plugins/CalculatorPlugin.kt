package com.devrinth.launchpad.search.plugins

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.devrinth.launchpad.R
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.search.SearchPlugin
import com.notkamui.keval.Keval

class CalculatorPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "calculator"
    override var PRIORITY = 1

    // Regex to quickly check if the query is a valid math expression
    private val mathRegex = Regex("^[\\d\\s()+\\-*/.^%]+\$")


    override fun pluginProcess(query: String) {
        super.pluginProcess(query)

        if (query.length < 3 || !mathRegex.matches(query)) {
            pluginResult(emptyList(), "")
            return
        }

        try {
            val result = Keval.eval(query)
            pluginResult(
                arrayListOf(
                    ResultAdapter(
                        result.toString(),
                        query,
                        AppCompatResources.getDrawable(mContext, R.drawable.baseline_calculate_24),
                        null,
                        null
                    )
                ), query
            )
        } catch (e: Exception) {
            pluginResult(emptyList(), "")
        }
    }

}