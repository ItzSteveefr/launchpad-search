package com.devrinth.launchpad.fragments

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.devrinth.launchpad.viewmodels.CustomKeywordsViewModel

class CustomKeywordsPreferences : PreferenceFragmentCompat() {

    private val viewModel: CustomKeywordsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        val appCategory = PreferenceCategory(context).apply {
            title = "Apps"
            screen.addPreference(this)
        }

        viewModel.apps.observe(this) { apps ->
            appCategory.removeAll()
            apps.forEach { app ->
                val preference = Preference(context).apply {
                    title = app.label
                    icon = app.icon
                    key = app.packageName
                    summary = if (app.keyword.isNullOrEmpty()) "No keyword set" else "Keyword: ${app.keyword}"
                    setOnPreferenceClickListener {
                        showKeywordDialog(app.packageName, app.keyword ?: "")
                        true
                    }
                }
                appCategory.addPreference(preference)
            }
        }

        viewModel.loadApps()
    }

    private fun showKeywordDialog(packageName: String, currentKeyword: String) {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Set Custom Keyword")

        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(currentKeyword)
        }
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val newKeyword = input.text.toString()
            viewModel.saveKeyword(packageName, newKeyword)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}}
